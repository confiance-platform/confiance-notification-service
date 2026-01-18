package com.confiance.notification.service;

import com.confiance.common.exception.BadRequestException;
import com.confiance.notification.dto.OtpRequest;
import com.confiance.notification.dto.OtpResponse;
import com.confiance.notification.dto.OtpVerifyRequest;
import com.confiance.notification.entity.OtpLog;
import com.confiance.notification.enums.OtpProvider;
import com.confiance.notification.enums.OtpPurpose;
import com.confiance.notification.repository.OtpLogRepository;
import com.confiance.notification.service.otp.OtpSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OtpService {

    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String OTP_ATTEMPTS_KEY_PREFIX = "otp_attempts:";

    private final Map<OtpProvider, OtpSender> otpSenders;
    private final OtpLogRepository otpLogRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${otp.provider:twilio}")
    private String defaultProvider;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    @Autowired
    public OtpService(List<OtpSender> senders, OtpLogRepository otpLogRepository,
                      RedisTemplate<String, String> redisTemplate) {
        this.otpSenders = senders.stream()
                .collect(Collectors.toMap(OtpSender::getProvider, sender -> sender));
        this.otpLogRepository = otpLogRepository;
        this.redisTemplate = redisTemplate;
        log.info("OTP service initialized with providers: {}", otpSenders.keySet());
    }

    public OtpResponse sendOtp(OtpRequest request) {
        String identifier = request.getIdentifier();
        OtpPurpose purpose = request.getPurpose();

        long recentRequests = otpLogRepository.countByIdentifierAndPurposeAndCreatedAtAfter(
                identifier, purpose, LocalDateTime.now().minusMinutes(1));
        if (recentRequests >= 3) {
            log.warn("Rate limit exceeded for OTP requests: {}", identifier);
            throw new BadRequestException("Too many OTP requests. Please wait before retrying.");
        }

        OtpProvider provider = OtpProvider.valueOf(defaultProvider.toUpperCase().replace("-", "_"));
        OtpSender sender = otpSenders.get(provider);

        if (sender == null || !sender.isConfigured()) {
            log.warn("OTP provider {} not available, using fallback", provider);
            return sendOtpFallback(request);
        }

        OtpResponse response = sender.sendOtp(identifier, purpose, request.getCountryCode());

        saveOtpLog(request, response, provider);

        return response;
    }

    public OtpResponse verifyOtp(OtpVerifyRequest request) {
        String identifier = request.getIdentifier();
        OtpPurpose purpose = request.getPurpose();
        String otp = request.getOtp();

        String attemptsKey = OTP_ATTEMPTS_KEY_PREFIX + identifier + ":" + purpose;
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= maxAttempts) {
            log.warn("Max OTP attempts exceeded for: {}", identifier);
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        OtpProvider provider = OtpProvider.valueOf(defaultProvider.toUpperCase().replace("-", "_"));
        OtpSender sender = otpSenders.get(provider);

        OtpResponse response;

        if (sender != null && sender.isConfigured()) {
            response = sender.verifyOtp(identifier, otp, purpose);
        } else {
            response = verifyOtpFallback(identifier, otp, purpose);
        }

        if ("VERIFIED".equals(response.getStatus())) {
            redisTemplate.delete(attemptsKey);
            redisTemplate.delete(OTP_KEY_PREFIX + identifier + ":" + purpose);

            Optional<OtpLog> logOpt = otpLogRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(identifier, purpose);
            logOpt.ifPresent(otpLog -> {
                otpLog.setStatus("VERIFIED");
                otpLog.setVerifiedAt(LocalDateTime.now());
                otpLogRepository.save(otpLog);
            });
        } else {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(expiryMinutes));
            response.setRemainingAttempts(maxAttempts - attempts - 1);
        }

        return response;
    }

    private OtpResponse sendOtpFallback(OtpRequest request) {
        String otp = generateOtp();
        String key = OTP_KEY_PREFIX + request.getIdentifier() + ":" + request.getPurpose();

        redisTemplate.opsForValue().set(key, otp, expiryMinutes, TimeUnit.MINUTES);

        log.info("OTP {} generated for {} (fallback mode - NOT SENT via SMS)", otp, request.getIdentifier());

        OtpLog otpLog = OtpLog.builder()
                .identifier(request.getIdentifier())
                .purpose(request.getPurpose())
                .provider(null)
                .status("GENERATED")
                .userId(request.getUserId())
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();
        otpLogRepository.save(otpLog);

        return OtpResponse.builder()
                .identifier(request.getIdentifier())
                .status("SENT")
                .message("OTP generated (configure SMS provider for actual delivery)")
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();
    }

    private OtpResponse verifyOtpFallback(String identifier, String otp, OtpPurpose purpose) {
        String key = OTP_KEY_PREFIX + identifier + ":" + purpose;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            log.info("OTP verified successfully for {} (fallback mode)", identifier);
            return OtpResponse.builder()
                    .identifier(identifier)
                    .status("VERIFIED")
                    .message("OTP verified successfully")
                    .build();
        } else {
            log.warn("Invalid OTP for {} (fallback mode)", identifier);
            return OtpResponse.builder()
                    .identifier(identifier)
                    .status("FAILED")
                    .message("Invalid OTP")
                    .build();
        }
    }

    private String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void saveOtpLog(OtpRequest request, OtpResponse response, OtpProvider provider) {
        try {
            OtpLog otpLog = OtpLog.builder()
                    .identifier(request.getIdentifier())
                    .purpose(request.getPurpose())
                    .provider(provider)
                    .status(response.getStatus())
                    .userId(request.getUserId())
                    .expiresAt(response.getExpiresAt())
                    .build();
            otpLogRepository.save(otpLog);
        } catch (Exception e) {
            log.error("Error saving OTP log: {}", e.getMessage());
        }
    }

    public boolean isProviderConfigured(OtpProvider provider) {
        OtpSender sender = otpSenders.get(provider);
        return sender != null && sender.isConfigured();
    }
}
