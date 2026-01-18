package com.confiance.notification.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private boolean enabled = true;

    // OTP Rate Limits
    private OtpLimits otp = new OtpLimits();

    // Payment Rate Limits
    private PaymentLimits payment = new PaymentLimits();

    // File Upload Rate Limits
    private FileLimits file = new FileLimits();

    // Email Rate Limits
    private EmailLimits email = new EmailLimits();

    // Global API Rate Limits
    private ApiLimits api = new ApiLimits();

    // IP Blocking
    private IpBlocking ipBlocking = new IpBlocking();

    @Data
    public static class OtpLimits {
        private int sendPerMinute = 2;           // Max OTP sends per minute per identifier
        private int sendPerHour = 10;            // Max OTP sends per hour per identifier
        private int sendPerDay = 20;             // Max OTP sends per day per identifier
        private int verifyAttemptsPerOtp = 3;    // Max verification attempts per OTP
        private int cooldownSeconds = 60;        // Cooldown between OTP sends
    }

    @Data
    public static class PaymentLimits {
        private int createOrderPerMinute = 5;    // Max order creations per minute per user
        private int createOrderPerHour = 30;     // Max order creations per hour per user
        private int verifyPerMinute = 10;        // Max verify attempts per minute per user
    }

    @Data
    public static class FileLimits {
        private int uploadPerMinute = 10;        // Max uploads per minute per user
        private int uploadPerHour = 100;         // Max uploads per hour per user
        private long maxFileSizeBytes = 10485760; // 10MB
    }

    @Data
    public static class EmailLimits {
        private int sendPerMinute = 5;           // Max emails per minute per user
        private int sendPerHour = 50;            // Max emails per hour per user
    }

    @Data
    public static class ApiLimits {
        private int requestsPerSecond = 10;      // Max API requests per second per IP
        private int requestsPerMinute = 100;     // Max API requests per minute per IP
    }

    @Data
    public static class IpBlocking {
        private boolean enabled = true;
        private int maxFailedAttempts = 10;      // Max failed attempts before blocking
        private int blockDurationMinutes = 30;   // Block duration in minutes
        private int failedAttemptWindowMinutes = 5; // Window to count failed attempts
    }
}
