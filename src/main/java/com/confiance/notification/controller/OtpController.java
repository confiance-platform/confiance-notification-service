package com.confiance.notification.controller;

import com.confiance.common.dto.ApiResponse;
import com.confiance.notification.dto.OtpRequest;
import com.confiance.notification.dto.OtpResponse;
import com.confiance.notification.dto.OtpVerifyRequest;
import com.confiance.notification.ratelimit.RateLimitExceededException;
import com.confiance.notification.ratelimit.RateLimitResult;
import com.confiance.notification.ratelimit.RateLimiterService;
import com.confiance.notification.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
@Tag(name = "OTP", description = "OTP generation and verification APIs")
public class OtpController {

    private final OtpService otpService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/send")
    @Operation(summary = "Send OTP", description = "Send OTP to phone number or email")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(
            @Valid @RequestBody OtpRequest request,
            HttpServletRequest httpRequest) {

        // Check rate limit for OTP sending
        RateLimitResult rateLimitResult = rateLimiterService.checkOtpSendLimit(request.getIdentifier());
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        OtpResponse response = otpService.sendOtp(request);

        // Clear failed attempts on successful send
        rateLimiterService.clearFailedAttempts(getClientIp(httpRequest));

        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP", description = "Verify OTP code")
    public ResponseEntity<ApiResponse<OtpResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            HttpServletRequest httpRequest) {

        // Check verification attempt limit
        RateLimitResult rateLimitResult = rateLimiterService.checkOtpVerifyLimit(request.getIdentifier());
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        // Increment verification attempt
        rateLimiterService.incrementOtpVerifyAttempt(request.getIdentifier());

        OtpResponse response = otpService.verifyOtp(request);

        if ("VERIFIED".equals(response.getStatus())) {
            // Reset verification attempts on success
            rateLimiterService.resetOtpVerifyAttempts(request.getIdentifier());
            rateLimiterService.clearFailedAttempts(getClientIp(httpRequest));
            return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response));
        } else {
            // Record failed attempt for IP blocking
            rateLimiterService.recordFailedAttempt(getClientIp(httpRequest));
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage()));
        }
    }

    @PostMapping("/resend")
    @Operation(summary = "Resend OTP", description = "Resend OTP to the same identifier")
    public ResponseEntity<ApiResponse<OtpResponse>> resendOtp(
            @Valid @RequestBody OtpRequest request,
            HttpServletRequest httpRequest) {

        // Check rate limit for OTP sending (same as send)
        RateLimitResult rateLimitResult = rateLimiterService.checkOtpSendLimit(request.getIdentifier());
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        // Reset previous verification attempts
        rateLimiterService.resetOtpVerifyAttempts(request.getIdentifier());

        OtpResponse response = otpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully", response));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
