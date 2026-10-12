
package com.asyncsourav.quicktix.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;



@Schema(description = "Payload for user login authentication")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "user@quicktix.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email format")
        String email,

        @Schema(description = "Account password", example = "password123")
        @NotBlank(message = "Password is required")
        String password
) {}
