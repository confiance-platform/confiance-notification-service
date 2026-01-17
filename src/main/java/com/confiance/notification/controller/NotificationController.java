package com.confiance.notification.controller;

import com.confiance.common.dto.ApiResponse;
import com.confiance.notification.dto.EmailRequest;
import com.confiance.notification.dto.EmailResponse;
import com.confiance.notification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Email notification APIs")
public class NotificationController {

    private final EmailService emailService;

    @PostMapping("/send-email")
    @Operation(summary = "Send Email", description = "Send an email notification")
    public ResponseEntity<ApiResponse<EmailResponse>> sendEmail(@Valid @RequestBody EmailRequest request) {
        EmailResponse response = emailService.sendEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email sent successfully", response));
    }

    @PostMapping("/send-email/simple")
    @Operation(summary = "Send Simple Email", description = "Send a simple text email")
    public ResponseEntity<ApiResponse<Void>> sendSimpleEmail(@RequestBody Map<String, String> request) {
        emailService.sendSimpleEmail(
                request.get("to"),
                request.get("subject"),
                request.get("body")
        );
        return ResponseEntity.ok(ApiResponse.success("Email sent successfully", null));
    }

    @PostMapping("/send-email/template")
    @Operation(summary = "Send Templated Email", description = "Send an email using a template")
    public ResponseEntity<ApiResponse<EmailResponse>> sendTemplatedEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String templateName,
            @RequestBody Map<String, Object> variables,
            @RequestParam(required = false) Long userId) {
        EmailResponse response = emailService.sendTemplatedEmail(to, subject, templateName, variables, userId);
        return ResponseEntity.ok(ApiResponse.success("Email sent successfully", response));
    }

    @PostMapping("/send-email/async")
    @Operation(summary = "Send Email Async", description = "Send an email asynchronously")
    public ResponseEntity<ApiResponse<Void>> sendEmailAsync(@Valid @RequestBody EmailRequest request) {
        emailService.sendEmailAsync(request);
        return ResponseEntity.accepted().body(ApiResponse.success("Email queued for delivery", null));
    }
}
