

package com.asyncsourav.quicktix.controller;



import com.asyncsourav.quicktix.dto.common.ApiResponse;
import com.asyncsourav.quicktix.dto.request.VenueCreateRequest;
import com.asyncsourav.quicktix.dto.response.VenueResponse;
import com.asyncsourav.quicktix.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@Tag(name = "Venues", description = "Endpoints for venue setup and management")
@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {


    private final VenueService venueService;

    
    /**
     * POST /api/venues
     * Creates a new venue. Restricted to ORGANIZER and ADMIN roles.
     */
    @Operation(summary = "Create venue", description = "Creates a new venue with name, address, and seating capacity. Restricted to ORGANIZER or ADMIN role.")
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
    @Operation(summary = "Get all venues", description = "Public endpoint to retrieve all registered venues.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues() {
        List<VenueResponse> venues = venueService.getAllVenues();
        return ResponseEntity.ok(ApiResponse.success("Venues fetched successfully", venues));
    }

    /**
     * GET /api/venues/{id}
     * Returns a specific venue by ID (public access).
     */
    @Operation(summary = "Get venue by ID", description = "Public endpoint to retrieve details for a specific venue.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(@PathVariable Long id) {
        VenueResponse venue = venueService.getVenueById(id);
        return ResponseEntity.ok(ApiResponse.success("Venue fetched successfully", venue));
    }

    /**
     * GET /api/venues/organizer/{organizerId}
     * Returns all venues created by a specific organizer.
     */
    @Operation(summary = "Get organizer venues", description = "Retrieves all venues managed by a specific organizer.")
    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getVenuesByOrganizer(@PathVariable Long organizerId) {
        List<VenueResponse> venues = venueService.getVenuesByOrganizer(organizerId);
        return ResponseEntity.ok(ApiResponse.success("Organizer venues fetched successfully", venues));
    }

    /**
     * PUT /api/venues/{id}
     * Updates an existing venue. Restricted to owning ORGANIZER or ADMIN.
     */
    @Operation(summary = "Update venue", description = "Updates venue details. Restricted to the owning organizer or admin.")
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
    @Operation(summary = "Delete venue", description = "Deletes a venue. Restricted to the owning organizer or admin.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        venueService.deleteVenue(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Venue deleted successfully", null));
    }
}
