

package com.asyncsourav.quicktix.dto.response;



import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Platform-wide statistics overview for the Admin Dashboard.
 */

@Schema(description = "Platform-wide aggregated metrics overview")
public record AdminStatsResponse(
        @Schema(description = "Total registered user accounts", example = "3")
        long totalUsers,

        @Schema(description = "Total registered venues", example = "3")
        long totalVenues,

        @Schema(description = "Total published events", example = "4")
        long totalEvents,

        @Schema(description = "Total booking records", example = "1")
        long totalBookings,

        @Schema(description = "Gross revenue collected ($)", example = "199.98")
        BigDecimal totalRevenue,

        @Schema(description = "Total confirmed bookings", example = "1")
        long confirmedBookings,

        @Schema(description = "Total cancelled bookings", example = "0")
        long cancelledBookings
) {}
