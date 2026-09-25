

package com.asyncsourav.quicktix.service;


import com.asyncsourav.quicktix.dto.request.BookingRequest;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.entity.*;
import com.asyncsourav.quicktix.exception.BadRequestException;
import com.asyncsourav.quicktix.exception.ResourceNotFoundException;
import com.asyncsourav.quicktix.exception.SeatUnavailableException;
import com.asyncsourav.quicktix.repository.BookingRepository;
import com.asyncsourav.quicktix.repository.EventRepository;
import com.asyncsourav.quicktix.repository.SeatRepository;
import com.asyncsourav.quicktix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;




@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {


    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;


    /**
     * Creates a new booking with Optimistic Locking concurrency protection.
     * If any seat is taken or concurrent modification occurs, the transaction rolls back.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", 
                        "email", 
                        userEmail
                ));

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event", 
                        "id", 
                        request.eventId()
                ));

        List<Seat> requestedSeats = seatRepository.findAllById(request.seatIds());

        if (requestedSeats.size() != request.seatIds().size()) {
            throw new BadRequestException("One or more selected seat IDs do not exist.");
        }

        // Validate event ownership and availability for each seat
        for (Seat seat : requestedSeats) {
            if (!seat.getEvent().getId().equals(event.getId())) {
                throw new BadRequestException("Seat '" + seat.getSeatLabel() + "' does not belong to event ID: " + event.getId());
            }
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                log.warn(
                        "Booking conflict: Seat '{}' (ID: {}) status is {}", 
                        seat.getSeatLabel(), 
                        seat.getId(), 
                        seat.getStatus()
                );
                throw new SeatUnavailableException(
                        seat.getId(), 
                        seat.getSeatLabel(), 
                        "Seat is currently " + seat.getStatus()
                );
            }
        }

        // Calculate total amount from seat prices
        BigDecimal totalAmount = requestedSeats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build and persist Booking
        Booking booking = Booking.builder()
                .user(user)
                .event(event)
                .totalAmount(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .seats(new ArrayList<>())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Transition seats to BOOKED (Hibernate checks @Version automatically on save)
        for (Seat seat : requestedSeats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBooking(savedBooking);
            savedBooking.getSeats().add(seat);
        }

        seatRepository.saveAll(requestedSeats);
        log.info("Booking ID: {} confirmed for user: {} with {} seats. Total: {}",
                savedBooking.getId(), userEmail, requestedSeats.size(), totalAmount);

        return BookingResponse.fromEntity(savedBooking);
    }

    /**
     * Retrieves all bookings made by the authenticated user (newest first).
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a booking by ID. Restricted to owner or ADMIN.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, String userEmail) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = booking.getUser().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to view this booking.");
        }

        return BookingResponse.fromEntity(booking);
    }

    /**
     * Cancels a booking and releases all its reserved seats back to AVAILABLE.
     */
    @Transactional
    public BookingResponse cancelBooking(Long id, String userEmail) {
        
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = booking.getUser().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to cancel this booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("This booking is already cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Release seats back to AVAILABLE
        List<Seat> seats = seatRepository.findByBookingId(id);
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setBooking(null);
        }
        seatRepository.saveAll(seats);

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking ID: {} cancelled by: {}. Released {} seats.", id, userEmail, seats.size());

        return BookingResponse.fromEntity(updatedBooking);
    }
}
