
package com.asyncsourav.quicktix.exception;


    
public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(String message) {
        super(message);
    }

    public SeatUnavailableException(Long seatId, String seatLabel, String reason) {
        super(String.format("Seat '%s' (ID: %d) is unavailable: %s", seatLabel, seatId, reason));
    }
}
