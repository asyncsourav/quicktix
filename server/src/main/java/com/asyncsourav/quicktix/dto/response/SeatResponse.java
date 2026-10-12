
package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.Seat;
import com.asyncsourav.quicktix.entity.SeatStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/*
public class SeatResponseNoob {
    private Long id;
    private Event event; // Big object overhead!
    private String seatLabel;
    private BigDecimal price;
    private SeatStatus status;
    private Long version; // Exposing internal concurrency state!
    private Booking booking; // Circular reference!
}
*/


@Schema(description = "Seat availability response item")
public record SeatResponse(
        @Schema(description = "Seat unique ID", example = "10")
        Long id,

        @Schema(description = "Associated event ID", example = "1")
        Long eventId,

        @Schema(description = "Seat alphanumeric identifier", example = "A1")
        String seatLabel,

        @Schema(description = "Seat price", example = "49.99")
        BigDecimal price,

        @Schema(description = "Current seat booking status", example = "AVAILABLE")
        SeatStatus status
) {

    public static SeatResponse fromEntity(Seat seat) {

        if (seat == null)
            return null;

        return new SeatResponse(
                seat.getId(),
                seat.getEvent() != null ? seat.getEvent().getId() : null,
                seat.getSeatLabel(),
                seat.getPrice(),
                seat.getStatus()
        );
    }
}
