

package com.asyncsourav.quicktix.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Authentication response payload containing JWT token and user info")
public record AuthResponse(
        @Schema(description = "Signed JWT bearer token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Token type prefix", example = "Bearer")
        String tokenType,

        @Schema(description = "Authenticated user profile")
        UserResponse user
) {
    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
