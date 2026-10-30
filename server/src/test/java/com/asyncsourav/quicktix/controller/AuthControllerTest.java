

package com.asyncsourav.quicktix.controller;



import com.asyncsourav.quicktix.dto.request.LoginRequest;
import com.asyncsourav.quicktix.dto.request.RefreshTokenRequest;
import com.asyncsourav.quicktix.dto.response.AuthResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.exception.GlobalExceptionHandler;
import com.asyncsourav.quicktix.service.AuthService;
import com.asyncsourav.quicktix.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;




@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;


    @BeforeEach

    void setUp() {

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }




    @Test
    @DisplayName("login: Valid credentials return 200 OK, auth tokens, and HttpOnly refresh cookie")
    void login_ShouldReturnTokensAndSetCookie() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("sourav@quicktix.com", "password123");
        UserResponse userResponse = new UserResponse(1L, "Sourav Kumar", "sourav@quicktix.com", Role.USER, LocalDateTime.now());
        AuthResponse authResponse = AuthResponse.of("access-jwt-token", "refresh-opaque-token", userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When
        var resultActions = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-opaque-token"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        verify(authService).login(any(LoginRequest.class));
    }




    @Test
    @DisplayName("refresh: Valid cookie presents refresh token and returns 200 OK with rotated tokens")
    void refresh_WithCookie_ShouldReturnNewTokensAndSetCookie() throws Exception {

        // Given
        Cookie cookie = new Cookie("refreshToken", "valid-refresh-token");
        UserResponse userResponse = new UserResponse(1L, "Sourav Kumar", "sourav@quicktix.com", Role.USER, LocalDateTime.now());
        AuthResponse authResponse = AuthResponse.of("new-access-jwt", "new-refresh-opaque", userResponse);

        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // When
        var resultActions = mockMvc.perform(post("/api/auth/refresh")
                .cookie(cookie));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-jwt"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-opaque"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        verify(authService).refresh(any(RefreshTokenRequest.class));
    }




    @Test
    @DisplayName("refresh: Missing refresh token in both cookie and body returns 401 Unauthorized")
    void refresh_WithoutToken_ShouldThrowBadCredentialsException() throws Exception {
        // Given - No cookie or request body provided

        // When
        var resultActions = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Refresh token is required."));

        verify(authService, never()).refresh(any());
    }



    @Test
    @DisplayName("logout: Revokes refresh token and clears HttpOnly cookie")
    void logout_ShouldClearCookieAndCallService() throws Exception {
        // Given
        Cookie cookie = new Cookie("refreshToken", "valid-refresh-token");

        // When
        var resultActions = mockMvc.perform(post("/api/auth/logout")
                .cookie(cookie));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        verify(authService).logout(any(RefreshTokenRequest.class));
    }
}
