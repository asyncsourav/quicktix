
package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;

import java.time.LocalDateTime;


/*
@GetMapping("/api/users/{id}")
public User getUserDirectly(@PathVariable Long id) {
    return userRepository.findById(id).orElse(null); // Leaks passwordHash in JSON!
}
*/

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
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
