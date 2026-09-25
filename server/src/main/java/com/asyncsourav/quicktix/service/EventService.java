

package com.asyncsourav.quicktix.service;


import com.asyncsourav.quicktix.dto.request.EventCreateRequest;
import com.asyncsourav.quicktix.dto.response.EventResponse;
import com.asyncsourav.quicktix.dto.response.SeatResponse;
import com.asyncsourav.quicktix.entity.*;
import com.asyncsourav.quicktix.exception.ResourceNotFoundException;
import com.asyncsourav.quicktix.repository.EventRepository;
import com.asyncsourav.quicktix.repository.SeatRepository;
import com.asyncsourav.quicktix.repository.UserRepository;
import com.asyncsourav.quicktix.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;



@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;

    /**
     * Creates an event and auto-generates all initial seats based on venue capacity.
     */
    @Transactional
    public EventResponse createEvent(EventCreateRequest request, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", organizerEmail));

        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.venueId()));

        boolean isAdmin = organizer.getRole() == Role.ADMIN;
        boolean isVenueOwner = venue.getOrganizer().getId().equals(organizer.getId());

        if (!isAdmin && !isVenueOwner) {
            log.warn("Organizer {} tried to schedule event at venue ID {} owned by another organizer",
                    organizerEmail, venue.getId());
            throw new AccessDeniedException("You can only schedule events at venues you own.");
        }

        Event event = Event.builder()
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .venue(venue)
                .startTime(request.startTime())
                .category(request.category().trim())
                .basePrice(request.basePrice())
                .organizer(organizer)
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event '{}' created with ID: {}", savedEvent.getTitle(), savedEvent.getId());

        // Auto-generate seats for this event based on venue capacity (e.g., A1, A2, ...)
        generateSeatsForEvent(savedEvent, venue.getTotalCapacity());

        return EventResponse.fromEntity(savedEvent);
    }

    /**
     * Helper method to generate seat records for an event.
     */
    private void generateSeatsForEvent(Event event, int capacity) {
        List<Seat> seats = new ArrayList<>(capacity);
        int seatsPerRow = 10;

        for (int i = 0; i < capacity; i++) {
            char rowLetter = (char) ('A' + (i / seatsPerRow));
            int seatNumber = (i % seatsPerRow) + 1;
            String seatLabel = String.format("%c%d", rowLetter, seatNumber);

            Seat seat = Seat.builder()
                    .event(event)
                    .seatLabel(seatLabel)
                    .price(event.getBasePrice())
                    .status(SeatStatus.AVAILABLE)
                    .build();

            seats.add(seat);
        }

        seatRepository.saveAll(seats);
        log.info("Generated {} seats for event ID: {}", seats.size(), event.getId());
    }

    /**
     * Retrieves events with optional category and search keyword filters.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(String category, String search) {
        List<Event> events;

        if (category != null && !category.isBlank()) {
            events = eventRepository.findByCategoryIgnoreCase(category.trim());
        } else if (search != null && !search.isBlank()) {
            events = eventRepository.findByTitleContainingIgnoreCase(search.trim());
        } else {
            events = eventRepository.findAll();
        }

        return events.stream().map(EventResponse::fromEntity).toList();
    }

    /**
     * Retrieves a single event by ID.
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));

        return EventResponse.fromEntity(event);
    }

    /**
     * Retrieves the real-time interactive seat map for an event.
     */
    @Transactional(readOnly = true)
    public List<SeatResponse> getEventSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }

        return seatRepository.findByEventIdOrderBySeatLabelAsc(eventId)
                .stream()
                .map(SeatResponse::fromEntity)
                .toList();
    }

    /**
     * Updates an existing event. Restricted to the organizer or an ADMIN.
     */
    @Transactional
    public EventResponse updateEvent(Long id, EventCreateRequest request, String currentUserEmail) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = event.getOrganizer().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            log.warn("Unauthorized event update attempt on event ID: {} by user: {}", id, currentUserEmail);
            throw new AccessDeniedException("You do not have permission to modify this event.");
        }

        event.setTitle(request.title().trim());
        event.setDescription(request.description() != null ? request.description().trim() : null);
        event.setStartTime(request.startTime());
        event.setCategory(request.category().trim());
        event.setBasePrice(request.basePrice());

        Event updatedEvent = eventRepository.save(event);
        log.info("Event ID: {} updated successfully by: {}", id, currentUserEmail);

        return EventResponse.fromEntity(updatedEvent);
    }

    /**
     * Deletes an event. Restricted to the organizer or an ADMIN.
     */
    @Transactional
    public void deleteEvent(Long id, String currentUserEmail) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = event.getOrganizer().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            log.warn("Unauthorized event deletion attempt on event ID: {} by user: {}", id, currentUserEmail);
            throw new AccessDeniedException("You do not have permission to delete this event.");
        }

        eventRepository.delete(event);
        log.info("Event ID: {} deleted successfully by: {}", id, currentUserEmail);
    }
}
