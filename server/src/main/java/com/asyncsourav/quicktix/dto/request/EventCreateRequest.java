
package com.asyncsourav.quicktix.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/*
public class EventCreateRequest {
    private String title;
    private String description;
    private Long venueId;
    private String startTime; // String parsing in service layer is error-prone
    private String category;
    private double basePrice; // Double precision issues!
}
*/

@Schema(description = "Payload for creating or updating an event")
public record EventCreateRequest(
        @Schema(description = "Event title", example = "Coldplay Music of the Spheres Tour")
        @NotBlank(message = "Event title is required")
        @Size(min = 4, max = 200, message = "Event title must be between 4 and 200 characters")
        String title,

        @Schema(description = "Detailed event description", example = "Live stadium concert featuring guest performances.")
        String description,

        @Schema(description = "Associated venue identifier", example = "1")
        @NotNull(message = "Venue ID is required")
        Long venueId,

        @Schema(description = "Event commencement timestamp (ISO 8601)", example = "2026-11-20T19:30:00")
        @NotNull(message = "Start time is required")
        @Future(message = "Event start time must be in the future")
        LocalDateTime startTime,

        @Schema(description = "Event genre or category", example = "Concert")
        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category,

        @Schema(description = "Base ticket price per seat", example = "89.99")
        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
        BigDecimal basePrice
) {}
