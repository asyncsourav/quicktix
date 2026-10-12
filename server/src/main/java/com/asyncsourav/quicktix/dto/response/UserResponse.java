
package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;


/*
@GetMapping("/api/users/{id}")
public User getUserDirectly(@PathVariable Long id) {
    return userRepository.findById(id).orElse(null); // Leaks passwordHash in JSON!
}
*/

@Schema(description = "Public user profile response")
public record UserResponse(
        @Schema(description = "User unique ID", example = "1")
        Long id,

        @Schema(description = "Full name", example = "Sourav Kumar")
        String name,

        @Schema(description = "Email address", example = "user@quicktix.com")
        String email,

        @Schema(description = "User role", example = "USER")
        Role role,

        @Schema(description = "Account registration timestamp", example = "2026-09-25T12:00:00")
        LocalDateTime createdAt
) {

    public static UserResponse fromEntity(User user) {

        if (user == null)
                return null;

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
