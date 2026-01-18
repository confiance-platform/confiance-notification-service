package com.confiance.notification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private BigDecimal amount;

    private String currency;

    @NotNull(message = "User ID is required")
    private Long userId;

    private String receipt;

    private String description;

    private Map<String, String> notes;

    private CustomerInfo customer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String name;
        private String email;
        private String contact;
    }
}
