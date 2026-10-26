

package com.asyncsourav.quicktix.dto.email;


import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;



/**
 * Immutable payload containing booking details for asynchronous email generation.
 * Decoupled from JPA entities to avoid LazyInitializationException outside transactional boundaries.
 */
@Schema(description = "Payload containing confirmed booking details for asynchronous email notifications")
public record BookingConfirmationEmailDetails(

        @Schema(description = "Recipient email address", example = "user@quicktix.com")
        String recipientEmail,

        @Schema(description = "Recipient full name", example = "Jane Doe")
        String recipientName,

        @Schema(description = "Unique booking confirmation ID", example = "42")
        Long bookingId,

        @Schema(description = "Event title", example = "Coldplay Music of the Spheres Tour")
        String eventTitle,

        @Schema(description = "Event commencement timestamp", example = "2026-11-20T19:30:00")
        LocalDateTime eventStartTime,

        @Schema(description = "Hosting venue name", example = "Madison Square Garden")
        String venueName,

        @Schema(description = "List of reserved seat labels", example = "[\"A1\", \"A2\"]")
        List<String> seatLabels,

        @Schema(description = "Total transaction amount paid", example = "150.00")
        BigDecimal totalAmount
) {}
