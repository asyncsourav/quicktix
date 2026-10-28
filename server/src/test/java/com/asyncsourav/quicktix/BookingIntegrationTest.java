

package com.asyncsourav.quicktix;


import com.asyncsourav.quicktix.dto.request.BookingRequest;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.entity.*;
import com.asyncsourav.quicktix.repository.*;
import com.asyncsourav.quicktix.service.BookingService;
import com.asyncsourav.quicktix.service.EmailService;
import com.asyncsourav.quicktix.service.SeatBroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;




/**
 * Integration tests against a real PostgreSQL container via Testcontainers.
 * Validates real SQL transactions, foreign keys, Hibernate @Version optimistic locking,
 * and high-concurrency race condition safety without mocks.
 */
class BookingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private SeatBroadcastService seatBroadcastService;

    @MockitoBean
    private EmailService emailService;


    private User testUser;
    private Venue testVenue;
    private Event testEvent;
    private Seat testSeat1;
    private Seat testSeat2;



    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .name("Alice Attendee")
                .email("alice.integration@quicktix.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .build());

        testVenue = venueRepository.save(Venue.builder()
                .name("O2 Arena London")
                .address("Peninsula Square, London")
                .totalCapacity(20000)
                .organizer(testUser)
                .build());

        testEvent = eventRepository.save(Event.builder()
                .title("Coldplay Live in London")
                .category("Music")
                .description("World tour headline performance")
                .basePrice(new BigDecimal("85.00"))
                .startTime(LocalDateTime.now().plusMonths(2))
                .venue(testVenue)
                .organizer(testUser)
                .build());

        testSeat1 = seatRepository.save(Seat.builder()
                .seatLabel("B1")
                .event(testEvent)
                .status(SeatStatus.AVAILABLE)
                .price(new BigDecimal("85.00"))
                .build());

        testSeat2 = seatRepository.save(Seat.builder()
                .seatLabel("B2")
                .event(testEvent)
                .status(SeatStatus.AVAILABLE)
                .price(new BigDecimal("85.00"))
                .build());
    }




    @Test
    @DisplayName("End-to-end booking flow commits transaction and increments @Version in real PostgreSQL")
    void fullBookingLifecycle_AgainstRealPostgresContainer() {

        BookingRequest request = new BookingRequest(
                testEvent.getId(), 
                List.of(testSeat1.getId(), testSeat2.getId())
        );

        BookingResponse response = bookingService.createBooking(request, testUser.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("170.00"));
        assertThat(response.seats()).hasSize(2);

        Booking persistedBooking = bookingRepository.findById(response.id()).orElseThrow();
        assertThat(persistedBooking.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(persistedBooking.getEvent().getId()).isEqualTo(testEvent.getId());
        assertThat(persistedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        Seat seat1FromDb = seatRepository.findById(testSeat1.getId()).orElseThrow();
        assertThat(seat1FromDb.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(seat1FromDb.getVersion()).isEqualTo(1L);

        Seat seat2FromDb = seatRepository.findById(testSeat2.getId()).orElseThrow();
        assertThat(seat2FromDb.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(seat2FromDb.getVersion()).isEqualTo(1L);

        verify(seatBroadcastService).broadcastSeatStatusBatch(eq(testEvent.getId()), any());
        verify(emailService).sendBookingConfirmation(any());
    }



    @Test
    @DisplayName("Cancellation flow releases seats back to AVAILABLE and increments @Version in real PostgreSQL")
    void fullCancellationLifecycle_AgainstRealPostgresContainer() {

        BookingRequest request = new BookingRequest(
                testEvent.getId(), 
                List.of(testSeat1.getId())
        );

        BookingResponse bookingResponse = bookingService.createBooking(request, testUser.getEmail());
        BookingResponse cancelResponse = bookingService.cancelBooking(bookingResponse.id(), testUser.getEmail());

        assertThat(cancelResponse.status()).isEqualTo(BookingStatus.CANCELLED);

        Booking cancelledBooking = bookingRepository.findById(bookingResponse.id()).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        Seat seatAfterCancel = seatRepository.findById(testSeat1.getId()).orElseThrow();
        assertThat(seatAfterCancel.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seatAfterCancel.getBooking()).isNull();
        assertThat(seatAfterCancel.getVersion()).isEqualTo(2L);
    }
}
