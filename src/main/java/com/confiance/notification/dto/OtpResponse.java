package com.confiance.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    private String identifier;
    private String status;
    private String message;
    private LocalDateTime expiresAt;
    private int remainingAttempts;
}
