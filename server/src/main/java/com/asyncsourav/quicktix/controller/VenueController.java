

package com.asyncsourav.quicktix.controller;



import com.asyncsourav.quicktix.dto.common.ApiResponse;
import com.asyncsourav.quicktix.dto.request.VenueCreateRequest;
import com.asyncsourav.quicktix.dto.response.VenueResponse;
import com.asyncsourav.quicktix.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    /**
     * POST /api/venues
     * Creates a new venue. Restricted to ORGANIZER and ADMIN roles.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            @Valid @RequestBody VenueCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        VenueResponse response = venueService.createVenue(request, userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Venue created successfully", response));
    }

    /**
     * GET /api/venues
     * Returns all venues (public access).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues() {
        List<VenueResponse> venues = venueService.getAllVenues();
        return ResponseEntity.ok(ApiResponse.success("Venues fetched successfully", venues));
    }

    /**
     * GET /api/venues/{id}
     * Returns a specific venue by ID (public access).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(@PathVariable Long id) {
        VenueResponse venue = venueService.getVenueById(id);
        return ResponseEntity.ok(ApiResponse.success("Venue fetched successfully", venue));
    }

    /**
     * GET /api/venues/organizer/{organizerId}
     * Returns all venues created by a specific organizer.
     */
    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getVenuesByOrganizer(@PathVariable Long organizerId) {
        List<VenueResponse> venues = venueService.getVenuesByOrganizer(organizerId);
        return ResponseEntity.ok(ApiResponse.success("Organizer venues fetched successfully", venues));
    }

    /**
     * PUT /api/venues/{id}
     * Updates an existing venue. Restricted to owning ORGANIZER or ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(
            @PathVariable Long id,
            @Valid @RequestBody VenueCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        VenueResponse response = venueService.updateVenue(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Venue updated successfully", response));
    }

    /**
     * DELETE /api/venues/{id}
     * Deletes an existing venue. Restricted to owning ORGANIZER or ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        venueService.deleteVenue(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Venue deleted successfully", null));
    }
}
