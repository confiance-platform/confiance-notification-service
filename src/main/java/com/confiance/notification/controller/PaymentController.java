package com.confiance.notification.controller;

import com.confiance.common.dto.ApiResponse;
import com.confiance.common.dto.PageResponse;
import com.confiance.notification.dto.PaymentOrderRequest;
import com.confiance.notification.dto.PaymentOrderResponse;
import com.confiance.notification.dto.PaymentResponse;
import com.confiance.notification.dto.PaymentVerifyRequest;
import com.confiance.notification.ratelimit.RateLimitExceededException;
import com.confiance.notification.ratelimit.RateLimitResult;
import com.confiance.notification.ratelimit.RateLimiterService;
import com.confiance.notification.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Razorpay payment gateway APIs")
public class PaymentController {

    private final PaymentService paymentService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/create-order")
    @Operation(summary = "Create Payment Order", description = "Create a new Razorpay payment order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @Valid @RequestBody PaymentOrderRequest request,
            HttpServletRequest httpRequest) {

        // Check rate limit for payment order creation
        RateLimitResult rateLimitResult = rateLimiterService.checkPaymentCreateLimit(request.getUserId());
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        PaymentOrderResponse response = paymentService.createOrder(request);
        rateLimiterService.clearFailedAttempts(getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Payment order created successfully", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Payment", description = "Verify payment signature after successful payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request,
            HttpServletRequest httpRequest) {

        // Extract userId from the order (we'll get it from the payment record)
        PaymentResponse existingPayment = paymentService.getPaymentByRazorpayOrderId(request.getRazorpayOrderId());

        // Check rate limit for payment verification
        RateLimitResult rateLimitResult = rateLimiterService.checkPaymentVerifyLimit(existingPayment.getUserId());
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        PaymentResponse response = paymentService.verifyPayment(request);
        rateLimiterService.clearFailedAttempts(getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get Payment by Order ID", description = "Get payment details by internal order ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(@PathVariable String orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/razorpay-order/{razorpayOrderId}")
    @Operation(summary = "Get Payment by Razorpay Order ID", description = "Get payment details by Razorpay order ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByRazorpayOrderId(@PathVariable String razorpayOrderId) {
        PaymentResponse response = paymentService.getPaymentByRazorpayOrderId(razorpayOrderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Payments", description = "Get all payments for a user")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getUserPayments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<PaymentResponse> response = paymentService.getUserPayments(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refund/{razorpayPaymentId}")
    @Operation(summary = "Refund Payment", description = "Initiate a refund for a payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable String razorpayPaymentId,
            @RequestParam BigDecimal amount) {
        PaymentResponse response = paymentService.refundPayment(razorpayPaymentId, amount);
        return ResponseEntity.ok(ApiResponse.success("Refund initiated successfully", response));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay Webhook", description = "Handle Razorpay webhook events")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        log.info("Received Razorpay webhook");
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully", null));
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
