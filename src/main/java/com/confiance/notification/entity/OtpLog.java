package com.confiance.notification.entity;

import com.confiance.notification.enums.OtpProvider;
import com.confiance.notification.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    private OtpProvider provider;

    @Column(nullable = false)
    private String status;

    private Long userId;

    private int attemptCount;

    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;

    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
