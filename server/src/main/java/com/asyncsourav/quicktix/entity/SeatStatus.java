
package com.asyncsourav.quicktix.entity;

/**
AVAILABLE: Seat is open for any user to select and hold/book.
HELD: Seat is temporarily held during the checkout countdown timer.
BOOKED: Seat is confirmed and purchased.
*/

public enum SeatStatus {
    AVAILABLE,
    HELD,
    BOOKED
}
