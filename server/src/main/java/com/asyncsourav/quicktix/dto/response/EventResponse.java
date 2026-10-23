

package com.asyncsourav.quicktix.dto.response;


import com.asyncsourav.quicktix.entity.Event;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;



/*
@GetMapping("/api/events/{id}")
public Event getEventNoob(@PathVariable Long id) {
    return eventRepository.findById(id).get(); // Throws LazyInitializationException!
}
*/


@Schema(description = "Event details response")
public record EventResponse(
        @Schema(description = "Event unique ID", example = "1")
        Long id,

        @Schema(description = "Event title", example = "Coldplay Music of the Spheres Tour")
        String title,

        @Schema(description = "Event description", example = "Live stadium concert featuring guest performances.")
        String description,

        @Schema(description = "Venue ID", example = "1")
        Long venueId,

        @Schema(description = "Venue name", example = "Madison Square Garden")
        String venueName,

        @Schema(description = "Venue address", example = "4 Pennsylvania Plaza, New York, NY 10001")
        String venueAddress,

        @Schema(description = "Event start timestamp", example = "2026-11-20T19:30:00")
        LocalDateTime startTime,

        @Schema(description = "Category", example = "Concert")
        String category,

        @Schema(description = "Base ticket price", example = "89.99")
        BigDecimal basePrice,

        @Schema(description = "Organizer user ID", example = "2")
        Long organizerId,

        @Schema(description = "Organizer user name", example = "Event Corp")
        String organizerName,

        @Schema(description = "Creation timestamp", example = "2026-09-25T12:00:00")
        LocalDateTime createdAt
) implements Serializable {

    private static final long serialVersionUID = 1L;

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
