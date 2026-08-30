

package com.asyncsourav.quicktix.repository;


import com.asyncsourav.quicktix.entity.Booking;
import com.asyncsourav.quicktix.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;




public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByEventId(Long eventId);
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);
    long countByEventIdAndStatus(Long eventId, BookingStatus status);
}
