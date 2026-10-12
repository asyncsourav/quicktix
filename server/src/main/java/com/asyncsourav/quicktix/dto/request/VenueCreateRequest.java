
package com.asyncsourav.quicktix.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



@Schema(description = "Payload for creating a new venue")
public record VenueCreateRequest(
        @Schema(description = "Venue name", example = "Madison Square Garden")
        @NotBlank(message = "Venue name is required")
        @Size(max = 150, message = "Venue name must not exceed 150 characters")
        String name,

        @Schema(description = "Physical address", example = "4 Pennsylvania Plaza, New York, NY 10001")
        @NotBlank(message = "Venue address is required")
        @Size(max = 255, message = "Venue address must not exceed 255 characters")
        String address,

        @Schema(description = "Total seat capacity", example = "100")
        @NotNull(message = "Total capacity is required")
        @Min(value = 1, message = "Total capacity must be at least 1")
        Integer totalCapacity
) {}
