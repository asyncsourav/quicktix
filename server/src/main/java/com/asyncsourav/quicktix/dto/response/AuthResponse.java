

package com.asyncsourav.quicktix.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Authentication response payload containing dual JWT access token, rotating refresh token, and user profile")
public record AuthResponse(
    
        @Schema(description = "Signed JWT bearer access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Alias for access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Opaque rotating refresh token (7 days TTL)", example = "d94b003a-3c0f-4886-b48e-dcfdb24d36d2")
        String refreshToken,

        @Schema(description = "Token type prefix", example = "Bearer")
        String tokenType,

        @Schema(description = "Authenticated user profile")
        UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken, UserResponse user) {
        return new AuthResponse(accessToken, accessToken, refreshToken, "Bearer", user);
    }

    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, token, null, "Bearer", user);
    }
}
