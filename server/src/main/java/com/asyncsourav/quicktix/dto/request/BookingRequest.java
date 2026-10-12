
package com.asyncsourav.quicktix.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;



@Schema(description = "Payload for creating a new seat booking")
public record BookingRequest(
        @Schema(description = "Target event identifier", example = "1")
        @NotNull(message = "Event ID is required")
        Long eventId,

        @Schema(description = "List of seat IDs to reserve and book", example = "[1, 2]")
        @NotEmpty(message = "At least one seat must be selected for booking")
        List<Long> seatIds
) {}
