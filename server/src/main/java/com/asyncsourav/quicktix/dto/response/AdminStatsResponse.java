

package com.asyncsourav.quicktix.dto.response;



import java.math.BigDecimal;

/**
 * Platform-wide statistics overview for the Admin Dashboard.
 */

public record AdminStatsResponse(
        long totalUsers,
        long totalVenues,
        long totalEvents,
        long totalBookings,
        BigDecimal totalRevenue,
        long confirmedBookings,
        long cancelledBookings
) {}
