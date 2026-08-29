
package com.asyncsourav.quicktix.dto.response;

import com.asyncsourav.quicktix.entity.Venue;

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

public record VenueResponse(
        Long id,
        String name,
        String address,
        Integer totalCapacity,
        Long organizerId,
        String organizerName,
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
