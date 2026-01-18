package com.confiance.notification.dto;

import com.confiance.notification.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {
    private String orderId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private String currency;
    private String receipt;
    private PaymentStatus status;
    private Long userId;
    private String razorpayKeyId;
    private Map<String, String> notes;
    private LocalDateTime createdAt;
}
