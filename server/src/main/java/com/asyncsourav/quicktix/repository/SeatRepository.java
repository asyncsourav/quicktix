
package com.asyncsourav.quicktix.repository;


import com.asyncsourav.quicktix.entity.Seat;
import com.asyncsourav.quicktix.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;



public interface SeatRepository extends JpaRepository<Seat, Long> {


    List<Seat> findByEventIdOrderBySeatLabelAsc(Long eventId);
    List<Seat> findByEventIdAndStatus(Long eventId, SeatStatus status);
    Optional<Seat> findByEventIdAndSeatLabel(Long eventId, String seatLabel);
    long countByEventIdAndStatus(Long eventId, SeatStatus status);
    List<Seat> findByBookingId(Long bookingId);
}
