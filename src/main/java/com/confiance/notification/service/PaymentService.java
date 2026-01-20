package com.confiance.notification.service;

import com.confiance.common.dto.PageResponse;
import com.confiance.common.exception.BadRequestException;
import com.confiance.common.exception.InternalServerException;
import com.confiance.common.exception.ResourceNotFoundException;
import com.confiance.notification.dto.PaymentOrderRequest;
import com.confiance.notification.dto.PaymentOrderResponse;
import com.confiance.notification.dto.PaymentResponse;
import com.confiance.notification.dto.PaymentVerifyRequest;
import com.confiance.notification.entity.Payment;
import com.confiance.notification.enums.PaymentStatus;
import com.confiance.notification.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final FeatureService featureService;

    @Autowired
    public PaymentService(@Autowired(required = false) RazorpayClient razorpayClient,
                          PaymentRepository paymentRepository,
                          ObjectMapper objectMapper,
                          EmailService emailService,
                          FeatureService featureService) {
        this.razorpayClient = razorpayClient;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.emailService = emailService;
        this.featureService = featureService;
    }

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;

    @Transactional
    public PaymentOrderResponse createOrder(PaymentOrderRequest request) {
        // Check if PAYMENT feature is enabled
        if (!featureService.isEnabled(FeatureService.FEATURE_PAYMENT)) {
            log.warn("Payment feature is DISABLED - cannot create order");
            throw new BadRequestException("Payment service is temporarily disabled. Please try again later.");
        }

        if (razorpayClient == null) {
            throw new InternalServerException("Payment gateway is not configured");
        }

        try {
            String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String currency = request.getCurrency() != null ? request.getCurrency() : defaultCurrency;

            long amountInPaise = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", request.getReceipt() != null ? request.getReceipt() : orderId);

            if (request.getNotes() != null) {
                JSONObject notes = new JSONObject(request.getNotes());
                orderRequest.put("notes", notes);
            }

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            String razorpayOrderId = razorpayOrder.get("id");
            long amountFromRazorpay = ((Number) razorpayOrder.get("amount")).longValue();
            long amountPaidFromRazorpay = ((Number) razorpayOrder.get("amount_paid")).longValue();
            long amountDueFromRazorpay = ((Number) razorpayOrder.get("amount_due")).longValue();

            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .razorpayOrderId(razorpayOrderId)
                    .amount(request.getAmount())
                    .amountPaid(BigDecimal.valueOf(amountPaidFromRazorpay).divide(BigDecimal.valueOf(100)))
                    .amountDue(BigDecimal.valueOf(amountDueFromRazorpay).divide(BigDecimal.valueOf(100)))
                    .currency(currency)
                    .receipt(request.getReceipt())
                    .status(PaymentStatus.CREATED)
                    .userId(request.getUserId())
                    .description(request.getDescription())
                    .notes(request.getNotes() != null ? objectMapper.writeValueAsString(request.getNotes()) : null)
                    .build();

            if (request.getCustomer() != null) {
                payment.setCustomerName(request.getCustomer().getName());
                payment.setCustomerEmail(request.getCustomer().getEmail());
                payment.setCustomerContact(request.getCustomer().getContact());
            }

            Payment savedPayment = paymentRepository.save(payment);

            log.info("Razorpay order created successfully: {}", razorpayOrderId);

            return PaymentOrderResponse.builder()
                    .orderId(savedPayment.getOrderId())
                    .razorpayOrderId(razorpayOrderId)
                    .amount(savedPayment.getAmount())
                    .amountPaid(savedPayment.getAmountPaid())
                    .amountDue(savedPayment.getAmountDue())
                    .currency(savedPayment.getCurrency())
                    .receipt(savedPayment.getReceipt())
                    .status(savedPayment.getStatus())
                    .userId(savedPayment.getUserId())
                    .razorpayKeyId(razorpayKeyId)
                    .notes(request.getNotes())
                    .createdAt(savedPayment.getCreatedAt())
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new InternalServerException("Failed to create payment order: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating payment order: {}", e.getMessage());
            throw new InternalServerException("Failed to create payment order");
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "razorpayOrderId", request.getRazorpayOrderId()));

        try {
            String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, webhookSecret);

            if (isValid || verifySignatureManually(payload, request.getRazorpaySignature())) {
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
                payment.setRazorpaySignature(request.getRazorpaySignature());
                payment.setStatus(PaymentStatus.CAPTURED);
                payment.setAmountPaid(payment.getAmount());
                payment.setAmountDue(BigDecimal.ZERO);
                payment.setPaidAt(LocalDateTime.now());

                Payment savedPayment = paymentRepository.save(payment);

                log.info("Payment verified successfully: {}", request.getRazorpayPaymentId());

                return toPaymentResponse(savedPayment);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setErrorDescription("Signature verification failed");
                paymentRepository.save(payment);

                throw new BadRequestException("Payment signature verification failed");
            }
        } catch (RazorpayException e) {
            log.error("Failed to verify payment: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorDescription(e.getMessage());
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification failed: " + e.getMessage());
        }
    }

    private String getKeySecret() {
        return webhookSecret;
    }

    private boolean verifySignatureManually(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());
            String expectedSignature = HexFormat.of().formatHex(hash);
            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying signature: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature");
            throw new BadRequestException("Invalid webhook signature");
        }

        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");

            log.info("Processing Razorpay webhook event: {}", eventType);

            JSONObject payloadData = event.getJSONObject("payload");
            JSONObject paymentEntity = payloadData.getJSONObject("payment").getJSONObject("entity");

            String razorpayOrderId = paymentEntity.optString("order_id");
            String razorpayPaymentId = paymentEntity.getString("id");
            String status = paymentEntity.getString("status");

            if (!StringUtils.hasText(razorpayOrderId)) {
                log.warn("No order_id in webhook payload");
                return;
            }

            Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
            if (payment == null) {
                log.warn("Payment not found for order: {}", razorpayOrderId);
                return;
            }

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setWebhookPayload(payload);

            switch (eventType) {
                case "payment.authorized":
                    payment.setStatus(PaymentStatus.AUTHORIZED);
                    break;
                case "payment.captured":
                    payment.setStatus(PaymentStatus.CAPTURED);
                    payment.setAmountPaid(payment.getAmount());
                    payment.setAmountDue(BigDecimal.ZERO);
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setMethod(paymentEntity.optString("method"));
                    break;
                case "payment.failed":
                    payment.setStatus(PaymentStatus.FAILED);
                    JSONObject errorObj = paymentEntity.optJSONObject("error_description");
                    if (errorObj != null) {
                        payment.setErrorCode(paymentEntity.optString("error_code"));
                        payment.setErrorDescription(paymentEntity.optString("error_description"));
                    }
                    break;
                case "refund.created":
                    payment.setStatus(PaymentStatus.REFUNDED);
                    break;
            }

            paymentRepository.save(payment);
            log.info("Webhook processed successfully for payment: {}", razorpayPaymentId);

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            throw new InternalServerException("Failed to process webhook");
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        if (!StringUtils.hasText(webhookSecret)) {
            log.warn("Webhook secret not configured");
            return true;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());
            String expectedSignature = HexFormat.of().formatHex(hash);
            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature: {}", e.getMessage());
            return false;
        }
    }

    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return toPaymentResponse(payment);
    }

    public PaymentResponse getPaymentByRazorpayOrderId(String razorpayOrderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "razorpayOrderId", razorpayOrderId));
        return toPaymentResponse(payment);
    }

    public PageResponse<PaymentResponse> getUserPayments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> paymentPage = paymentRepository.findByUserId(userId, pageable);

        return PageResponse.<PaymentResponse>builder()
                .content(paymentPage.getContent().stream().map(this::toPaymentResponse).toList())
                .pageNumber(paymentPage.getNumber())
                .pageSize(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .last(paymentPage.isLast())
                .first(paymentPage.isFirst())
                .empty(paymentPage.isEmpty())
                .build();
    }

    @Transactional
    public PaymentResponse refundPayment(String razorpayPaymentId, BigDecimal amount) {
        if (razorpayClient == null) {
            throw new InternalServerException("Payment gateway is not configured");
        }

        Payment payment = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "razorpayPaymentId", razorpayPaymentId));

        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue());

            razorpayClient.payments.refund(razorpayPaymentId, refundRequest);

            payment.setStatus(PaymentStatus.REFUNDED);
            Payment savedPayment = paymentRepository.save(payment);

            log.info("Payment refunded successfully: {}", razorpayPaymentId);

            return toPaymentResponse(savedPayment);

        } catch (RazorpayException e) {
            log.error("Failed to refund payment: {}", e.getMessage());
            throw new InternalServerException("Failed to refund payment: " + e.getMessage());
        }
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .userId(payment.getUserId())
                .method(payment.getMethod())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public boolean isConfigured() {
        return razorpayClient != null;
    }
}
