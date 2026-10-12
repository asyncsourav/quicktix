
package com.asyncsourav.quicktix.dto.response;

import com.asyncsourav.quicktix.entity.Venue;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;


/*

public class VenueResponseNoob {
    private Long id;
    private String name;
    private String address;
    private Integer totalCapacity;
    private User organizer; // Full user entity serialized!
}
*/

@Schema(description = "Venue details response")
public record VenueResponse(
        @Schema(description = "Venue unique ID", example = "1")
        Long id,

        @Schema(description = "Venue name", example = "Madison Square Garden")
        String name,

        @Schema(description = "Venue physical address", example = "4 Pennsylvania Plaza, New York, NY 10001")
        String address,

        @Schema(description = "Total seating capacity", example = "100")
        Integer totalCapacity,

        @Schema(description = "Organizer user ID", example = "2")
        Long organizerId,

        @Schema(description = "Organizer user name", example = "Event Corp")
        String organizerName,

        @Schema(description = "Creation timestamp", example = "2026-09-25T12:00:00")
        LocalDateTime createdAt
) {
    public static VenueResponse fromEntity(Venue venue) {

        if (venue == null)
            return null;

        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getTotalCapacity(),
                venue.getOrganizer() != null ? venue.getOrganizer().getId() : null,
                venue.getOrganizer() != null ? venue.getOrganizer().getName() : null,
                venue.getCreatedAt()
        );
    }
}
