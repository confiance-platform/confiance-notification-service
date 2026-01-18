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
public class UserNotificationResponse {

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private String actionUrl;
    private String icon;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
