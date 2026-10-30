


package com.asyncsourav.quicktix.service;




import com.asyncsourav.quicktix.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
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
        ConsumptionProbe probe;
        try {
            ProxyManager<String> pm = getProxyManager();
            if (pm == null) {
                log.warn("Rate limiter proxy manager unavailable, failing open for key: {}", key);
                return true;
            }

            BucketConfiguration config = BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();

            Bucket bucket = pm.builder().build(key, () -> config);
            probe = bucket.tryConsumeAndReturnRemaining(1);
        } 
        // if theredis is down, Rate limiter is unavailabe then and hence it fall open
        catch (Exception e) {
            log.warn("Rate limiter unavailable, failing open: {}", e.toString());
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
}
