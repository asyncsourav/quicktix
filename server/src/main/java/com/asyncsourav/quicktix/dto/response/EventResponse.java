
package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/*
@GetMapping("/api/events/{id}")
public Event getEventNoob(@PathVariable Long id) {
    return eventRepository.findById(id).get(); // Throws LazyInitializationException!
}
*/


public record EventResponse(
        Long id,
        String title,
        String description,
        Long venueId,
        String venueName,
        String venueAddress,
        LocalDateTime startTime,
        String category,
        BigDecimal basePrice,
        Long organizerId,
        String organizerName,
        LocalDateTime createdAt
) {

    public static EventResponse fromEntity(Event event) {

        if (event == null)
            return null;

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getVenue() != null ? event.getVenue().getId() : null,
                event.getVenue() != null ? event.getVenue().getName() : null,
                event.getVenue() != null ? event.getVenue().getAddress() : null,
                event.getStartTime(),
                event.getCategory(),
                event.getBasePrice(),
                event.getOrganizer() != null ? event.getOrganizer().getId() : null,
                event.getOrganizer() != null ? event.getOrganizer().getName() : null,
                event.getCreatedAt()
        );
    }
}
