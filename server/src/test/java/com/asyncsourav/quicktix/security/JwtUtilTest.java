

package com.asyncsourav.quicktix.security;


import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;


class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;


    @BeforeEach
    void setUp() {

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpirationMs", 604800000L);

        testUser = User.builder()
                .id(1L)
                .name("Sourav")
                .email("sourav@quicktix.com")
                .role(Role.USER)
                .build();
    }



    @Test
    @DisplayName("generateAccessToken and extractEmail: Generates verifiable JWT and extracts subject")
    void generateAccessToken_And_ExtractEmail() {
        String token = jwtUtil.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("sourav@quicktix.com");
        assertThat(jwtUtil.isTokenValid(token, "sourav@quicktix.com")).isTrue();
        assertThat(jwtUtil.isTokenValid(token, "other@quicktix.com")).isFalse();
    }

    
    
    
    @Test
    @DisplayName("hashToken: Produces deterministic 64-char SHA-256 hex hash")
    void hashToken_DeterministicHex() {
        String hash1 = JwtUtil.hashToken("my_raw_refresh_token");
        String hash2 = JwtUtil.hashToken("my_raw_refresh_token");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }

    
    
    @Test
    @DisplayName("generateRawRefreshToken: Generates secure unique random token")
    void generateRawRefreshToken_UniqueAndNonNull() {
        String token1 = jwtUtil.generateRawRefreshToken();
        String token2 = jwtUtil.generateRawRefreshToken();

        assertThat(token1).isNotBlank();
        assertThat(token2).isNotBlank();
        assertThat(token1).isNotEqualTo(token2);
    }
}

