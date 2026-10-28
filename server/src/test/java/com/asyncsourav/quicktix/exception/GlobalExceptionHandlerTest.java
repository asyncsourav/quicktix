

package com.asyncsourav.quicktix.exception;



import com.asyncsourav.quicktix.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }



    @Test
    @DisplayName("Status mapping: SeatUnavailableException maps to 409 CONFLICT")
    void handleSeatUnavailable_Returns409() {

        SeatUnavailableException ex = new SeatUnavailableException(1L, "A1", "Seat already booked");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleSeatUnavailable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("Seat 'A1'");
    }



    @Test
    @DisplayName("Status mapping: ObjectOptimisticLockingFailureException maps to 409 CONFLICT")
    void handleOptimisticLockFailure_Returns409() {

        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException("Seat", 1L);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOptimisticLockFailure(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("just selected or booked by another user");
    }



    @Test
    @DisplayName("Status mapping: RateLimitExceededException maps to 429 with Retry-After header")
    void handleRateLimitExceeded_Returns429WithHeader() {

        RateLimitExceededException ex = new RateLimitExceededException(5);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRateLimitExceeded(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().status()).isEqualTo(429);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
    }



    @Test
    @DisplayName("Status mapping: BadCredentialsException maps to 401 UNAUTHORIZED")
    void handleBadCredentials_Returns401() {

        BadCredentialsException ex = new BadCredentialsException("Invalid password");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().status()).isEqualTo(401);
    }



    @Test
    @DisplayName("Status mapping: AccessDeniedException maps to 403 FORBIDDEN")
    void handleAccessDenied_Returns403() {
        
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().status()).isEqualTo(403);
    }
}

