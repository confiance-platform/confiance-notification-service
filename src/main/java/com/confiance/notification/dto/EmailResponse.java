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
public class EmailResponse {
    private String messageId;
    private String status;
    private String recipient;
    private LocalDateTime sentAt;
    private String provider;
}
