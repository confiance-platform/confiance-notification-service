package com.confiance.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "userId"),
        @Index(name = "idx_notification_read", columnList = "isRead"),
        @Index(name = "idx_notification_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String type; // INFO, WARNING, SUCCESS, ERROR, TRADE, RECOMMENDATION, etc.

    private String actionUrl; // Optional link for the notification

    private String icon; // Optional icon name

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    private LocalDateTime readAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
