package com.confiance.notification.config;

import com.confiance.notification.entity.Feature;
import com.confiance.notification.repository.FeatureRepository;
import com.confiance.notification.service.FeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run before other initializers
public class FeatureInitializer implements CommandLineRunner {

    private final FeatureRepository featureRepository;

    @Override
    public void run(String... args) {
        initializeFeatures();
    }

    private void initializeFeatures() {
        // Communication Features
        createFeatureIfNotExists(
                FeatureService.FEATURE_EMAIL,
                "Email Service",
                "Enable/disable all email sending functionality",
                "COMMUNICATION",
                true
        );

        createFeatureIfNotExists(
                FeatureService.FEATURE_SMS,
                "SMS Service",
                "Enable/disable all SMS sending functionality",
                "COMMUNICATION",
                true
        );

        createFeatureIfNotExists(
                FeatureService.FEATURE_OTP,
                "OTP Service",
                "Enable/disable OTP generation and verification",
                "COMMUNICATION",
                true
        );

        createFeatureIfNotExists(
                FeatureService.FEATURE_PUSH_NOTIFICATION,
                "Push Notifications",
                "Enable/disable push notification sending",
                "COMMUNICATION",
                true
        );

        // Specific Email Features
        createFeatureIfNotExists(
                FeatureService.FEATURE_WELCOME_EMAIL,
                "Welcome Email",
                "Enable/disable welcome emails for new users",
                "EMAIL",
                true
        );

        createFeatureIfNotExists(
                FeatureService.FEATURE_PASSWORD_RESET,
                "Password Reset Email",
                "Enable/disable password reset emails",
                "EMAIL",
                true
        );

        createFeatureIfNotExists(
                FeatureService.FEATURE_PAYMENT_NOTIFICATION,
                "Payment Notification Email",
                "Enable/disable payment confirmation emails",
                "EMAIL",
                true
        );

        // Payment Features
        createFeatureIfNotExists(
                FeatureService.FEATURE_PAYMENT,
                "Payment Gateway",
                "Enable/disable payment processing via Razorpay",
                "PAYMENT",
                true
        );

        // File Upload Features
        createFeatureIfNotExists(
                FeatureService.FEATURE_FILE_UPLOAD,
                "File Upload",
                "Enable/disable file upload to Cloudinary",
                "STORAGE",
                true
        );

        log.info("Feature flags initialization completed");
    }

    private void createFeatureIfNotExists(String code, String name, String description, String category, boolean enabled) {
        if (featureRepository.existsByCode(code)) {
            log.debug("Feature {} already exists", code);
            return;
        }

        Feature feature = Feature.builder()
                .code(code)
                .name(name)
                .description(description)
                .category(category)
                .enabled(enabled)
                .build();

        featureRepository.save(feature);
        log.info("Created feature flag: {} ({})", code, enabled ? "enabled" : "disabled");
    }
}
