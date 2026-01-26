package com.confiance.notification.controller;

import com.confiance.common.dto.ApiResponse;
import com.confiance.notification.entity.Feature;
import com.confiance.notification.service.FeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
@Tag(name = "Feature Flags", description = "Feature flag management APIs")
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    @Operation(summary = "Get All Features", description = "Get all feature flags")
    public ResponseEntity<ApiResponse<List<Feature>>> getAllFeatures() {
        List<Feature> features = featureService.getAllFeatures();
        return ResponseEntity.ok(ApiResponse.success("Features retrieved successfully", features));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get Feature by Code", description = "Get feature flag by its code")
    public ResponseEntity<ApiResponse<Feature>> getFeature(@PathVariable String code) {
        return featureService.getFeature(code)
                .map(feature -> ResponseEntity.ok(ApiResponse.success("Feature retrieved successfully", feature)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{code}/status")
    @Operation(summary = "Get Feature Status", description = "Check if a feature is enabled with details")
    public ResponseEntity<ApiResponse<FeatureService.FeatureStatus>> getFeatureStatus(@PathVariable String code) {
        FeatureService.FeatureStatus status = featureService.getFeatureStatus(code);
        return ResponseEntity.ok(ApiResponse.success("Feature status retrieved", status));
    }

    @GetMapping("/{code}/enabled")
    @Operation(summary = "Check Feature Enabled", description = "Quick check if a feature is enabled")
    public ResponseEntity<ApiResponse<Boolean>> isFeatureEnabled(@PathVariable String code) {
        boolean enabled = featureService.isEnabled(code);
        return ResponseEntity.ok(ApiResponse.success(
                enabled ? "Feature is enabled" : "Feature is disabled",
                enabled
        ));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get Features by Category", description = "Get all features in a category")
    public ResponseEntity<ApiResponse<List<Feature>>> getFeaturesByCategory(@PathVariable String category) {
        List<Feature> features = featureService.getFeaturesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success("Features retrieved successfully", features));
    }

    @PostMapping
    @Operation(summary = "Create Feature", description = "Create a new feature flag")
    public ResponseEntity<ApiResponse<Feature>> createFeature(@RequestBody Feature feature) {
        Feature created = featureService.createFeature(feature);
        return ResponseEntity.ok(ApiResponse.success("Feature created successfully", created));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Update Feature", description = "Update an existing feature flag")
    public ResponseEntity<ApiResponse<Feature>> updateFeature(
            @PathVariable String code,
            @RequestBody Feature feature) {
        Feature updated = featureService.updateFeature(code, feature);
        return ResponseEntity.ok(ApiResponse.success("Feature updated successfully", updated));
    }

    @PostMapping("/{code}/enable")
    @Operation(summary = "Enable Feature", description = "Enable a feature flag")
    public ResponseEntity<ApiResponse<Feature>> enableFeature(
            @PathVariable String code,
            @RequestParam(defaultValue = "system") String enabledBy) {
        Feature enabled = featureService.enableFeature(code, enabledBy);
        return ResponseEntity.ok(ApiResponse.success("Feature enabled successfully", enabled));
    }

    @PostMapping("/{code}/disable")
    @Operation(summary = "Disable Feature", description = "Disable a feature flag")
    public ResponseEntity<ApiResponse<Feature>> disableFeature(
            @PathVariable String code,
            @RequestBody Map<String, String> request) {
        String disabledBy = request.getOrDefault("disabledBy", "system");
        String reason = request.getOrDefault("reason", "Disabled via API");
        Feature disabled = featureService.disableFeature(code, disabledBy, reason);
        return ResponseEntity.ok(ApiResponse.success("Feature disabled successfully", disabled));
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Clear Feature Cache", description = "Clear the feature flag cache")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        featureService.clearCache();
        return ResponseEntity.ok(ApiResponse.success("Feature cache cleared", null));
    }
}
