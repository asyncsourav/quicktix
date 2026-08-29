
package com.asyncsourav.quicktix.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;



public record BookingRequest(
        @NotNull(message = "Event ID is required")
        Long eventId,

        @NotEmpty(message = "At least one seat must be selected for booking")
        List<Long> seatIds
) {}
