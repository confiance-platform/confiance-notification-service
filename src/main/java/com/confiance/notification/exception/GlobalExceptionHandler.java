package com.confiance.notification.exception;

import com.confiance.common.dto.ApiResponse;
import com.confiance.notification.ratelimit.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimitExceededException(
            RateLimitExceededException ex,
            HttpServletResponse response) {

        log.warn("Rate limit exceeded: {}", ex.getMessage());

        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        response.setHeader("X-RateLimit-Remaining", "0");

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(apiResponse);
    }
}
