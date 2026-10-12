

package com.asyncsourav.quicktix.controller;


import com.asyncsourav.quicktix.dto.common.ApiResponse;
import com.asyncsourav.quicktix.dto.request.EventCreateRequest;
import com.asyncsourav.quicktix.dto.response.EventResponse;
import com.asyncsourav.quicktix.dto.response.SeatResponse;
import com.asyncsourav.quicktix.service.EventService;
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



@Tag(name = "Events", description = "Endpoints for browsing, creating, and filtering events")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {


    private final EventService eventService;


    /**
     * POST /api/events
     * Creates a new event and auto-populates its seat layout. Restricted to only ORGANIZER and ADMIN.
     */
    @Operation(summary = "Create event", description = "Creates a new event and auto-generates venue seats. Requires ORGANIZER or ADMIN role.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        EventResponse response = eventService.createEvent(request, userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Event created and seats generated successfully", 
                        response
                ));
    }


    /**
     * GET /api/events
     * Public endpoint to list and filter events by category or search keyword.
     */
    @Operation(summary = "List and filter events", description = "Public endpoint to retrieve upcoming events, with optional category and search filters.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        List<EventResponse> events = eventService.getAllEvents(category, search);
        return ResponseEntity.ok(ApiResponse.success("Events fetched successfully", events));
    }


    /**
     * GET /api/events/{id}
     * Public endpoint to get event details.
     */
    @Operation(summary = "Get event by ID", description = "Retrieves complete event metadata by its ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success("Event fetched successfully", event));
    }


    /**
     * GET /api/events/{id}/seats
     * Public endpoint to get the real-time seat map and availability for an event.
     */
    @Operation(summary = "Get seat map", description = "Retrieves all seats and their real-time availability status (AVAILABLE, HELD, BOOKED) for an event.")
    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getEventSeats(@PathVariable Long id) {
        List<SeatResponse> seats = eventService.getEventSeats(id);
        return ResponseEntity.ok(ApiResponse.success("Seats fetched successfully", seats));
    }


    /**
     * PUT /api/events/{id}
     * Updates event details. Restricted to owning ORGANIZER or ADMIN.
     */
    @Operation(summary = "Update event", description = "Updates an event's details. Restricted to the owning organizer or an admin.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        EventResponse response = eventService.updateEvent(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", response));
    }

    
    /**
     * DELETE /api/events/{id}
     * Deletes an event. Restricted to owning ORGANIZER or ADMIN.
     */
    @Operation(summary = "Delete event", description = "Deletes an event. Restricted to the owning organizer or an admin.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        eventService.deleteEvent(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully", null));
    }
}
