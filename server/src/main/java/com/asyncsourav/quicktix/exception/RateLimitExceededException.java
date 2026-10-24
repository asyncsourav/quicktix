package com.asyncsourav.quicktix.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Too many booking requests. Please wait " + retryAfterSeconds + " second"
                + (retryAfterSeconds == 1 ? "" : "s") + " before trying again.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
