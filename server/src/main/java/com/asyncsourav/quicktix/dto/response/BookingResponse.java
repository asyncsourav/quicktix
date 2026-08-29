
package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.Booking;
import com.asyncsourav.quicktix.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


public record BookingResponse(
        Long id,
        Long userId,
        String userName,
        Long eventId,
        String eventTitle,
        List<SeatResponse> seats,
        BigDecimal totalAmount,
        BookingStatus status,
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
