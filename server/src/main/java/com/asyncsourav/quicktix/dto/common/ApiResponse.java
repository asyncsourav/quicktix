

package com.asyncsourav.quicktix.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;


@Schema(description = "Standard API response wrapper envelope")
public record ApiResponse<T>(
        @Schema(description = "Operation success flag", example = "true")
        boolean success,

        @Schema(description = "Human-readable response message", example = "Operation completed successfully")
        String message,

        @Schema(description = "Enclosed response payload data")
        T data,

        @Schema(description = "Response generation timestamp", example = "2026-09-25T12:00:00")
        LocalDateTime timestamp
) {
    // * The '<T>' just means to say that this method uses genric type <T> 
    // * "ApiResponse<T>" is the return type of this methods

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
