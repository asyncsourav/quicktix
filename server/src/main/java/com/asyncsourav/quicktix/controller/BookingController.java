

package com.asyncsourav.quicktix.controller;


import com.asyncsourav.quicktix.dto.common.ApiResponse;
import com.asyncsourav.quicktix.dto.request.BookingRequest;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.service.BookingService;
import com.asyncsourav.quicktix.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
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



@Tag(name = "Bookings", description = "Endpoints for seat reservation and booking management with concurrency safety")
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize ("isAuthenticated()")
@RequiredArgsConstructor
public class BookingController {


    private final BookingService bookingService;
    private final RateLimiterService rateLimiterService;


    /**
     * POST /api/bookings
     * Creates a new booking. Protected with Optimistic Locking and Distributed Rate Limiting.
     */
    @Operation(summary = "Create booking", description = "Reserves selected seats and confirms booking. Protected against race conditions via JPA optimistic locking and protected against bots via Bucket4j Redis rate limiting.")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest servletRequest
        ) {

        String clientIp = extractClientIp(servletRequest);
        rateLimiterService.checkBookingRateLimit(userDetails.getUsername(), clientIp);

        BookingResponse response = bookingService.createBooking(request, userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking confirmed successfully", response));
    }



    /**
     * When the application runs in Docker or behind a reverse proxy (like Nginx, AWS ALB, or Cloudflare), 
     * calling request.getRemoteAddr() returns the internal IP of the proxy or Docker gateway , 
     * not the user's actual IP
     * 
     * Method for extracting the actual client IP 
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }


    
    /**
     * GET /api/bookings/me
     * Returns all bookings belonging to the currently authenticated user.
     */
    @Operation(summary = "Get user bookings", description = "Returns all confirmed/cancelled bookings belonging to the currently authenticated user.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<BookingResponse> bookings = bookingService.getMyBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully", bookings));
    }

    /**
     * GET /api/bookings/{id}
     * Returns details of a specific booking.
     */
    @Operation(summary = "Get booking by ID", description = "Returns details and reserved seat items of a specific booking.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse response = bookingService.getBookingById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Booking fetched successfully", response));
    }

    /**
     * DELETE /api/bookings/{id}
     * Cancels a booking and releases all associated seats back to AVAILABLE.
     */
    @Operation(summary = "Cancel booking", description = "Cancels a booking and immediately releases all associated seats back to AVAILABLE.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse response = bookingService.cancelBooking(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }
}
