

package com.asyncsourav.quicktix.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;



@Schema(description = "Refresh token rotation payload")
public record RefreshTokenRequest(
        
        @NotBlank(message = "Refresh token is required")
        @Schema(description = "Opaque refresh token string", example = "d94b003a-3c0f-4886-b48e-dcfdb24d36d2")
        String refreshToken
) {}
