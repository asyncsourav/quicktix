

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class AuthServiceTest {


    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;



    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Sourav Kumar")
                .email("sourav@gmail.com")
                .passwordHash("hashed_password")
                .role(Role.USER)
                .build();
    }




    @Test
    @DisplayName("Registration encodes password and persists user with role")
    void register_ShouldHashPasswordAndSaveUser() {
        // Given
        RegisterRequest request = new RegisterRequest("Sourav Kumar", "sourav@gmail.com", "password123", Role.USER);
        when(userRepository.existsByEmail("sourav@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        // When
        UserResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("sourav@gmail.com");
        assertThat(response.name()).isEqualTo("Sourav Kumar");
        assertThat(response.role()).isEqualTo(Role.USER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_password");
        assertThat(saved.getEmail()).isEqualTo("sourav@gmail.com");
    }




    @Test
    @DisplayName("Registration rejects duplicate email with BadRequestException")
    void register_WhenEmailAlreadyExists_ShouldThrowBadRequestException() {
        // Given
        RegisterRequest request = new RegisterRequest("Duplicate User", "sourav@gmail.com", "password123", Role.USER);
        when(userRepository.existsByEmail("sourav@gmail.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }




    @Test
    @DisplayName("Login issues access token and saves hashed refresh token in database")
    void login_ShouldIssueDualTokensAndPersistHashedRefreshToken() {

        // Given
        LoginRequest request = new LoginRequest("user@quicktix.com", "password123");
        when(userRepository.findByEmail("user@quicktix.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("mock_access_token");
        when(jwtUtil.generateRawRefreshToken()).thenReturn("raw_mock_refresh_token");
        when(jwtUtil.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock_access_token");
        assertThat(response.refreshToken()).isEqualTo("raw_mock_refresh_token");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getTokenHash()).isEqualTo(JwtUtil.hashToken("raw_mock_refresh_token"));
        assertThat(savedToken.getFamilyId()).isNotBlank();
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.isRevoked()).isFalse();
    }




    @Test
    @DisplayName("Rotation successfully rotates tokens within the same session family")
    void refresh_ShouldRotateTokensWithinSameFamily() {

        // Given
        String rawToken = "current_raw_refresh_token";
        String tokenHash = JwtUtil.hashToken(rawToken);
        String familyId = UUID.randomUUID().toString();

        RefreshToken existingToken = RefreshToken.builder()
                .id(10L)
                .user(testUser)
                .tokenHash(tokenHash)
                .familyId(familyId)
                .used(false)
                .revoked(false)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.markAsUsed(10L)).thenReturn(1);
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("new_access_token");
        when(jwtUtil.generateRawRefreshToken()).thenReturn("new_raw_refresh_token");
        when(jwtUtil.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        // When
        AuthResponse response = authService.refresh(new RefreshTokenRequest(rawToken));

        // Then
        assertThat(response.accessToken()).isEqualTo("new_access_token");
        assertThat(response.refreshToken()).isEqualTo("new_raw_refresh_token");

        verify(refreshTokenRepository).markAsUsed(10L);

        ArgumentCaptor<RefreshToken> newEntityCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(newEntityCaptor.capture());

        RefreshToken newTokenEntity = newEntityCaptor.getValue();
        assertThat(newTokenEntity.getFamilyId()).isEqualTo(familyId); // Must belong to same family
        assertThat(newTokenEntity.getTokenHash()).isEqualTo(JwtUtil.hashToken("new_raw_refresh_token"));
        assertThat(newTokenEntity.isUsed()).isFalse();
        assertThat(newTokenEntity.isRevoked()).isFalse();
    }




    @Test
    @DisplayName("CONCURRENCY: If two simultaneous refreshes occur, the losing request fails to claim the token and revokes the family")
    void refresh_WhenConcurrentClaimLosesRace_ShouldTriggerReuseDetectionAndRevokeFamily() {

        // Given
        String rawToken = "racing_refresh_token";
        String tokenHash = JwtUtil.hashToken(rawToken);
        String familyId = "racing-family-777";

        RefreshToken racingToken = RefreshToken.builder()
                .id(40L)
                .user(testUser)
                .tokenHash(tokenHash)
                .familyId(familyId)
                .used(false)
                .revoked(false)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(racingToken));
        when(refreshTokenRepository.markAsUsed(40L)).thenReturn(0); // Lost the race condition!

        // When & Then
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(rawToken)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token reuse detected");

        verify(refreshTokenRepository).revokeFamily(familyId);
        verify(jwtUtil, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }




    @Test
    @DisplayName("REUSE DETECTION: If an already-used refresh token is presented, revoke the entire family")
    void refresh_WhenTokenIsAlreadyUsed_ShouldTriggerReuseDetectionAndRevokeFamily() {

        // Given
        String rawToken = "stolen_compromised_refresh_token";
        String tokenHash = JwtUtil.hashToken(rawToken);
        String familyId = "compromised-session-family-999";

        RefreshToken alreadyUsedToken = RefreshToken.builder()
                .id(15L)
                .user(testUser)
                .tokenHash(tokenHash)
                .familyId(familyId)
                .used(true) // ALREADY USED!
                .revoked(false)
                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(alreadyUsedToken));

        // When & Then
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(rawToken)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token reuse detected");

        // Verify that the ENTIRE family was revoked!
        verify(refreshTokenRepository).revokeFamily(familyId);
        verify(refreshTokenRepository, never()).markAsUsed(any());
        verify(refreshTokenRepository, never()).save(argThat(RefreshToken::isUsed));
        verify(jwtUtil, never()).generateAccessToken(any());
    }




    @Test
    @DisplayName("REUSE DETECTION: If a revoked token is presented, revoke the family")
    void refresh_WhenTokenIsRevoked_ShouldTriggerReuseDetectionAndRevokeFamily() {

        // Given
        String rawToken = "revoked_token";
        String tokenHash = JwtUtil.hashToken(rawToken);
        String familyId = "revoked-family-123";

        RefreshToken revokedToken = RefreshToken.builder()
                .id(20L)
                .user(testUser)
                .tokenHash(tokenHash)
                .familyId(familyId)
                .used(false)
                .revoked(true) // REVOKED!
                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        // When & Then
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(rawToken)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token reuse detected");

        verify(refreshTokenRepository).revokeFamily(familyId);
    }




    @Test
    @DisplayName("Refresh token expiration check fails if token is past expiresAt")
    void refresh_WhenTokenIsExpired_ShouldThrowBadCredentialsException() {

        // Given
        String rawToken = "expired_token";
        String tokenHash = JwtUtil.hashToken(rawToken);

        RefreshToken expiredToken = RefreshToken.builder()
                .id(25L)
                .user(testUser)
                .tokenHash(tokenHash)
                .familyId("family-xyz")
                .used(false)
                .revoked(false)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)) // Expired 1 hour ago
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(rawToken)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token has expired");

        verify(refreshTokenRepository, never()).revokeFamily(anyString());
    }



    
    @Test
    @DisplayName("Logout revokes the entire token family")
    void logout_ShouldRevokeSessionFamily() {
        
        // Given
        String rawToken = "logout_token";
        String tokenHash = JwtUtil.hashToken(rawToken);
        String familyId = "family-to-logout";

        RefreshToken token = RefreshToken.builder()
                .id(30L)
                .familyId(familyId)
                .tokenHash(tokenHash)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        // When
        authService.logout(new RefreshTokenRequest(rawToken));

        // Then
        verify(refreshTokenRepository).revokeFamily(familyId);
    }
}
