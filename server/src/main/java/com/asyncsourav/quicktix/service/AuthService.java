

package com.asyncsourav.quicktix.service;


import com.asyncsourav.quicktix.dto.request.LoginRequest;
import com.asyncsourav.quicktix.dto.request.RegisterRequest;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import com.asyncsourav.quicktix.exception.BadRequestException;
import com.asyncsourav.quicktix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;





@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    
    /**
     * Registers a new user.
     *
     * @param request registration details containing name, email, password, and role
     * @return the registered user's response
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn(
                    "Registration failed: email '{}' is already in use",
                    normalizedEmail
            );

            throw new BadRequestException(
                    "Email '" + normalizedEmail + "' is already registered."
            );
        }

        String hashedPassword = passwordEncoder.encode(
                request.password()
        );

        Role assignedRole = request.role() != null
                ? request.role()
                : Role.USER;

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .passwordHash(hashedPassword)
                .role(assignedRole)
                .build();

        User savedUser = userRepository.save(user);

        log.info(
                "User registered successfully with ID: {} and Role: {}",
                savedUser.getId(),
                savedUser.getRole()
        );

        return UserResponse.fromEntity(savedUser);
    }



    /**
     * Authenticates a user using their email and password.
     *
     * @param request login credentials containing email and password
     * @return the authenticated user's response
     */
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn(
                            "Login failed: email '{}' not found",
                            normalizedEmail
                    );

                    return new BadCredentialsException(
                            "Invalid email or password."
                    );
                });

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            log.warn(
                    "Login failed: password mismatch for email '{}'",
                    normalizedEmail
            );

            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }

        log.info(
                "User logged in successfully: ID: {}",
                user.getId()
        );

        return UserResponse.fromEntity(user);
    }
}

