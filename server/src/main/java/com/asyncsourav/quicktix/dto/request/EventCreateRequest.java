
package com.asyncsourav.quicktix.dto.request;


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

public record EventCreateRequest(
        @NotBlank(message = "Event title is required")
        @Size(min = 4, max = 200, message = "Event title must be between 4 and 200 characters")
        String title,

        String description,

        @NotNull(message = "Venue ID is required")
        Long venueId,

        @NotNull(message = "Start time is required")
        @Future(message = "Event start time must be in the future")
        LocalDateTime startTime,

        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
        BigDecimal basePrice
) {}
