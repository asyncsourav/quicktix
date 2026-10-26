

package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.SeatStatus;
import io.swagger.v3.oas.annotations.media.Schema;



/**
 * WebSocket payload sent when a seat status changes (AVAILABLE, HELD, BOOKED).
 * Broadcast to STOMP topic: /topic/event.{eventId}
 */
@Schema(description = "Real-time WebSocket payload for seat status changes")
public record SeatStatusUpdate(

        @Schema(description = "Seat unique ID", example = "10")
        Long seatId,

        @Schema(description = "Seat alphanumeric identifier", example = "A1")
        String seatLabel,

        @Schema(description = "Updated seat availability status", example = "BOOKED")
        SeatStatus status,

        @Schema(description = "Associated event ID", example = "1")
        Long eventId
) {}
