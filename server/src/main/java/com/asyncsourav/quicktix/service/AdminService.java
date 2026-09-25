

package com.asyncsourav.quicktix.service;



import com.asyncsourav.quicktix.dto.response.AdminStatsResponse;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.entity.Booking;
import com.asyncsourav.quicktix.entity.BookingStatus;
import com.asyncsourav.quicktix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;



@Service
@RequiredArgsConstructor
public class AdminService {


    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;


    /**
     * Aggregates platform statistics for the Admin analytics dashboard.
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalVenues = venueRepository.count();
        long totalEvents = eventRepository.count();

        List<Booking> allBookings = bookingRepository.findAll();
        long totalBookings = allBookings.size();

        long confirmedBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        long cancelledBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .count();

        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminStatsResponse(
                totalUsers,
                totalVenues,
                totalEvents,
                totalBookings,
                totalRevenue,
                confirmedBookings,
                cancelledBookings
        );
    }



    /**
     * Lists all registered users on the platform.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }



    /**
     * Lists all bookings across the entire platform.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }
}
