

package com.asyncsourav.quicktix.entity;

/**
 * PENDING: Booking initiated, awaiting payment confirmation or seat hold release.
 * CONFIRMED: Payment verified, tickets generated and issued.
 * CANCELLED: Order cancelled by user/organizer or timed out; seats returned to AVAILABLE.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
