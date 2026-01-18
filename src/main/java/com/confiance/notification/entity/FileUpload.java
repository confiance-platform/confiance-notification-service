package com.confiance.notification.entity;

import com.confiance.notification.enums.FileType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_uploads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String publicId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String secureUrl;

    @Column(nullable = false)
    private String originalFileName;

    private String fileName;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    private String format;

    private Long size;

    private Integer width;

    private Integer height;

    private String folder;

    private Long userId;

    private String entityType;

    private Long entityId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
