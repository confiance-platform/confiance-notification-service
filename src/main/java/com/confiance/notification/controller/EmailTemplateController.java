package com.confiance.notification.controller;

import com.confiance.common.dto.ApiResponse;
import com.confiance.notification.entity.EmailTemplate;
import com.confiance.notification.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/email-templates")
@RequiredArgsConstructor
@Tag(name = "Email Templates", description = "Email template management APIs")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @GetMapping
    @Operation(summary = "Get All Templates", description = "Get all active email templates")
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getAllTemplates() {
        List<EmailTemplate> templates = emailTemplateService.getAllActiveTemplates();
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved successfully", templates));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get Template by Code", description = "Get email template by its code")
    public ResponseEntity<ApiResponse<EmailTemplate>> getTemplateByCode(@PathVariable String code) {
        return emailTemplateService.findByCode(code)
                .map(template -> ResponseEntity.ok(ApiResponse.success("Template retrieved successfully", template)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get Templates by Category", description = "Get email templates by category")
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getTemplatesByCategory(@PathVariable String category) {
        List<EmailTemplate> templates = emailTemplateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved successfully", templates));
    }

    @PostMapping
    @Operation(summary = "Create Template", description = "Create a new email template")
    public ResponseEntity<ApiResponse<EmailTemplate>> createTemplate(@RequestBody EmailTemplate template) {
        EmailTemplate created = emailTemplateService.createTemplate(template);
        return ResponseEntity.ok(ApiResponse.success("Template created successfully", created));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Update Template", description = "Update an existing email template")
    public ResponseEntity<ApiResponse<EmailTemplate>> updateTemplate(
            @PathVariable String code,
            @RequestBody EmailTemplate template) {
        EmailTemplate updated = emailTemplateService.updateTemplate(code, template);
        return ResponseEntity.ok(ApiResponse.success("Template updated successfully", updated));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete Template", description = "Soft delete an email template")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable String code) {
        emailTemplateService.deleteTemplate(code);
        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully", null));
    }

    @PostMapping("/{code}/preview")
    @Operation(summary = "Preview Template", description = "Preview a template with sample variables")
    public ResponseEntity<ApiResponse<EmailTemplateService.ProcessedTemplate>> previewTemplate(
            @PathVariable String code,
            @RequestBody Map<String, Object> variables) {
        EmailTemplateService.ProcessedTemplate processed = emailTemplateService.processEmailTemplate(code, variables);
        return ResponseEntity.ok(ApiResponse.success("Template preview generated", processed));
    }
}
