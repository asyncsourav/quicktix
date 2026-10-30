

package com.asyncsourav.quicktix.service;




import com.asyncsourav.quicktix.exception.RateLimitExceededException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.lettuce.core.RedisClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;




@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    
    @Mock
    private RedisClient redisClient;




    @Test
    @DisplayName("checkBookingRateLimit: When Redis is stopped, local fallback allows up to limit and blocks with 429")
    void checkBookingRateLimit_WhenRedisStopped_UsesLocalFallbackUntilLimitThenThrows429() {

        // Given - Redis is stopped / unavailable, simple meter registry
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimiterService service = new RateLimiterService(redisClient, registry);

        String username = "fallback-user@quicktix.com";
        String clientIp = "192.168.1.100";

        // When - Make requests within local capacity (burst of 5 for user limit)
        for (int i = 0; i < 5; i++) {
            service.checkBookingRateLimit(username, clientIp);
        }

        // Then - The 6th request exceeds local limit and throws RateLimitExceededException (which maps to 429)
        assertThatThrownBy(() -> service.checkBookingRateLimit(username, clientIp))
                .isInstanceOf(RateLimitExceededException.class);

        // Assert Micrometer fallback counter is incremented (> 0)
        double count = service.getFallbackCounter().count();
        assertThat(count).isGreaterThan(0.0);
    }




    @Test
    @DisplayName("checkLoginRateLimit: When Redis is stopped, login limit allows up to limit and blocks with 429")
    void checkLoginRateLimit_WhenRedisStopped_UsesLocalFallbackUntilLimitThenThrows429() {

        // Given - Redis is stopped / unavailable
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimiterService service = new RateLimiterService(redisClient, registry);

        String clientIp = "192.168.1.101";
        String email = "login-test@quicktix.com";

        // When - Consume burst capacity of 5
        for (int i = 0; i < 5; i++) {
            service.checkLoginRateLimit(clientIp, email);
        }

        // Then - 6th request throws 429
        assertThatThrownBy(() -> service.checkLoginRateLimit(clientIp, email))
                .isInstanceOf(RateLimitExceededException.class);

        // Counter is incremented
        assertThat(service.getFallbackCounter().count()).isGreaterThan(0.0);
    }




    @Test
    @DisplayName("CircuitBreaker: Opens after 5 failures and stays open")
    void circuitBreaker_OpensAfterFailures() {

        // Given - Redis fails
        RateLimiterService service = new RateLimiterService(redisClient);

        // When - Trigger 5 failures
        for (int i = 1; i <= 5; i++) {
            try {
                service.checkBookingRateLimit("cb-user" + i + "@quicktix.com", "10.0.0." + i);
            } catch (Exception ignored) {
            }
        }

        // Then - Circuit breaker transitions to OPEN
        assertThat(service.getCircuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }




    @Test
    @DisplayName("checkBookingRateLimit: Accepts requests with null or blank identifiers")
    void checkBookingRateLimit_WhenNoIpOrUsername_ShouldNotThrow() {
        
        RateLimiterService service = new RateLimiterService(redisClient);

        assertThatCode(() -> service.checkBookingRateLimit(null, null)).doesNotThrowAnyException();
        assertThatCode(() -> service.checkBookingRateLimit("", " ")).doesNotThrowAnyException();
    }
}
