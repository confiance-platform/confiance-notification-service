package com.confiance.notification.controller;

import com.confiance.common.dto.ApiResponse;
import com.confiance.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    @PostMapping("/send-email")
    public ResponseEntity<ApiResponse<Void>> sendEmail(@RequestBody Map<String, String> request) {
        emailService.sendEmail(request.get("to"), request.get("subject"), request.get("body"));
        return ResponseEntity.ok(ApiResponse.success("Email sent successfully", null));
    }
}