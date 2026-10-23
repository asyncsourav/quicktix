

package com.asyncsourav.quicktix.controller;



import com.asyncsourav.quicktix.dto.common.ApiResponse;
import com.asyncsourav.quicktix.dto.request.LoginRequest;
import com.asyncsourav.quicktix.dto.request.RefreshTokenRequest;
import com.asyncsourav.quicktix.dto.request.RegisterRequest;
import com.asyncsourav.quicktix.dto.response.AuthResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.security.JwtUtil;
import com.asyncsourav.quicktix.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




@Tag(name = "Auth", description = "Endpoints for user registration, JWT authentication, and rotating refresh tokens")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.cookie-name:refreshToken}")
    private String cookieName;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie-same-site:Lax}")
    private String cookieSameSite;



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
     * Authenticates credentials and returns a short-lived access token, sets an HttpOnly refresh token cookie, and returns user profile.
     */
    @Operation(summary = "Login user", description = "Authenticates user credentials and returns a short-lived access token in JSON and rotating refresh token in a Secure HttpOnly cookie.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid 
            @RequestBody 
            LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        if (authResponse.refreshToken() != null) {
            ResponseCookie cookie = createRefreshTokenCookie(authResponse.refreshToken());
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity
                .ok(ApiResponse.success("Login successful", authResponse));
    }



    /**
     * POST /api/auth/refresh
     * Validates an opaque refresh token (from HttpOnly cookie or request body), invalidates it, issues a new token pair, and sets the new cookie.
     */
    @Operation(summary = "Rotate refresh token", description = "Exchanges a valid refresh token from an HttpOnly cookie or request body for a new access token and rotating refresh cookie. If token reuse is detected, the entire session family is revoked.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String tokenToRefresh = extractRefreshToken(httpRequest, request);

        if (tokenToRefresh == null || tokenToRefresh.isBlank()) {
            throw new BadCredentialsException("Refresh token is required.");
        }

        try {
            AuthResponse authResponse = authService.refresh(new RefreshTokenRequest(tokenToRefresh));

            if (authResponse.refreshToken() != null) {
                ResponseCookie cookie = createRefreshTokenCookie(authResponse.refreshToken());
                httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            }

            return ResponseEntity
                    .ok(ApiResponse.success("Token refreshed successfully", authResponse));
        } catch (Exception ex) {
            ResponseCookie cleanCookie = createCleanRefreshTokenCookie();
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cleanCookie.toString());
            throw ex;
        }
    }



    /**
     * POST /api/auth/logout
     * Revokes the active refresh token session family and clears the HttpOnly refresh token cookie.
     */
    @Operation(summary = "Logout user", description = "Revokes the active refresh token session family and terminates the session, clearing the refresh cookie.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String tokenToRevoke = extractRefreshToken(httpRequest, request);

        if (tokenToRevoke != null && !tokenToRevoke.isBlank()) {
            authService.logout(new RefreshTokenRequest(tokenToRevoke));
        }

        ResponseCookie cleanCookie = createCleanRefreshTokenCookie();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cleanCookie.toString());

        return ResponseEntity
                .ok(ApiResponse.success("Logged out successfully", null));
    }



    private String extractRefreshToken(HttpServletRequest request, RefreshTokenRequest bodyRequest) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue().trim();
                }
            }
        }
        if (bodyRequest != null && bodyRequest.refreshToken() != null && !bodyRequest.refreshToken().isBlank()) {
            return bodyRequest.refreshToken().trim();
        }
        return null;
    }



    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(jwtUtil.getRefreshTokenExpirationMs() / 1000)
                .sameSite(cookieSameSite)
                .build();
    }

    

    private ResponseCookie createCleanRefreshTokenCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
    }
}
