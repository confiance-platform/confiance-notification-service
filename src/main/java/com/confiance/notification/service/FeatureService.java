package com.confiance.notification.service;

import com.confiance.notification.entity.Feature;
import com.confiance.notification.repository.FeatureRepository;
import com.confiance.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureService {

    private final FeatureRepository featureRepository;

    // Simple in-memory cache with TTL
    private final Map<String, CachedFeature> featureCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60000; // 1 minute cache

    // Feature codes as constants
    public static final String FEATURE_EMAIL = "EMAIL";
    public static final String FEATURE_SMS = "SMS";
    public static final String FEATURE_OTP = "OTP";
    public static final String FEATURE_PAYMENT = "PAYMENT";
    public static final String FEATURE_FILE_UPLOAD = "FILE_UPLOAD";
    public static final String FEATURE_PUSH_NOTIFICATION = "PUSH_NOTIFICATION";
    public static final String FEATURE_WELCOME_EMAIL = "WELCOME_EMAIL";
    public static final String FEATURE_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String FEATURE_PAYMENT_NOTIFICATION = "PAYMENT_NOTIFICATION";

    /**
     * Check if a feature is enabled
     * Uses cache to avoid database hits on every call
     */
    public boolean isEnabled(String featureCode) {
        CachedFeature cached = featureCache.get(featureCode);

        if (cached != null && !cached.isExpired()) {
            return cached.enabled;
        }

        // Cache miss or expired, fetch from database
        Optional<Feature> feature = featureRepository.findByCode(featureCode);
        boolean enabled = feature.map(Feature::getEnabled).orElse(true); // Default to enabled if not found

        featureCache.put(featureCode, new CachedFeature(enabled, System.currentTimeMillis()));
        log.debug("Feature {} is {}", featureCode, enabled ? "enabled" : "disabled");

        return enabled;
    }

    /**
     * Check if feature is enabled, with fallback behavior
     * Returns the disabled message if feature is disabled
     */
    public FeatureStatus getFeatureStatus(String featureCode) {
        Optional<Feature> feature = featureRepository.findByCode(featureCode);

        if (feature.isEmpty()) {
            return FeatureStatus.builder()
                    .code(featureCode)
                    .enabled(true)
                    .message("Feature not configured, defaulting to enabled")
                    .build();
        }

        Feature f = feature.get();
        return FeatureStatus.builder()
                .code(f.getCode())
                .name(f.getName())
                .enabled(f.getEnabled())
                .message(f.getEnabled() ? "Feature is active" : f.getDisabledMessage())
                .disabledAt(f.getDisabledAt())
                .disabledBy(f.getDisabledBy())
                .build();
    }

    public List<Feature> getAllFeatures() {
        return featureRepository.findAll();
    }

    public List<Feature> getFeaturesByCategory(String category) {
        return featureRepository.findByCategory(category);
    }

    public Optional<Feature> getFeature(String code) {
        return featureRepository.findByCode(code);
    }

    @Transactional
    public Feature enableFeature(String code, String enabledBy) {
        Feature feature = featureRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + code));

        feature.setEnabled(true);
        feature.setDisabledAt(null);
        feature.setDisabledBy(null);
        feature.setDisabledMessage(null);

        Feature saved = featureRepository.save(feature);

        // Invalidate cache
        featureCache.remove(code);

        log.info("Feature {} enabled by {}", code, enabledBy);
        return saved;
    }

    @Transactional
    public Feature disableFeature(String code, String disabledBy, String reason) {
        Feature feature = featureRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + code));

        feature.setEnabled(false);
        feature.setDisabledAt(LocalDateTime.now());
        feature.setDisabledBy(disabledBy);
        feature.setDisabledMessage(reason);

        Feature saved = featureRepository.save(feature);

        // Invalidate cache
        featureCache.remove(code);

        log.warn("Feature {} disabled by {} - Reason: {}", code, disabledBy, reason);
        return saved;
    }

    @Transactional
    public Feature createFeature(Feature feature) {
        if (featureRepository.existsByCode(feature.getCode())) {
            throw new IllegalArgumentException("Feature with code '" + feature.getCode() + "' already exists");
        }
        return featureRepository.save(feature);
    }

    @Transactional
    public Feature updateFeature(String code, Feature updatedFeature) {
        Feature existing = featureRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + code));

        existing.setName(updatedFeature.getName());
        existing.setDescription(updatedFeature.getDescription());
        existing.setCategory(updatedFeature.getCategory());

        // Invalidate cache
        featureCache.remove(code);

        return featureRepository.save(existing);
    }

    /**
     * Clear all cached features
     */
    public void clearCache() {
        featureCache.clear();
        log.info("Feature cache cleared");
    }

    // Inner class for cache entry
    private static class CachedFeature {
        final boolean enabled;
        final long timestamp;

        CachedFeature(boolean enabled, long timestamp) {
            this.enabled = enabled;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    // DTO for feature status
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FeatureStatus {
        private String code;
        private String name;
        private boolean enabled;
        private String message;
        private LocalDateTime disabledAt;
        private String disabledBy;
    }
}
