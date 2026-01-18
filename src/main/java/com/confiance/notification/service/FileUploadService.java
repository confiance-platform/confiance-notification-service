package com.confiance.notification.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.confiance.common.exception.BadRequestException;
import com.confiance.common.exception.InternalServerException;
import com.confiance.notification.dto.FileUploadResponse;
import com.confiance.notification.entity.FileUpload;
import com.confiance.notification.enums.FileType;
import com.confiance.notification.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    private final Cloudinary cloudinary;
    private final FileUploadRepository fileUploadRepository;

    @Autowired
    public FileUploadService(@Autowired(required = false) Cloudinary cloudinary,
                              FileUploadRepository fileUploadRepository) {
        this.cloudinary = cloudinary;
        this.fileUploadRepository = fileUploadRepository;
    }

    @Value("${cloudinary.folder:confiance}")
    private String defaultFolder;

    @Value("${cloudinary.max-file-size:10485760}")
    private long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv");
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList("mp4", "avi", "mov", "wmv", "mkv");

    public FileUploadResponse uploadImage(MultipartFile file, Long userId, String folder, String entityType, Long entityId) {
        validateFile(file, ALLOWED_IMAGE_EXTENSIONS);
        return uploadToCloudinary(file, userId, folder, "image", entityType, entityId);
    }

    public FileUploadResponse uploadDocument(MultipartFile file, Long userId, String folder, String entityType, Long entityId) {
        validateFile(file, ALLOWED_DOCUMENT_EXTENSIONS);
        return uploadToCloudinary(file, userId, folder, "raw", entityType, entityId);
    }

    public FileUploadResponse uploadVideo(MultipartFile file, Long userId, String folder, String entityType, Long entityId) {
        validateFile(file, ALLOWED_VIDEO_EXTENSIONS);
        return uploadToCloudinary(file, userId, folder, "video", entityType, entityId);
    }

    public FileUploadResponse uploadFile(MultipartFile file, Long userId, String folder, String entityType, Long entityId) {
        validateFileSize(file);
        String extension = getFileExtension(file.getOriginalFilename());

        if (ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            return uploadImage(file, userId, folder, entityType, entityId);
        } else if (ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
            return uploadVideo(file, userId, folder, entityType, entityId);
        } else {
            return uploadToCloudinary(file, userId, folder, "raw", entityType, entityId);
        }
    }

    private FileUploadResponse uploadToCloudinary(MultipartFile file, Long userId, String folder,
                                                   String resourceType, String entityType, Long entityId) {
        if (cloudinary == null) {
            throw new InternalServerException("Cloudinary is not configured");
        }

        try {
            String effectiveFolder = folder != null ? folder : defaultFolder;
            String publicId = effectiveFolder + "/" + UUID.randomUUID().toString();

            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", resourceType,
                    "overwrite", true
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), uploadOptions);

            String cloudinaryPublicId = (String) result.get("public_id");
            String url = (String) result.get("url");
            String secureUrl = (String) result.get("secure_url");
            String format = (String) result.get("format");
            Number width = (Number) result.get("width");
            Number height = (Number) result.get("height");
            Number bytes = (Number) result.get("bytes");

            FileType fileType = determineFileType(resourceType, format);

            FileUpload fileUpload = FileUpload.builder()
                    .publicId(cloudinaryPublicId)
                    .url(url)
                    .secureUrl(secureUrl)
                    .originalFileName(file.getOriginalFilename())
                    .fileName(cloudinaryPublicId)
                    .fileType(fileType)
                    .format(format)
                    .size(bytes != null ? bytes.longValue() : file.getSize())
                    .width(width != null ? width.intValue() : null)
                    .height(height != null ? height.intValue() : null)
                    .folder(effectiveFolder)
                    .userId(userId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .build();

            FileUpload savedUpload = fileUploadRepository.save(fileUpload);

            log.info("File uploaded successfully to Cloudinary: {}", cloudinaryPublicId);

            return FileUploadResponse.builder()
                    .fileId(savedUpload.getId().toString())
                    .publicId(cloudinaryPublicId)
                    .url(url)
                    .secureUrl(secureUrl)
                    .fileName(cloudinaryPublicId)
                    .originalFileName(file.getOriginalFilename())
                    .fileType(fileType)
                    .format(format)
                    .size(savedUpload.getSize())
                    .width(savedUpload.getWidth())
                    .height(savedUpload.getHeight())
                    .folder(effectiveFolder)
                    .uploadedAt(savedUpload.getCreatedAt())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage());
            throw new InternalServerException("Failed to upload file: " + e.getMessage());
        }
    }

    public void deleteFile(String publicId) {
        if (cloudinary == null) {
            throw new InternalServerException("Cloudinary is not configured");
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            fileUploadRepository.findByPublicId(publicId).ifPresent(fileUpload -> {
                fileUpload.setDeletedAt(LocalDateTime.now());
                fileUploadRepository.save(fileUpload);
            });

            log.info("File deleted successfully from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", e.getMessage());
            throw new InternalServerException("Failed to delete file: " + e.getMessage());
        }
    }

    public FileUploadResponse getFileByPublicId(String publicId) {
        return fileUploadRepository.findByPublicId(publicId)
                .map(this::toResponse)
                .orElseThrow(() -> new BadRequestException("File not found"));
    }

    public List<FileUploadResponse> getFilesByEntity(String entityType, Long entityId) {
        return fileUploadRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FileUploadResponse toResponse(FileUpload fileUpload) {
        return FileUploadResponse.builder()
                .fileId(fileUpload.getId().toString())
                .publicId(fileUpload.getPublicId())
                .url(fileUpload.getUrl())
                .secureUrl(fileUpload.getSecureUrl())
                .fileName(fileUpload.getFileName())
                .originalFileName(fileUpload.getOriginalFileName())
                .fileType(fileUpload.getFileType())
                .format(fileUpload.getFormat())
                .size(fileUpload.getSize())
                .width(fileUpload.getWidth())
                .height(fileUpload.getHeight())
                .folder(fileUpload.getFolder())
                .uploadedAt(fileUpload.getCreatedAt())
                .build();
    }

    private void validateFile(MultipartFile file, List<String> allowedExtensions) {
        validateFileSize(file);

        String extension = getFileExtension(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new BadRequestException("File type not allowed. Allowed types: " + String.join(", ", allowedExtensions));
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private FileType determineFileType(String resourceType, String format) {
        if ("image".equals(resourceType)) {
            return FileType.IMAGE;
        } else if ("video".equals(resourceType)) {
            return FileType.VIDEO;
        } else if (ALLOWED_DOCUMENT_EXTENSIONS.contains(format)) {
            return FileType.DOCUMENT;
        }
        return FileType.OTHER;
    }

    public boolean isConfigured() {
        return cloudinary != null;
    }
}
