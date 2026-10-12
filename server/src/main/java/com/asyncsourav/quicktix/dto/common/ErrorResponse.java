
package com.asyncsourav.quicktix.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;



@Schema(description = "Standard error envelope returned when a request fails")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Error timestamp", example = "2026-09-25T12:00:00")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP error title", example = "Bad Request")
        String error,

        @Schema(description = "Detailed error message", example = "Seat A1 is already booked")
        String message,

        @Schema(description = "Request URI path", example = "/api/bookings")
        String path,

        @Schema(description = "Field validation errors map, if applicable")
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse ofValidation(int status, String error, String message, String path, Map<String, String> validationErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, validationErrors);
    }
}
