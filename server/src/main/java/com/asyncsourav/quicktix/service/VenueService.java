

package com.asyncsourav.quicktix.service;


import com.asyncsourav.quicktix.dto.request.VenueCreateRequest;
import com.asyncsourav.quicktix.dto.response.VenueResponse;
import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import com.asyncsourav.quicktix.entity.Venue;
import com.asyncsourav.quicktix.exception.ResourceNotFoundException;
import com.asyncsourav.quicktix.repository.UserRepository;
import com.asyncsourav.quicktix.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



@Slf4j
@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new venue linked to the authenticated organizer.
     */
    @Transactional
    public VenueResponse createVenue(VenueCreateRequest request, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", organizerEmail));

        Venue venue = Venue.builder()
                .name(request.name().trim())
                .address(request.address().trim())
                .totalCapacity(request.totalCapacity())
                .organizer(organizer)
                .build();

        Venue savedVenue = venueRepository.save(venue);
        log.info("Venue created successfully with ID: {} by organizer: {}", savedVenue.getId(), organizerEmail);

        return VenueResponse.fromEntity(savedVenue);
    }

    /**
     * Retrieves all venues in the system.
     */
    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll()
                .stream()
                .map(VenueResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single venue by its primary key ID.
     */
    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));

        return VenueResponse.fromEntity(venue);
    }

    /**
     * Retrieves all venues owned by a specific organizer.
     */
    @Transactional(readOnly = true)
    public List<VenueResponse> getVenuesByOrganizer(Long organizerId) {
        return venueRepository.findByOrganizerId(organizerId)
                .stream()
                .map(VenueResponse::fromEntity)
                .toList();
    }

    /**
     * Updates an existing venue. Enforces that only the creator or an ADMIN can perform updates.
     */
    @Transactional
    public VenueResponse updateVenue(Long id, VenueCreateRequest request, String currentUserEmail) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = venue.getOrganizer().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            log.warn("Unauthorized venue update attempt on venue ID: {} by user: {}", id, currentUserEmail);
            throw new AccessDeniedException("You do not have permission to modify this venue.");
        }

        venue.setName(request.name().trim());
        venue.setAddress(request.address().trim());
        venue.setTotalCapacity(request.totalCapacity());

        Venue updatedVenue = venueRepository.save(venue);
        log.info("Venue ID: {} updated successfully by: {}", id, currentUserEmail);

        return VenueResponse.fromEntity(updatedVenue);
    }

    /**
     * Deletes a venue. Enforces owner or ADMIN permissions.
     */
    @Transactional
    public void deleteVenue(Long id, String currentUserEmail) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = venue.getOrganizer().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            log.warn("Unauthorized venue deletion attempt on venue ID: {} by user: {}", id, currentUserEmail);
            throw new AccessDeniedException("You do not have permission to delete this venue.");
        }

        venueRepository.delete(venue);
        log.info("Venue ID: {} deleted successfully by: {}", id, currentUserEmail);
    }
}
