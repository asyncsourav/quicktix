

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;




/**
 * Integration test verifying the full booking lifecycle against a real PostgreSQL container via Testcontainers.
 * Validates real SQL transactions, foreign keys, and Hibernate @Version optimistic locking without mocks.
 */
class BookingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookingService bookingService;

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
        // Clean up database state in reverse foreign-key dependency order
        seatRepository.deleteAll();
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Persist real User in PostgreSQL
        testUser = userRepository.save(User.builder()
                .name("Alice Attendee")
                .email("alice.integration@quicktix.com")
                .passwordHash("$2a$10$hashedpasswordfore2etesting")
                .role(Role.USER)
                .build());

        // 2. Persist real Venue in PostgreSQL
        testVenue = venueRepository.save(Venue.builder()
                .name("O2 Arena London")
                .address("Peninsula Square, London")
                .totalCapacity(20000)
                .organizer(testUser)
                .build());

        // 3. Persist real Event in PostgreSQL
        testEvent = eventRepository.save(Event.builder()
                .title("Coldplay Live in London")
                .category("Music")
                .description("World tour headline performance")
                .basePrice(new BigDecimal("85.00"))
                .startTime(LocalDateTime.now().plusMonths(2))
                .venue(testVenue)
                .organizer(testUser)
                .build());

        // 4. Persist real Seats in PostgreSQL with initial version = 0
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

        // Given: Booking request for 2 seats
        BookingRequest request = new BookingRequest(
                testEvent.getId(), 
                List.of(testSeat1.getId(), 
                testSeat2.getId())
        );

        // When: Execute createBooking through the service layer
        BookingResponse response = bookingService.createBooking(
                request, 
                testUser.getEmail()
        );

        // Then 1: Verify API response
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("170.00"));
        assertThat(response.seats()).hasSize(2);

        // Then 2: Verify real database persistence in PostgreSQL
        Booking persistedBooking = bookingRepository.findById(response.id()).orElseThrow();
        assertThat(persistedBooking.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(persistedBooking.getEvent().getId()).isEqualTo(testEvent.getId());
        assertThat(persistedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(persistedBooking.getTotalAmount()).isEqualByComparingTo(new BigDecimal("170.00"));

        // Then 3: Verify seats transitioned to BOOKED and @Version incremented from 0 to 1
        Seat seat1FromDb = seatRepository.findById(testSeat1.getId()).orElseThrow();
        assertThat(seat1FromDb.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(seat1FromDb.getBooking().getId()).isEqualTo(persistedBooking.getId());
        assertThat(seat1FromDb.getVersion()).isEqualTo(1L); // Verifies Hibernate @Version increment in PostgreSQL

        Seat seat2FromDb = seatRepository.findById(testSeat2.getId()).orElseThrow();
        assertThat(seat2FromDb.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(seat2FromDb.getBooking().getId()).isEqualTo(persistedBooking.getId());
        assertThat(seat2FromDb.getVersion()).isEqualTo(1L);

        // Then 4: Verify external side-effects (STOMP broadcast & async email)
        verify(seatBroadcastService).broadcastSeatStatusBatch(eq(testEvent.getId()), any());
        verify(emailService).sendBookingConfirmation(any());
    }



    
    @Test
    @DisplayName("Cancellation flow releases seats back to AVAILABLE and increments @Version in real PostgreSQL")
    void fullCancellationLifecycle_AgainstRealPostgresContainer() {

        // Given: Create a confirmed booking first
        BookingRequest request = new BookingRequest(
                testEvent.getId(), 
                List.of(testSeat1.getId()
        ));

        BookingResponse bookingResponse = bookingService.createBooking(
                request, 
                testUser.getEmail()
        );

        // When: Cancel the booking
        BookingResponse cancelResponse = bookingService.cancelBooking(
                bookingResponse.id(), 
                testUser.getEmail()
        );

        // Then 1: Verify cancellation response
        assertThat(cancelResponse.status()).isEqualTo(BookingStatus.CANCELLED);

        // Then 2: Verify database state in PostgreSQL
        Booking cancelledBooking = bookingRepository.findById(bookingResponse.id()).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        // Then 3: Verify seat status returned to AVAILABLE and @Version incremented to 2
        Seat seatAfterCancel = seatRepository.findById(testSeat1.getId()).orElseThrow();
        assertThat(seatAfterCancel.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seatAfterCancel.getBooking()).isNull();
        assertThat(seatAfterCancel.getVersion()).isEqualTo(2L); // 0 (create) -> 1 (booked) -> 2 (cancelled)
    }
}
