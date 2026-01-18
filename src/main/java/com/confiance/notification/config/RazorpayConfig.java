package com.confiance.notification.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() {
        if (!StringUtils.hasText(keyId) || !StringUtils.hasText(keySecret)) {
            log.warn("Razorpay credentials not configured");
            return null;
        }
        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay client: {}", e.getMessage());
            return null;
        }
    }
}
