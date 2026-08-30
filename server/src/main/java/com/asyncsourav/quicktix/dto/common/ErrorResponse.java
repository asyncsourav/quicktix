
package com.asyncsourav.quicktix.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;



@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse ofValidation(int status, String error, String message, String path, Map<String, String> validationErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, validationErrors);
    }
}
