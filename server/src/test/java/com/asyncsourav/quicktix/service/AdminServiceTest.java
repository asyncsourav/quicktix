

package com.asyncsourav.quicktix.service;



import com.asyncsourav.quicktix.dto.response.AdminStatsResponse;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.entity.*;
import com.asyncsourav.quicktix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;




@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AdminService adminService;


    private User organizer;
    private User user;
    private Event event;
    private Venue venue;


    @BeforeEach
    void setUp() {

        organizer = User.builder()
                .id(2L)
                .name("Organizer User")
                .email("organizer@quicktix.com")
                .passwordHash("hash")
                .role(Role.ORGANIZER)
                .build();

        user = User.builder()
                .id(1L)
                .name("Normal User")
                .email("user@quicktix.com")
                .passwordHash("hash2")
                .role(Role.USER)
                .build();

        venue = Venue.builder()
                .id(10L)
                .name("Grand Hall")
                .address("Main Street")
                .totalCapacity(50)
                .organizer(organizer)
                .build();

        event = Event.builder()
                .id(20L)
                .title("Rock Night")
                .description("Live concert")
                .venue(venue)
                .startTime(LocalDateTime.now().plusDays(2))
                .category("Concert")
                .basePrice(new BigDecimal("80.00"))
                .organizer(organizer)
                .build();
    }



    @Test
    @DisplayName("getPlatformStats: Aggregates users, venues, events, bookings and revenue")
    void getPlatformStats_ShouldAggregateMetrics() {
        
        when(userRepository.count()).thenReturn(2L);
        when(venueRepository.count()).thenReturn(1L);
        when(eventRepository.count()).thenReturn(3L);

        Booking confirmedBooking = Booking.builder()
                .id(100L)
                .user(user)
                .event(event)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("120.00"))
                .seats(List.of())
                .build();

        Booking cancelledBooking = Booking.builder()
                .id(101L)
                .user(user)
                .event(event)
                .status(BookingStatus.CANCELLED)
                .totalAmount(new BigDecimal("25.00"))
                .seats(List.of())
                .build();

        Booking secondConfirmedBooking = Booking.builder()
                .id(102L)
                .user(organizer)
                .event(event)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("60.00"))
                .seats(List.of())
                .build();

        when(bookingRepository.findAll()).thenReturn(List.of(confirmedBooking, cancelledBooking, secondConfirmedBooking));

        AdminStatsResponse stats = adminService.getPlatformStats();

        assertThat(stats.totalUsers()).isEqualTo(2L);
        assertThat(stats.totalVenues()).isEqualTo(1L);
        assertThat(stats.totalEvents()).isEqualTo(3L);
        assertThat(stats.totalBookings()).isEqualTo(3L);
        assertThat(stats.confirmedBookings()).isEqualTo(2L);
        assertThat(stats.cancelledBookings()).isEqualTo(1L);
        assertThat(stats.totalRevenue()).isEqualByComparingTo(new BigDecimal("180.00"));
    }



    @Test
    @DisplayName("getAllUsers: Returns mapped user DTO list")
    void getAllUsers_ShouldMapUsersToUserResponses() {

        when(userRepository.findAll()).thenReturn(List.of(user, organizer));

        List<UserResponse> users = adminService.getAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).email()).isEqualTo("user@quicktix.com");
        assertThat(users.get(1).role()).isEqualTo(Role.ORGANIZER);
    }




    @Test
    @DisplayName("getAllBookings: Returns mapped booking DTO list")
    void getAllBookings_ShouldMapBookingsToBookingResponses() {

        Booking booking = Booking.builder()
                .id(300L)
                .user(user)
                .event(event)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("99.99"))
                .seats(List.of())
                .build();

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        List<BookingResponse> bookings = adminService.getAllBookings();

        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).id()).isEqualTo(300L);
        assertThat(bookings.get(0).eventTitle()).isEqualTo("Rock Night");
        assertThat(bookings.get(0).status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
