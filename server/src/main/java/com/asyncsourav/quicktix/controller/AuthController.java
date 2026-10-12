

package com.asyncsourav.quicktix.controller;



import com.asyncsourav.quicktix.dto.common.ApiResponse;
import com.asyncsourav.quicktix.dto.request.LoginRequest;
import com.asyncsourav.quicktix.dto.request.RegisterRequest;
import com.asyncsourav.quicktix.dto.response.AuthResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;





@Tag(name = "Auth", description = "Endpoints for user registration and JWT authentication")
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

        UserResponse response = authService
                .register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }



    /**
     * POST /api/auth/login
     * Authenticates credentials and returns a signed JWT bearer token.
     */
    @Operation(summary = "Login user", description = "Authenticates user credentials and returns a signed JWT token.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid 
            @RequestBody 
            LoginRequest request) {

        AuthResponse response = authService
                .login(request);

        return ResponseEntity
                .ok(ApiResponse.success("Login successful", response));
    }
}
