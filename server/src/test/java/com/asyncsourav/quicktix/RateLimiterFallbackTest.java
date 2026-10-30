


package com.asyncsourav.quicktix;




import com.asyncsourav.quicktix.dto.request.BookingRequest;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import com.asyncsourav.quicktix.repository.UserRepository;
import com.asyncsourav.quicktix.service.BookingService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;




/**
 * Fallback rate limiter test with Redis stopped / unavailable.
 * Verifies that requests pass until the local limit and then return 429,
 * and asserts that the Micrometer fallback counter is incremented.
 */
@SpringBootTest
class RateLimiterFallbackTest extends BaseIntegrationTest {


    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private BookingService bookingService;




    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Ensure test user exists in the database
        if (userRepository.findByEmail("ratelimit.fallback@quicktix.com").isEmpty()) {
            userRepository.save(User.builder()
                    .name("Fallback Tester")
                    .email("ratelimit.fallback@quicktix.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.USER)
                    .build());
        }
    }




    @Test
    @DisplayName("When Redis is stopped: Login works until local limit, then returns 429, and counter > 0")
    void loginRateLimit_WhenRedisStopped_UsesLocalFallbackUntilLimitThen429() throws Exception {

        // Given - Redis is stopped (no Redis container started)
        String clientIp = "10.20.30.40";
        String loginJson = """
                {
                    "email": "ratelimit.fallback@quicktix.com",
                    "password": "password123"
                }
                """;

        // When - Make requests within local capacity (burst limit = 5)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk());
        }

        // Then - The 6th request exceeds the local limit and returns 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isTooManyRequests());

        // Assert the Micrometer counter is above zero
        double counterValue = meterRegistry.get("ratelimiter.fallback.used").counter().count();
        assertThat(counterValue).isGreaterThan(0.0);
    }




    @Test
    @DisplayName("When Redis is stopped: Booking works until local limit, then returns 429, and counter > 0")
    void bookingRateLimit_WhenRedisStopped_UsesLocalFallbackUntilLimitThen429() throws Exception {

        // Given - Mock booking service to return confirmed booking
        BookingResponse mockBooking = new BookingResponse(
                999L,
                1L,
                "Fallback Tester",
                1L,
                "Test Concert",
                List.of(),
                new BigDecimal("50.00"),
                null,
                LocalDateTime.now()
        );
        when(bookingService.createBooking(any(BookingRequest.class), eq("ratelimit.fallback@quicktix.com")))
                .thenReturn(mockBooking);

        // First login to get access token
        String loginJson = """
                {
                    "email": "ratelimit.fallback@quicktix.com",
                    "password": "password123"
                }
                """;
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.20.30.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String token = responseBody.split("\"accessToken\":\"")[1].split("\"")[0];

        String bookingJson = """
                {
                    "eventId": 1,
                    "seatIds": [1]
                }
                """;
        String bookingClientIp = "10.20.30.50";

        // When - Make requests within local capacity (burst limit = 5)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/bookings")
                            .header("X-Forwarded-For", bookingClientIp)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingJson))
                    .andExpect(status().isCreated());
        }

        // Then - The 6th request exceeds the local limit and returns 429 Too Many Requests
        mockMvc.perform(post("/api/bookings")
                        .header("X-Forwarded-For", bookingClientIp)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isTooManyRequests());

        // Assert the Micrometer counter is above zero
        double counterValue = meterRegistry.get("ratelimiter.fallback.used").counter().count();
        assertThat(counterValue).isGreaterThan(0.0);
    }
}
