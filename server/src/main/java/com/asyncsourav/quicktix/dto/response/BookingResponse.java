
package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.Booking;
import com.asyncsourav.quicktix.entity.BookingStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Schema(description = "Confirmed or cancelled booking response")
public record BookingResponse(
        @Schema(description = "Booking unique ID", example = "1")
        Long id,

        @Schema(description = "Booking owner user ID", example = "3")
        Long userId,

        @Schema(description = "Booking owner name", example = "Alex Morgan")
        String userName,

        @Schema(description = "Event ID", example = "1")
        Long eventId,

        @Schema(description = "Event title", example = "Coldplay Music of the Spheres Tour")
        String eventTitle,

        @Schema(description = "List of booked seats")
        List<SeatResponse> seats,

        @Schema(description = "Total booking cost", example = "99.98")
        BigDecimal totalAmount,

        @Schema(description = "Booking status", example = "CONFIRMED")
        BookingStatus status,

        @Schema(description = "Booking confirmation timestamp", example = "2026-09-25T12:00:00")
        LocalDateTime createdAt
) {

    public static BookingResponse fromEntity(Booking booking) {

        if (booking == null)
            return null;

        List<SeatResponse> seatResponses = Collections.emptyList();

        if (booking.getSeats() != null) {
            seatResponses = booking.getSeats()
                    .stream()
                    .map(seat -> {
                        return SeatResponse.fromEntity(seat);
                    })
                    .toList();
        }

        return new BookingResponse(
                booking.getId(),
                booking.getUser() != null ? booking.getUser().getId() : null,
                booking.getUser() != null ? booking.getUser().getName() : null,
                booking.getEvent() != null ? booking.getEvent().getId() : null,
                booking.getEvent() != null ? booking.getEvent().getTitle() : null,
                seatResponses,
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
