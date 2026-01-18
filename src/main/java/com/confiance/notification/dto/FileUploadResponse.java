package com.confiance.notification.dto;

import com.confiance.notification.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String fileId;
    private String publicId;
    private String url;
    private String secureUrl;
    private String fileName;
    private String originalFileName;
    private FileType fileType;
    private String format;
    private Long size;
    private Integer width;
    private Integer height;
    private String folder;
    private LocalDateTime uploadedAt;
}
