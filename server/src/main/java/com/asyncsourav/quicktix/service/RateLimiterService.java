


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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;




@Slf4j
@Service
public class RateLimiterService {


    private final RedisClient redisClient;
    private final CircuitBreaker circuitBreaker;
    private volatile StatefulRedisConnection<String, byte[]> connection;
    private volatile ProxyManager<String> proxyManager;


    // Per-user: burst capacity 5, refills 1 token per second
    private static final Supplier<Bandwidth> USER_LIMIT_SUPPLIER = () -> Bandwidth.builder()
            .capacity(5)
            .refillGreedy(1, Duration.ofSeconds(1))
            .build();


    // Per-IP: burst capacity 10, refills 2 tokens per second
    private static final Supplier<Bandwidth> IP_LIMIT_SUPPLIER = () -> Bandwidth.builder()
            .capacity(10)
            .refillGreedy(2, Duration.ofSeconds(1))
            .build();


    public RateLimiterService(RedisClient rateLimitRedisClient) {
        this.redisClient = rateLimitRedisClient;

        // Configure Resilience4j Circuit Breaker:
        // If Redis fails 5 times, trip to OPEN and skip Redis for 15 seconds (zero latency wait).
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(RateLimitExceededException.class)
                .build();

        this.circuitBreaker = CircuitBreaker.of("redisRateLimiter", config);
    }


    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }




    /**
     * Checks rate limits for a booking request:
     * 1. Per-IP limit (blocks ticket-scalping bots hammering from the same IP)
     * 2. Per-User limit (blocks single authenticated account automation)
     *
     * Fails open if Redis is down or slow.
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



    private boolean checkLimit(String key, Bandwidth limit) {

        // If Circuit Breaker is OPEN (Redis is known to be down), skip immediately with zero wait!
        if (!circuitBreaker.tryAcquirePermission()) {
            log.warn("Redis Circuit Breaker is OPEN. Skipping rate limit check for key: {}", key);
            return true;
        }

        long startTime = System.nanoTime();
        ConsumptionProbe probe;

        try {
            ProxyManager<String> pm = getProxyManager();
            if (pm == null) {
                throw new IllegalStateException("Redis proxy manager is unavailable");
            }

            BucketConfiguration config = BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();

            Bucket bucket = pm.builder().build(key, () -> config);
            probe = bucket.tryConsumeAndReturnRemaining(1);

            // Record success in Circuit Breaker
            circuitBreaker.onSuccess(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        } 
        catch (RateLimitExceededException e) {
            // Legitimate rate limit hit by a bot/user: rethrow so HTTP 429 is returned
            throw e;
        } 
        catch (Exception e) {
            // Redis is down or timing out: record failure in Circuit Breaker and fail open
            circuitBreaker.onError(System.nanoTime() - startTime, TimeUnit.NANOSECONDS, e);
            log.warn("Rate limiter unavailable, failing open: {}", e.getMessage());
            return true;
        }

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

            if (redisClient == null) {
                return null;
            }

            if (connection == null || !connection.isOpen()) {
                connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
            }

            if (connection == null) {
                return null;
            }

            proxyManager = Bucket4jLettuce.casBasedBuilder(connection)
                    .expirationAfterWrite(
                            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10))
                    )
                    .build();

            return proxyManager;
        }
    }
}
