package com.asyncsourav.quicktix.controller;

import com.asyncsourav.quicktix.dto.common.ApiResponse;
import com.asyncsourav.quicktix.dto.request.LoginRequest;
import com.asyncsourav.quicktix.dto.request.RefreshTokenRequest;
import com.asyncsourav.quicktix.dto.request.RegisterRequest;
import com.asyncsourav.quicktix.dto.response.AuthResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Endpoints for user registration, JWT authentication, and rotating refresh tokens")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Registers a new user and returns their sanitized public profile.
     */
    @Operation(summary = "Register a new user", description = "Registers a new user account with hashed password and returns the public user profile.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid 
            @RequestBody 
            RegisterRequest request) {

        UserResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    /**
     * POST /api/auth/login
     * Authenticates credentials and returns a dual token pair (access token + rotating refresh token).
     */
    @Operation(summary = "Login user", description = "Authenticates user credentials and returns a short-lived access token, rotating refresh token, and user profile.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid 
            @RequestBody 
            LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", authResponse.refreshToken())
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity
                .ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * POST /api/auth/refresh
     * Validates an opaque refresh token, invalidates it, issues a new dual pair, and enforces token-family reuse detection.
     */
    @Operation(summary = "Rotate refresh token", description = "Exchanges a valid refresh token for a new access token and rotating refresh token. If token reuse is detected, the entire session family is revoked.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletResponse response) {

        String token = (cookieToken != null && !cookieToken.isBlank())
                ? cookieToken
                : (request != null ? request.refreshToken() : null);

        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Refresh token is required.");
        }

        AuthResponse authResponse = authService.refresh(new RefreshTokenRequest(token));

        ResponseCookie cookie = ResponseCookie.from("refreshToken", authResponse.refreshToken())
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity
                .ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    /**
     * POST /api/auth/logout
     * Revokes the active refresh token session family and clears the HttpOnly cookie.
     */
    @Operation(summary = "Logout user", description = "Revokes the active refresh token session family and terminates the session.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletResponse response) {

        String token = (cookieToken != null && !cookieToken.isBlank())
                ? cookieToken
                : (request != null ? request.refreshToken() : null);

        if (token != null && !token.isBlank()) {
            authService.logout(new RefreshTokenRequest(token));
        }

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity
                .ok(ApiResponse.success("Logged out successfully", null));
    }
}
