
package com.asyncsourav.quicktix.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



public record VenueCreateRequest(
        @NotBlank(message = "Venue name is required")
        @Size(max = 150, message = "Venue name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Venue address is required")
        @Size(max = 255, message = "Venue address must not exceed 255 characters")
        String address,

        @NotNull(message = "Total capacity is required")
        @Min(value = 1, message = "Total capacity must be at least 1")
        Integer totalCapacity
) {}
