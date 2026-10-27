

package com.asyncsourav.quicktix.service;



import com.asyncsourav.quicktix.BaseIntegrationTest;
import com.asyncsourav.quicktix.config.DataInitializer;
import com.asyncsourav.quicktix.dto.response.EventResponse;
import com.asyncsourav.quicktix.entity.Event;
import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import com.asyncsourav.quicktix.entity.Venue;
import com.asyncsourav.quicktix.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;




class EventCacheTest extends BaseIntegrationTest {


    @Autowired
    private EventService eventService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private DataInitializer dataInitializer;

    private Event testEvent;


    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("events") != null) {
            cacheManager.getCache("events").clear();
        }
        if (cacheManager.getCache("event") != null) {
            cacheManager.getCache("event").clear();
        }

        User organizer = User.builder()
                .id(1L)
                .name("Organizer")
                .email("org@test.com")
                .role(Role.ORGANIZER)
                .build();

        Venue venue = Venue.builder()
                .id(1L)
                .name("Grand Arena")
                .address("123 Main St")
                .totalCapacity(500)
                .organizer(organizer)
                .build();

        testEvent = Event.builder()
                .id(1L)
                .title("Tech Conference 2026")
                .category("Technology")
                .description("Annual Developer Summit")
                .basePrice(BigDecimal.valueOf(99.99))
                .startTime(LocalDateTime.now().plusDays(10))
                .venue(venue)
                .organizer(organizer)
                .build();
    }



    @Test
    @DisplayName("getAllEvents caches results on repeated calls")
    void getAllEvents_cachesResults() {
        when(eventRepository.findAll()).thenReturn(List.of(testEvent));

        // First call - should invoke repository
        List<EventResponse> firstResult = eventService.getAllEvents(null, null);
        assertThat(firstResult).hasSize(1);
        verify(eventRepository, times(1)).findAll();

        // Second call with same params - should hit cache
        List<EventResponse> secondResult = eventService.getAllEvents(null, null);
        assertThat(secondResult).hasSize(1);
        verify(eventRepository, times(1)).findAll();
    }


    
    @Test
    @DisplayName("getEventById caches event details by ID")
    void getEventById_cachesResult() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // First call - invokes repository
        EventResponse first = eventService.getEventById(1L);
        assertThat(first.id()).isEqualTo(1L);
        verify(eventRepository, times(1)).findById(1L);

        // Second call - hits cache
        EventResponse second = eventService.getEventById(1L);
        assertThat(second.id()).isEqualTo(1L);
        verify(eventRepository, times(1)).findById(1L);
    }
}
