
package com.asyncsourav.quicktix.dto.request;


import com.asyncsourav.quicktix.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



@Schema(description = "Payload for user registration")
public record RegisterRequest(
        @Schema(description = "User full name", example = "Alex Morgan")
        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        @Schema(description = "User email address", example = "alex@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Schema(description = "Account password (min 6 chars)", example = "securePass123")
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password,

        @Schema(description = "Assigned user role", example = "USER")
        @NotNull(message = "Role is required (USER, ORGANIZER, ADMIN)")
        Role role
) {}
