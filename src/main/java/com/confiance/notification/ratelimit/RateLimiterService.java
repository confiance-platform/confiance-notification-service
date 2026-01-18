package com.confiance.notification.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitConfig config;

    // In-memory bucket cache for fast rate limiting
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String BLOCKED_IP_PREFIX = "blocked_ip:";
    private static final String FAILED_ATTEMPTS_PREFIX = "failed_attempts:";
    private static final String OTP_COOLDOWN_PREFIX = "otp_cooldown:";

    // ==================== OTP Rate Limiting ====================

    /**
     * Check if OTP can be sent (respects cooldown and rate limits)
     */
    public RateLimitResult checkOtpSendLimit(String identifier) {
        if (!config.isEnabled()) {
            return RateLimitResult.allowed();
        }

        String sanitizedId = sanitizeKey(identifier);

        // Check cooldown first
        String cooldownKey = OTP_COOLDOWN_PREFIX + sanitizedId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            return RateLimitResult.denied(
                    "Please wait " + ttl + " seconds before requesting another OTP",
                    ttl != null ? ttl.intValue() : config.getOtp().getCooldownSeconds()
            );
        }

        // Check per-minute limit
        String minuteKey = RATE_LIMIT_PREFIX + "otp:minute:" + sanitizedId;
        if (!checkAndIncrement(minuteKey, config.getOtp().getSendPerMinute(), 60)) {
            return RateLimitResult.denied("Too many OTP requests. Please try again in a minute.", 60);
        }

        // Check per-hour limit
        String hourKey = RATE_LIMIT_PREFIX + "otp:hour:" + sanitizedId;
        if (!checkAndIncrement(hourKey, config.getOtp().getSendPerHour(), 3600)) {
            return RateLimitResult.denied("Hourly OTP limit reached. Please try again later.", 3600);
        }

        // Check per-day limit
        String dayKey = RATE_LIMIT_PREFIX + "otp:day:" + sanitizedId;
        if (!checkAndIncrement(dayKey, config.getOtp().getSendPerDay(), 86400)) {
            return RateLimitResult.denied("Daily OTP limit reached. Please try again tomorrow.", 86400);
        }

        // Set cooldown
        redisTemplate.opsForValue().set(cooldownKey, "1", config.getOtp().getCooldownSeconds(), TimeUnit.SECONDS);

        return RateLimitResult.allowed();
    }

    /**
     * Check OTP verification attempts
     */
    public RateLimitResult checkOtpVerifyLimit(String identifier) {
        if (!config.isEnabled()) {
            return RateLimitResult.allowed();
        }

        String key = RATE_LIMIT_PREFIX + "otp:verify:" + sanitizeKey(identifier);
        int current = getCount(key);

        if (current >= config.getOtp().getVerifyAttemptsPerOtp()) {
            return RateLimitResult.denied("Maximum verification attempts exceeded. Please request a new OTP.", 0);
        }

        return RateLimitResult.allowed();
    }

    /**
     * Increment OTP verification attempt counter
     */
    public void incrementOtpVerifyAttempt(String identifier) {
        String key = RATE_LIMIT_PREFIX + "otp:verify:" + sanitizeKey(identifier);
        increment(key, 300); // 5 minutes TTL
    }

    /**
     * Reset OTP verification attempts (on successful verify or new OTP)
     */
    public void resetOtpVerifyAttempts(String identifier) {
        String key = RATE_LIMIT_PREFIX + "otp:verify:" + sanitizeKey(identifier);
        redisTemplate.delete(key);
    }

    // ==================== Payment Rate Limiting ====================

    /**
     * Check if payment order can be created
     */
    public RateLimitResult checkPaymentCreateLimit(Long userId) {
        if (!config.isEnabled()) {
            return RateLimitResult.allowed();
        }

        // Check per-minute limit
        String minuteKey = RATE_LIMIT_PREFIX + "payment:create:minute:" + userId;
        if (!checkAndIncrement(minuteKey, config.getPayment().getCreateOrderPerMinute(), 60)) {
            return RateLimitResult.denied("Too many payment requests. Please wait a minute.", 60);
        }

        // Check per-hour limit
        String hourKey = RATE_LIMIT_PREFIX + "payment:create:hour:" + userId;
        if (!checkAndIncrement(hourKey, config.getPayment().getCreateOrderPerHour(), 3600)) {
            return RateLimitResult.denied("Hourly payment limit reached. Please try again later.", 3600);
        }

        return RateLimitResult.allowed();
    }

    /**
     * Check payment verification limit
     */
    public RateLimitResult checkPaymentVerifyLimit(Long userId) {
        if (!config.isEnabled()) {
            return RateLimitResult.allowed();
        }

        String key = RATE_LIMIT_PREFIX + "payment:verify:minute:" + userId;
        if (!checkAndIncrement(key, config.getPayment().getVerifyPerMinute(), 60)) {
            return RateLimitResult.denied("Too many verification attempts. Please wait.", 60);
        }

        return RateLimitResult.allowed();
    }

    // ==================== File Upload Rate Limiting ====================

    /**
     * Check if file can be uploaded
     * @param identifier User ID or IP address as string
     */
    public RateLimitResult checkFileUploadLimit(String identifier) {
        if (!config.isEnabled()) {
            return RateLimitResult.allowed();
        }

        // Check per-minute limit
        String minuteKey = RATE_LIMIT_PREFIX + "file:upload:minute:" + identifier;
        if (!checkAndIncrement(minuteKey, config.getFile().getUploadPerMinute(), 60)) {
            return RateLimitResult.denied("Too many uploads. Please wait a minute.", 60);
        }

        // Check per-hour limit
        String hourKey = RATE_LIMIT_PREFIX + "file:upload:hour:" + identifier;
        if (!checkAndIncrement(hourKey, config.getFile().getUploadPerHour(), 3600)) {
            return RateLimitResult.denied("Hourly upload limit reached. Please try again later.", 3600);
        }

        return RateLimitResult.allowed();
    }

    // ==================== Email Rate Limiting ====================

    /**
     * Check if email can be sent
     * @param identifier User ID or email address as string
     */
    public RateLimitResult checkEmailSendLimit(String identifier) {
        if (!config.isEnabled()) {
            return RateLimitResult.allowed();
        }

        // Check per-minute limit
        String minuteKey = RATE_LIMIT_PREFIX + "email:send:minute:" + identifier;
        if (!checkAndIncrement(minuteKey, config.getEmail().getSendPerMinute(), 60)) {
            return RateLimitResult.denied("Too many emails. Please wait a minute.", 60);
        }

        // Check per-hour limit
        String hourKey = RATE_LIMIT_PREFIX + "email:send:hour:" + identifier;
        if (!checkAndIncrement(hourKey, config.getEmail().getSendPerHour(), 3600)) {
            return RateLimitResult.denied("Hourly email limit reached. Please try again later.", 3600);
        }

        return RateLimitResult.allowed();
    }

    // ==================== API Rate Limiting (by IP) ====================

    /**
     * Check global API rate limit by IP
     */
    public RateLimitResult checkApiRateLimit(String ipAddress) {
        if (!config.isEnabled()) {
            return RateLimitResult.allowed();
        }

        // Check if IP is blocked
        if (isIpBlocked(ipAddress)) {
            Long ttl = redisTemplate.getExpire(BLOCKED_IP_PREFIX + ipAddress, TimeUnit.SECONDS);
            return RateLimitResult.denied(
                    "Your IP has been temporarily blocked due to suspicious activity.",
                    ttl != null ? ttl.intValue() : config.getIpBlocking().getBlockDurationMinutes() * 60
            );
        }

        // Use bucket4j for fast in-memory rate limiting
        String bucketKey = "api:" + ipAddress;
        Bucket bucket = bucketCache.computeIfAbsent(bucketKey, k -> createApiBucket());

        if (!bucket.tryConsume(1)) {
            recordFailedAttempt(ipAddress);
            return RateLimitResult.denied("Too many requests. Please slow down.", 1);
        }

        return RateLimitResult.allowed();
    }

    private Bucket createApiBucket() {
        Bandwidth perSecond = Bandwidth.classic(
                config.getApi().getRequestsPerSecond(),
                Refill.greedy(config.getApi().getRequestsPerSecond(), Duration.ofSeconds(1))
        );
        Bandwidth perMinute = Bandwidth.classic(
                config.getApi().getRequestsPerMinute(),
                Refill.greedy(config.getApi().getRequestsPerMinute(), Duration.ofMinutes(1))
        );
        return Bucket.builder()
                .addLimit(perSecond)
                .addLimit(perMinute)
                .build();
    }

    // ==================== IP Blocking ====================

    /**
     * Check if IP is blocked
     */
    public boolean isIpBlocked(String ipAddress) {
        if (!config.getIpBlocking().isEnabled()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCKED_IP_PREFIX + ipAddress));
    }

    /**
     * Block an IP address
     */
    public void blockIp(String ipAddress, String reason) {
        if (!config.getIpBlocking().isEnabled()) {
            return;
        }
        String key = BLOCKED_IP_PREFIX + ipAddress;
        redisTemplate.opsForValue().set(key, reason,
                config.getIpBlocking().getBlockDurationMinutes(), TimeUnit.MINUTES);
        log.warn("IP blocked: {} - Reason: {}", ipAddress, reason);
    }

    /**
     * Unblock an IP address
     */
    public void unblockIp(String ipAddress) {
        redisTemplate.delete(BLOCKED_IP_PREFIX + ipAddress);
        redisTemplate.delete(FAILED_ATTEMPTS_PREFIX + ipAddress);
        log.info("IP unblocked: {}", ipAddress);
    }

    /**
     * Record failed attempt (for suspicious activity detection)
     */
    public void recordFailedAttempt(String ipAddress) {
        if (!config.getIpBlocking().isEnabled()) {
            return;
        }

        String key = FAILED_ATTEMPTS_PREFIX + ipAddress;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, config.getIpBlocking().getFailedAttemptWindowMinutes(), TimeUnit.MINUTES);
        }

        if (count != null && count >= config.getIpBlocking().getMaxFailedAttempts()) {
            blockIp(ipAddress, "Too many failed attempts");
        }
    }

    /**
     * Clear failed attempts (on successful action)
     */
    public void clearFailedAttempts(String ipAddress) {
        redisTemplate.delete(FAILED_ATTEMPTS_PREFIX + ipAddress);
    }

    // ==================== Helper Methods ====================

    private boolean checkAndIncrement(String key, int limit, int ttlSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return count != null && count <= limit;
    }

    private int getCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    private void increment(String key, int ttlSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    private String sanitizeKey(String input) {
        return input.replaceAll("[^a-zA-Z0-9+@._-]", "_");
    }

    /**
     * Get remaining rate limit info
     */
    public RateLimitInfo getRateLimitInfo(String type, String identifier) {
        String minuteKey = RATE_LIMIT_PREFIX + type + ":minute:" + sanitizeKey(identifier);
        String hourKey = RATE_LIMIT_PREFIX + type + ":hour:" + sanitizeKey(identifier);

        int minuteCount = getCount(minuteKey);
        int hourCount = getCount(hourKey);
        Long minuteTtl = redisTemplate.getExpire(minuteKey, TimeUnit.SECONDS);
        Long hourTtl = redisTemplate.getExpire(hourKey, TimeUnit.SECONDS);

        return RateLimitInfo.builder()
                .minuteUsed(minuteCount)
                .hourUsed(hourCount)
                .minuteResetIn(minuteTtl != null ? minuteTtl.intValue() : 0)
                .hourResetIn(hourTtl != null ? hourTtl.intValue() : 0)
                .build();
    }
}
