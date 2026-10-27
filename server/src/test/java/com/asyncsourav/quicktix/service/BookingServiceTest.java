

package com.asyncsourav.quicktix.service;


import com.asyncsourav.quicktix.dto.email.BookingConfirmationEmailDetails;
import com.asyncsourav.quicktix.dto.request.BookingRequest;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.entity.*;
import com.asyncsourav.quicktix.exception.SeatUnavailableException;
import com.asyncsourav.quicktix.repository.BookingRepository;
import com.asyncsourav.quicktix.repository.EventRepository;
import com.asyncsourav.quicktix.repository.SeatRepository;
import com.asyncsourav.quicktix.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SeatBroadcastService seatBroadcastService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingService bookingService;


    private User testUser;
    private Venue testVenue;
    private Event testEvent;
    private Seat testSeat1;
    private Seat testSeat2;



    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Sourav Kumar")
                .email("user@quicktix.com")
                .role(Role.USER)
                .build();

        testVenue = Venue.builder()
                .id(10L)
                .name("Metro Arena")
                .build();

        testEvent = Event.builder()
                .id(100L)
                .title("Rock Festival 2026")
                .venue(testVenue)
                .startTime(LocalDateTime.of(2026, 8, 15, 19, 30))
                .basePrice(new BigDecimal("50.00"))
                .build();

        testSeat1 = Seat.builder()
                .id(1001L)
                .seatLabel("A1")
                .event(testEvent)
                .status(SeatStatus.AVAILABLE)
                .price(new BigDecimal("50.00"))
                .version(0L)
                .build();

        testSeat2 = Seat.builder()
                .id(1002L)
                .seatLabel("A2")
                .event(testEvent)
                .status(SeatStatus.AVAILABLE)
                .price(new BigDecimal("50.00"))
                .version(0L)
                .build();
    }




    @Test
    @DisplayName("createBooking: Confirms booking, transitions seats to BOOKED, broadcasts STOMP and dispatches async email")
    void createBooking_Success() {

        BookingRequest request = new BookingRequest(100L, List.of(1001L, 1002L));
        List<Seat> seats = new ArrayList<>(List.of(testSeat1, testSeat2));

        when(userRepository.findByEmail("user@quicktix.com")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));
        when(seatRepository.findAllById(List.of(1001L, 1002L))).thenReturn(seats);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request, "user@quicktix.com");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(testSeat1.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(testSeat2.getStatus()).isEqualTo(SeatStatus.BOOKED);

        verify(seatRepository).saveAll(seats);
        verify(seatBroadcastService).broadcastSeatStatusBatch(eq(100L), any());
        verify(emailService).sendBookingConfirmation(any(BookingConfirmationEmailDetails.class));
    }




    @Test
    @DisplayName("OPTIMISTIC LOCKING RACE: Simulates concurrent collision (OptimisticLockException) during seat save and rolls back")
    void createBooking_WhenOptimisticLockCollisionOccurs_ShouldThrowOptimisticLockingFailureException() {

        // Given: Concurrent transaction already incremented @Version on testSeat1
        BookingRequest request = new BookingRequest(100L, List.of(1001L));
        List<Seat> seats = new ArrayList<>(List.of(testSeat1));

        when(userRepository.findByEmail("user@quicktix.com")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));
        when(seatRepository.findAllById(List.of(1001L))).thenReturn(seats);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(501L);
            return b;
        });

        // Simulate Hibernate @Version collision exception on saveAll
        when(seatRepository.saveAll(any())).thenThrow(
                new ObjectOptimisticLockingFailureException(Seat.class.getName(), 1001L)
        );

        // When & Then: Concurrency conflict must bubble up to trigger rollback and 409 Conflict
        assertThatThrownBy(() -> bookingService.createBooking(request, "user@quicktix.com"))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // STOMP broadcast and email dispatch MUST NEVER be triggered if persistence collides
        verify(seatBroadcastService, never()).broadcastSeatStatusBatch(any(), any());
        verify(emailService, never()).sendBookingConfirmation(any());
    }




    @Test
    @DisplayName("createBooking: Fails when requested seat is already BOOKED (SeatUnavailableException)")
    void createBooking_WhenSeatAlreadyBooked_ShouldThrowSeatUnavailableException() {

        testSeat1.setStatus(SeatStatus.BOOKED);
        BookingRequest request = new BookingRequest(100L, List.of(1001L));

        when(userRepository.findByEmail("user@quicktix.com")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));
        when(seatRepository.findAllById(List.of(1001L))).thenReturn(List.of(testSeat1));

        assertThatThrownBy(() -> bookingService.createBooking(request, "user@quicktix.com"))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("Seat is currently BOOKED");

        verify(bookingRepository, never()).save(any());
        verify(seatRepository, never()).saveAll(any());
        verify(emailService, never()).sendBookingConfirmation(any());
    }




    @Test
    @DisplayName("cancelBooking: Releases seats to AVAILABLE, broadcasts update and sends cancellation email")
    void cancelBooking_Success() {
        
        Booking booking = Booking.builder()
                .id(500L)
                .user(testUser)
                .event(testEvent)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("100.00"))
                .seats(new ArrayList<>(List.of(testSeat1, testSeat2)))
                .build();

        testSeat1.setStatus(SeatStatus.BOOKED);
        testSeat1.setBooking(booking);
        testSeat2.setStatus(SeatStatus.BOOKED);
        testSeat2.setBooking(booking);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("user@quicktix.com")).thenReturn(Optional.of(testUser));
        when(seatRepository.findByBookingId(500L)).thenReturn(List.of(testSeat1, testSeat2));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(500L, "user@quicktix.com");

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(testSeat1.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(testSeat1.getBooking()).isNull();
        assertThat(testSeat2.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(testSeat2.getBooking()).isNull();

        verify(seatRepository).saveAll(any());
        verify(seatBroadcastService).broadcastSeatStatusBatch(eq(100L), any());
        verify(emailService).sendBookingCancellation(eq("user@quicktix.com"), eq("Sourav Kumar"), eq(500L), eq("Rock Festival 2026"), any());
    }
}
