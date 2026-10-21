

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity
                .ok(ApiResponse.success("Login successful", response));
    }



    /**
     * POST /api/auth/refresh
     * Validates an opaque refresh token, invalidates it, issues a new dual pair, and enforces token-family reuse detection.
     */
    @Operation(summary = "Rotate refresh token", description = "Exchanges a valid refresh token for a new access token and rotating refresh token. If token reuse is detected, the entire session family is revoked.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid
            @RequestBody
            RefreshTokenRequest request) {

        AuthResponse response = authService.refresh(request);

        return ResponseEntity
                .ok(ApiResponse.success("Token refreshed successfully", response));
    }


    
    /**
     * POST /api/auth/logout
     * Revokes the active refresh token session family.
     */
    @Operation(summary = "Logout user", description = "Revokes the active refresh token session family and terminates the session.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false)
            RefreshTokenRequest request) {

        authService.logout(request);

        return ResponseEntity
                .ok(ApiResponse.success("Logged out successfully", null));
    }
}
