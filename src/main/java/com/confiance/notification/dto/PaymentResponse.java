package com.confiance.notification.dto;

import com.confiance.notification.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String orderId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private Long userId;
    private String method;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
