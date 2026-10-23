

package com.asyncsourav.quicktix.service;


import com.asyncsourav.quicktix.dto.request.LoginRequest;
import com.asyncsourav.quicktix.dto.request.RefreshTokenRequest;
import com.asyncsourav.quicktix.dto.request.RegisterRequest;
import com.asyncsourav.quicktix.dto.response.AuthResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.entity.RefreshToken;
import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import com.asyncsourav.quicktix.exception.BadRequestException;
import com.asyncsourav.quicktix.repository.RefreshTokenRepository;
import com.asyncsourav.quicktix.repository.UserRepository;
import com.asyncsourav.quicktix.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;



@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {



    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    /**
     * Registers a new user with BCrypt hashed password and validated role tier.
     *
     * @param request Validated registration parameters
     * @return Safe UserResponse DTO without password
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {

        String normalizedEmail = request
                .email()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn(
                    "Registration failed: email '{}' is already in use", 
                    normalizedEmail
            );
            throw new BadRequestException("Email '" + normalizedEmail + "' is already registered.");
        }

        String hashedPassword = passwordEncoder
                .encode(request.password());

        Role assignedRole = request.role() != null 
                ? request.role() 
                : Role.USER;

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .passwordHash(hashedPassword)
                .role(assignedRole)
                .build();

        User savedUser = userRepository
                .save(user);

        log.info(
                "User registered successfully with ID: {} and Role: {}", 
                savedUser.getId(), 
                savedUser.getRole()
        );

        return UserResponse
                .fromEntity(savedUser);
    }



    /**
     * Verifies user login credentials, issues an access token + rotating refresh token family.
     *
     * @param request validated login credentials
     * @return authentication response containing access token, rotating refresh token, and user profile
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {

        String normalizedEmail = request
                .email()
                .trim()
                .toLowerCase();

        User user = userRepository
                .findByEmail(normalizedEmail)
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

        // 1. Generate short-lived access token
        String accessToken = jwtUtil.generateAccessToken(user);

        // 2. Generate opaque refresh token and start a new session family
        String rawRefreshToken = jwtUtil.generateRawRefreshToken();
        String familyId = UUID.randomUUID().toString();
        String tokenHash = JwtUtil.hashToken(rawRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(jwtUtil.getRefreshTokenExpirationMs());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .familyId(familyId)
                .used(false)
                .revoked(false)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        UserResponse userResponse = UserResponse.fromEntity(user);

        log.info(
                "User logged in successfully: ID: {} | Session Family: {}",
                user.getId(),
                familyId
        );

        return AuthResponse.of(accessToken, rawRefreshToken, userResponse);
    }



    /**
     * Rotates both access token and refresh token with token-family reuse detection.
     *
     * @param request payload containing the current raw refresh token
     * @return new dual token pair within the same session family
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.refreshToken().trim();
        String tokenHash = JwtUtil.hashToken(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Token refresh rejected: token hash not found in database");
                    return new BadCredentialsException("Invalid or unrecognized refresh token.");
                }); 

        // 1. Check expiration
        if (token.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Token refresh rejected: refresh token expired for user ID {}", token.getUser().getId());
            throw new BadCredentialsException("Refresh token has expired. Please log in again.");
        }

        // 2. REUSE DETECTION: If this token was already used or revoked, a breach occurred!
        if (token.isUsed() || token.isRevoked()) {
            log.error(
                    "SECURITY ALERT: Refresh token reuse detected! User ID: {} | Family: {} | Token ID: {}. Revoking entire token family.",
                    token.getUser().getId(),
                    token.getFamilyId(),
                    token.getId()
            );

            // Invalidate the entire session family immediately
            refreshTokenRepository.revokeFamily(token.getFamilyId());

            throw new BadCredentialsException(
                    "Security violation: Refresh token reuse detected. All active sessions in this family have been terminated. Please log in again."
            );
        }

        // 3. Atomically claim this token: only one concurrent request can win
        int claimed = refreshTokenRepository.markAsUsed(token.getId());
        if (claimed == 0) {
            log.error(
                    "SECURITY ALERT: Concurrent refresh token race detected! User ID: {} | Family: {} | Token ID: {}. Revoking entire token family.",
                    token.getUser().getId(),
                    token.getFamilyId(),
                    token.getId()
            );
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            throw new BadCredentialsException(
                    "Security violation: Refresh token reuse detected. All active sessions in this family have been terminated. Please log in again."
            );
        }

        // 4. Issue new access token
        String newAccessToken = jwtUtil.generateAccessToken(token.getUser());

        // 5. Issue new refresh token within the SAME session family
        String newRawRefreshToken = jwtUtil.generateRawRefreshToken();
        String newTokenHash = JwtUtil.hashToken(newRawRefreshToken);
        Instant newExpiresAt = Instant.now().plusMillis(jwtUtil.getRefreshTokenExpirationMs());

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .user(token.getUser())
                .tokenHash(newTokenHash)
                .familyId(token.getFamilyId())
                .used(false)
                .revoked(false)
                .expiresAt(newExpiresAt)
                .build();

        refreshTokenRepository.save(newRefreshTokenEntity);

        log.info(
                "Refresh token successfully rotated for user ID: {} in session family: {}",
                token.getUser().getId(),
                token.getFamilyId()
        );

        return AuthResponse.of(newAccessToken, newRawRefreshToken, UserResponse.fromEntity(token.getUser()));
    }


    
    /**
     * Revokes the active refresh token session family on logout.
     *
     * @param request optional payload containing the refresh token to revoke
     */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }

        String rawToken = request.refreshToken().trim();
        String tokenHash = JwtUtil.hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            log.info("User logged out. Revoked session family: {}", token.getFamilyId());
        });
    }
}
