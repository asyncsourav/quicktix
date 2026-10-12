

package com.asyncsourav.quicktix.controller;



import com.asyncsourav.quicktix.dto.common.ApiResponse;
import com.asyncsourav.quicktix.dto.response.AdminStatsResponse;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;




@Tag(name = "Admin", description = "Endpoints for platform metrics, user management, and booking oversight")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {


    private final AdminService adminService;


    /**
     * GET /api/admin/stats
     * Aggregates platform-wide metrics (revenue, occupancy, user counts).
     */
    @Operation(summary = "Get platform statistics", description = "Aggregates revenue, active bookings, venue and user totals. Restricted to ADMIN role.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminService.getPlatformStats();
        return ResponseEntity.ok(ApiResponse.success("Platform statistics retrieved", stats));
    }


    /**
     * GET /api/admin/users
     * Returns a list of all registered platform users.
     */
    @Operation(summary = "Get all users", description = "Returns all registered user accounts on the platform. Restricted to ADMIN role.")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("All users retrieved", users));
    }


    /**
     * GET /api/admin/bookings
     * Returns platform-wide booking records.
     */
    @Operation(summary = "Get all platform bookings", description = "Returns all confirmed and cancelled bookings across all events. Restricted to ADMIN role.")
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        List<BookingResponse> bookings = adminService.getAllBookings();
        return ResponseEntity.ok(ApiResponse.success("All bookings retrieved", bookings));
    }
}
