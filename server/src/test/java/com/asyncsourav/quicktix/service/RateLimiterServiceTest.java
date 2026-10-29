

package com.asyncsourav.quicktix.service;



import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;




@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    
    @Mock
    private RedisClient redisClient;



    @Test
    @DisplayName("checkBookingRateLimit: Fails open when Redis is unavailable")
    void checkBookingRateLimit_WhenRedisIsUnavailable_ShouldNotThrow() {

        RateLimiterService service = new RateLimiterService(redisClient);

        assertThatCode(() -> service.checkBookingRateLimit("user@quicktix.com", "203.0.113.9"))
                .doesNotThrowAnyException();
    }



    @Test
    @DisplayName("checkLimit: Returns true when proxy manager is unavailable")
    void checkLimit_WhenProxyManagerIsNull_ShouldReturnTrue() throws Exception {

        RateLimiterService service = new RateLimiterService(redisClient);

        Method checkLimit = RateLimiterService.class.getDeclaredMethod("checkLimit", String.class, Bandwidth.class);
        checkLimit.setAccessible(true);

        boolean result = (boolean) checkLimit.invoke(
                service,
                "rate:booking:user:user@quicktix.com",
                Bandwidth.classic(5, Refill.greedy(1, Duration.ofSeconds(1)))
        );

        assertThat(result).isTrue();
    }



    @Test
    @DisplayName("checkBookingRateLimit: Accepts requests with null or blank identifiers")
    void checkBookingRateLimit_WhenNoIpOrUsername_ShouldNotThrow() {
        
        RateLimiterService service = new RateLimiterService(redisClient);

        assertThatCode(() -> service.checkBookingRateLimit(null, null)).doesNotThrowAnyException();
        assertThatCode(() -> service.checkBookingRateLimit("", " ")).doesNotThrowAnyException();
    }
}
