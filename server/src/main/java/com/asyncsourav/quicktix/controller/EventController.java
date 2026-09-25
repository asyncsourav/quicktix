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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * POST /api/events
     * Creates a new event and auto-populates its seat layout. Restricted to ORGANIZER and ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        EventResponse response = eventService.createEvent(request, userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created and seats generated successfully", response));
    }

    /**
     * GET /api/events
     * Public endpoint to list and filter events by category or search keyword.
     */
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
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success("Event fetched successfully", event));
    }

    /**
     * GET /api/events/{id}/seats
     * Public endpoint to get the real-time seat map and availability for an event.
     */
    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getEventSeats(@PathVariable Long id) {
        List<SeatResponse> seats = eventService.getEventSeats(id);
        return ResponseEntity.ok(ApiResponse.success("Seats fetched successfully", seats));
    }

    /**
     * PUT /api/events/{id}
     * Updates event details. Restricted to owning ORGANIZER or ADMIN.
     */
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
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        eventService.deleteEvent(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully", null));
    }
}
