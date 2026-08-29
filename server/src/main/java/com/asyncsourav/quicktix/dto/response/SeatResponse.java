
package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.Seat;
import com.asyncsourav.quicktix.entity.SeatStatus;

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


public record SeatResponse(
        Long id,
        Long eventId,
        String seatLabel,
        BigDecimal price,
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
