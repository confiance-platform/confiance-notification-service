package com.confiance.notification.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {

    private boolean allowed;
    private String message;
    private int retryAfterSeconds;

    public static RateLimitResult allowed() {
        return RateLimitResult.builder()
                .allowed(true)
                .build();
    }

    public static RateLimitResult denied(String message, int retryAfterSeconds) {
        return RateLimitResult.builder()
                .allowed(false)
                .message(message)
                .retryAfterSeconds(retryAfterSeconds)
                .build();
    }
}
