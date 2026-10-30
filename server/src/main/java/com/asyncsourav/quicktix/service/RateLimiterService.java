package com.asyncsourav.quicktix.service;




import com.asyncsourav.quicktix.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;




@Slf4j
@Service
public class RateLimiterService {


    private final RedisClient redisClient;
    private volatile StatefulRedisConnection<String, byte[]> connection;
    private volatile ProxyManager<String> proxyManager;

    // Resilience4j Circuit Breaker around Redis calls: opens after 5 failures, stays open for 10 seconds
    private final CircuitBreaker circuitBreaker;

    // Local in-memory fallback buckets when Redis is down or circuit breaker is open
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    // Micrometer Counter tracking fallback activations
    private final Counter fallbackCounter;


    // Per-user booking: burst capacity 5, refills 1 token per second
    private static final Supplier<Bandwidth> USER_LIMIT_SUPPLIER = () -> Bandwidth.builder()
            .capacity(5)
            .refillGreedy(1, Duration.ofSeconds(1))
            .build();


    // Per-IP booking: burst capacity 10, refills 2 tokens per second
    private static final Supplier<Bandwidth> IP_LIMIT_SUPPLIER = () -> Bandwidth.builder()
            .capacity(10)
            .refillGreedy(2, Duration.ofSeconds(1))
            .build();


    // Per-login attempt: burst capacity 5, refills 1 token per second
    private static final Supplier<Bandwidth> LOGIN_LIMIT_SUPPLIER = () -> Bandwidth.builder()
            .capacity(5)
            .refillGreedy(1, Duration.ofSeconds(1))
            .build();


    @Autowired
    public RateLimiterService(RedisClient rateLimitRedisClient, MeterRegistry meterRegistry) {
        this.redisClient = rateLimitRedisClient;

        // Circuit breaker: open after 5 failures, stay open for 10 seconds
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(100.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();
        this.circuitBreaker = CircuitBreaker.of("redisRateLimiter", cbConfig);

        // Micrometer counter to track fallback activations
        MeterRegistry registry = (meterRegistry != null) ? meterRegistry : new SimpleMeterRegistry();
        this.fallbackCounter = Counter.builder("ratelimiter.fallback.used")
                .description("Number of times local fallback rate limiter was used")
                .register(registry);
    }


    public RateLimiterService(RedisClient rateLimitRedisClient) {
        this(rateLimitRedisClient, new SimpleMeterRegistry());
    }




    /**
     * Checks rate limits for a booking request:
     * 1. Per-IP limit (blocks ticket-scalping bots hammering from the same IP)
     * 2. Per-User limit (blocks single authenticated account automation)
     *
     * Falls back to local limiter if Redis is unavailable or circuit breaker is open.
     */
    public void checkBookingRateLimit(String username, String clientIp) {

        // 1. Check IP limit first
        if (clientIp != null && !clientIp.isBlank()) {
            checkLimit("rate:booking:ip:" + clientIp, IP_LIMIT_SUPPLIER.get());
        }

        // 2. Check User limit
        if (username != null && !username.isBlank()) {
            checkLimit("rate:booking:user:" + username, USER_LIMIT_SUPPLIER.get());
        }
    }




    /**
     * Checks rate limits for a login request:
     * 1. Per-IP limit (prevents credential stuffing attacks from an IP)
     * 2. Per-Account limit (prevents brute forcing a specific user email)
     *
     * Falls back to local limiter if Redis is unavailable or circuit breaker is open.
     */
    public void checkLoginRateLimit(String clientIp, String email) {

        // 1. Check IP limit first
        if (clientIp != null && !clientIp.isBlank()) {
            checkLimit("rate:login:ip:" + clientIp, LOGIN_LIMIT_SUPPLIER.get());
        }

        // 2. Check User limit
        if (email != null && !email.isBlank()) {
            checkLimit("rate:login:user:" + email, LOGIN_LIMIT_SUPPLIER.get());
        }
    }




    /**
     * Primary rate limit check protected by Resilience4j Circuit Breaker.
     * If Redis is healthy -> checks distributed Redis bucket.
     * If Redis fails / Circuit is OPEN -> falls back straight to local in-memory bucket.
     */
    private boolean checkLimit(String key, Bandwidth limit) {

        // If circuit breaker is OPEN, go straight to local limiter without calling Redis
        if (!circuitBreaker.tryAcquirePermission()) {
            log.warn("Circuit breaker is OPEN. Bypassing Redis and using local fallback limiter for key: {}", key);
            return checkLocalLimit(key, limit);
        }

        long start = System.nanoTime();
        try {
            ProxyManager<String> pm = getProxyManager();
            if (pm == null) {
                throw new IllegalStateException("Redis proxy manager unavailable");
            }

            BucketConfiguration config = BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();

            Bucket bucket = pm.builder().build(key, () -> config);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            // Successful Redis call
            circuitBreaker.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);

            if (!probe.isConsumed()) {
                long wait = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
                throw new RateLimitExceededException(wait);
            }
            return true;
        } catch (RateLimitExceededException e) {
            // Rate limit exceeded is expected behavior, not an infrastructure failure
            circuitBreaker.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            throw e;
        } catch (Exception e) {
            // Infrastructure error (Redis connection failed, timeout, etc.) -> report error to circuit breaker
            circuitBreaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, e);
            log.warn("Redis rate limiter failed: {}. Falling back to local limiter.", e.getMessage());
            return checkLocalLimit(key, limit);
        }
    }




    /**
     * In-memory local fallback limiter when Redis is down or circuit breaker is open.
     * Increments the Micrometer fallback counter and enforces local limits.
     */
    private boolean checkLocalLimit(String key, Bandwidth limit) {

        fallbackCounter.increment();

        Bucket bucket = localBuckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(limit)
                .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long wait = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            throw new RateLimitExceededException(wait);
        }
        return true;
    }




    private ProxyManager<String> getProxyManager() {
        if (proxyManager != null && connection != null && connection.isOpen()) {
            return proxyManager;
        }

        synchronized (this) {
            if (proxyManager != null && connection != null && connection.isOpen()) {
                return proxyManager;
            }

            try {
                if (connection == null || !connection.isOpen()) {
                    connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
                }
                proxyManager = Bucket4jLettuce.casBasedBuilder(connection)
                        .expirationAfterWrite(
                                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10))
                        )
                        .build();
                return proxyManager;
            } catch (Exception e) {
                log.warn("Unable to establish Redis connection for rate limiting: {}", e.getMessage());
                return null;
            }
        }
    }


    public Counter getFallbackCounter() {
        return fallbackCounter;
    }


    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}
