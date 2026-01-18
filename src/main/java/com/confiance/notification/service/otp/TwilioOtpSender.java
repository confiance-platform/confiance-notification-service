package com.confiance.notification.service.otp;

import com.confiance.notification.dto.OtpResponse;
import com.confiance.notification.enums.OtpProvider;
import com.confiance.notification.enums.OtpPurpose;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@Slf4j
@ConditionalOnProperty(name = "otp.provider", havingValue = "twilio", matchIfMissing = true)
public class TwilioOtpSender implements OtpSender {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.verify-service-sid:}")
    private String verifyServiceSid;

    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio OTP service initialized");
        } else {
            log.warn("Twilio OTP service not configured - missing credentials");
        }
    }

    @Override
    public OtpResponse sendOtp(String identifier, OtpPurpose purpose, String countryCode) {
        if (!isConfigured()) {
            log.error("Twilio not configured");
            return OtpResponse.builder()
                    .identifier(identifier)
                    .status("FAILED")
                    .message("OTP service not configured")
                    .build();
        }

        try {
            String formattedNumber = formatPhoneNumber(identifier, countryCode);

            Verification verification = Verification.creator(
                    verifyServiceSid,
                    formattedNumber,
                    "sms"
            ).create();

            log.info("OTP sent successfully to {} via Twilio. Status: {}", identifier, verification.getStatus());

            return OtpResponse.builder()
                    .identifier(identifier)
                    .status("SENT")
                    .message("OTP sent successfully")
                    .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                    .build();

        } catch (Exception e) {
            log.error("Failed to send OTP via Twilio: {}", e.getMessage());
            return OtpResponse.builder()
                    .identifier(identifier)
                    .status("FAILED")
                    .message("Failed to send OTP: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OtpResponse verifyOtp(String identifier, String otp, OtpPurpose purpose) {
        if (!isConfigured()) {
            log.error("Twilio not configured");
            return OtpResponse.builder()
                    .identifier(identifier)
                    .status("FAILED")
                    .message("OTP service not configured")
                    .build();
        }

        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(verifyServiceSid)
                    .setTo(identifier)
                    .setCode(otp)
                    .create();

            if ("approved".equals(verificationCheck.getStatus())) {
                log.info("OTP verified successfully for {}", identifier);
                return OtpResponse.builder()
                        .identifier(identifier)
                        .status("VERIFIED")
                        .message("OTP verified successfully")
                        .build();
            } else {
                log.warn("OTP verification failed for {}. Status: {}", identifier, verificationCheck.getStatus());
                return OtpResponse.builder()
                        .identifier(identifier)
                        .status("FAILED")
                        .message("Invalid OTP")
                        .build();
            }

        } catch (Exception e) {
            log.error("Failed to verify OTP via Twilio: {}", e.getMessage());
            return OtpResponse.builder()
                    .identifier(identifier)
                    .status("FAILED")
                    .message("Failed to verify OTP: " + e.getMessage())
                    .build();
        }
    }

    private String formatPhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        }
        if (countryCode != null && !countryCode.isEmpty()) {
            String code = countryCode.startsWith("+") ? countryCode : "+" + countryCode;
            return code + phoneNumber;
        }
        return "+91" + phoneNumber;
    }

    @Override
    public OtpProvider getProvider() {
        return OtpProvider.TWILIO;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(accountSid) &&
                StringUtils.hasText(authToken) &&
                StringUtils.hasText(verifyServiceSid);
    }
}
