package com.confiance.notification.ratelimit;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(RateLimitResult result) {
        super(result.getMessage());
        this.retryAfterSeconds = result.getRetryAfterSeconds();
    }
}
