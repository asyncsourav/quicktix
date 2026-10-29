

package com.asyncsourav.quicktix.service;



import com.asyncsourav.quicktix.dto.request.EventCreateRequest;
import com.asyncsourav.quicktix.dto.response.EventResponse;
import com.asyncsourav.quicktix.dto.response.SeatResponse;
import com.asyncsourav.quicktix.entity.*;
import com.asyncsourav.quicktix.repository.EventRepository;
import com.asyncsourav.quicktix.repository.SeatRepository;
import com.asyncsourav.quicktix.repository.UserRepository;
import com.asyncsourav.quicktix.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private EventService eventService;



    private User organizer;
    private User admin;
    private Venue venue;



    @BeforeEach
    void setUp() {

        organizer = User.builder()
                .id(8L)
                .name("Event Organizer")
                .email("organizer@quicktix.com")
                .passwordHash("hash")
                .role(Role.ORGANIZER)
                .build();

        admin = User.builder()
                .id(99L)
                .name("System Admin")
                .email("admin@quicktix.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .build();

        venue = Venue.builder()
                .id(30L)
                .name("Sunset Arena")
                .address("Lake Road")
                .totalCapacity(12)
                .organizer(organizer)
                .build();
    }




    @Test
    @DisplayName("createEvent: Generates seats and returns event when organizer is allowed")
    void createEvent_ShouldGenerateSeatsAndSaveEvent() {

        EventCreateRequest request = new EventCreateRequest(
                "Future Fest",
                "Annual music festival",
                30L,
                LocalDateTime.now().plusDays(7),
                "Concert",
                new BigDecimal("59.99")
        );

        when(userRepository.findByEmail("organizer@quicktix.com")).thenReturn(Optional.of(organizer));
        when(venueRepository.findById(30L)).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(50L);
            return e;
        });

        EventResponse response = eventService.createEvent(request, "organizer@quicktix.com");

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.title()).isEqualTo("Future Fest");
        assertThat(response.venueId()).isEqualTo(30L);

        verify(seatRepository).saveAll(anyList());
        ArgumentCaptor<List<Seat>> seatCaptor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(seatCaptor.capture());
        assertThat(seatCaptor.getValue()).hasSize(12);
        assertThat(seatCaptor.getValue().get(0).getSeatLabel()).isEqualTo("A1");
    }




    @Test
    @DisplayName("createEvent: Denies organizer scheduling a venue they do not own")
    void createEvent_WhenOrganizerDoesNotOwnVenue_ShouldThrowAccessDenied() {

        User anotherOrganizer = User.builder().id(55L).name("Other Organizer").email("other@quicktix.com").passwordHash("hash").role(Role.ORGANIZER).build();
        EventCreateRequest request = new EventCreateRequest(
                "Future Fest",
                "Annual music festival",
                30L,
                LocalDateTime.now().plusDays(7),
                "Concert",
                new BigDecimal("59.99")
        );

        when(userRepository.findByEmail("other@quicktix.com")).thenReturn(Optional.of(anotherOrganizer));
        when(venueRepository.findById(30L)).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> eventService.createEvent(request, "other@quicktix.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("schedule events at venues you own");

        verify(eventRepository, never()).save(any());
    }




    @Test
    @DisplayName("getAllEvents: Returns filtered event list based on category")
    void getAllEvents_WhenCategoryProvided_ShouldReturnMatchingEvents() {

        Event event = Event.builder()
                .id(10L)
                .title("Summer Jam")
                .description("Festival")
                .venue(venue)
                .startTime(LocalDateTime.now().plusDays(3))
                .category("Concert")
                .basePrice(new BigDecimal("44.00"))
                .organizer(organizer)
                .build();

        when(eventRepository.findByCategoryIgnoreCase("concert")).thenReturn(List.of(event));

        List<EventResponse> events = eventService.getAllEvents("concert", null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).category()).isEqualTo("Concert");
        assertThat(events.get(0).title()).isEqualTo("Summer Jam");
    }




    @Test
    @DisplayName("getEventById: Returns mapped event DTO")
    void getEventById_ShouldReturnEventResponse() {

        Event event = Event.builder()
                .id(99L)
                .title("Launch Night")
                .description("Grand opener")
                .venue(venue)
                .startTime(LocalDateTime.now().plusDays(5))
                .category("Comedy")
                .basePrice(new BigDecimal("35.00"))
                .organizer(organizer)
                .build();

        when(eventRepository.findById(99L)).thenReturn(Optional.of(event));

        EventResponse response = eventService.getEventById(99L);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.organizerName()).isEqualTo("Event Organizer");
    }



    @Test
    @DisplayName("getEventSeats: Returns seat map for an existing event")
    void getEventSeats_ShouldReturnSeatResponses() {

        Seat seat = Seat.builder()
                .id(1001L)
                .seatLabel("A1")
                .event(Event.builder().id(5L).build())
                .status(SeatStatus.AVAILABLE)
                .price(new BigDecimal("42.00"))
                .version(0L)
                .build();

        when(eventRepository.existsById(5L)).thenReturn(true);
        when(seatRepository.findByEventIdOrderBySeatLabelAsc(5L)).thenReturn(List.of(seat));

        List<SeatResponse> seats = eventService.getEventSeats(5L);

        assertThat(seats).hasSize(1);
        assertThat(seats.get(0).seatLabel()).isEqualTo("A1");
        assertThat(seats.get(0).status()).isEqualTo(SeatStatus.AVAILABLE);
    }



    @Test
    @DisplayName("updateEvent: Allows admin to modify an event")
    void updateEvent_ShouldAllowAdminToUpdateEvent() {

        Event event = Event.builder()
                .id(77L)
                .title("Old Title")
                .description("Old")
                .venue(venue)
                .startTime(LocalDateTime.now().plusDays(1))
                .category("Music")
                .basePrice(new BigDecimal("25.00"))
                .organizer(organizer)
                .build();

        EventCreateRequest request = new EventCreateRequest(
                "New Title",
                "Updated description",
                30L,
                LocalDateTime.now().plusDays(9),
                "Comedy",
                new BigDecimal("79.00")
        );

        when(eventRepository.findById(77L)).thenReturn(Optional.of(event));
        when(userRepository.findByEmail("admin@quicktix.com")).thenReturn(Optional.of(admin));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.updateEvent(77L, request, "admin@quicktix.com");

        assertThat(response.title()).isEqualTo("New Title");
        assertThat(response.category()).isEqualTo("Comedy");
    }




    @Test
    @DisplayName("deleteEvent: Deletes event after permission check")
    void deleteEvent_ShouldRemoveEventIfAuthorized() {
        
        Event event = Event.builder()
                .id(120L)
                .title("Delete Me")
                .description("Soon")
                .venue(venue)
                .startTime(LocalDateTime.now().plusDays(2))
                .category("Art")
                .basePrice(new BigDecimal("14.00"))
                .organizer(organizer)
                .build();

        when(eventRepository.findById(120L)).thenReturn(Optional.of(event));
        when(userRepository.findByEmail("organizer@quicktix.com")).thenReturn(Optional.of(organizer));

        eventService.deleteEvent(120L, "organizer@quicktix.com");

        verify(eventRepository).delete(event);
    }
}
