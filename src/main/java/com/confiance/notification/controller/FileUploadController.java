package com.confiance.notification.controller;

import com.confiance.common.dto.ApiResponse;
import com.confiance.notification.dto.FileUploadResponse;
import com.confiance.notification.ratelimit.RateLimitExceededException;
import com.confiance.notification.ratelimit.RateLimitResult;
import com.confiance.notification.ratelimit.RateLimiterService;
import com.confiance.notification.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "File upload and management APIs (Cloudinary)")
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final RateLimiterService rateLimiterService;

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Image", description = "Upload an image file to Cloudinary")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            HttpServletRequest httpRequest) {

        // Check rate limit for file upload
        String identifier = userId != null ? String.valueOf(userId) : getClientIp(httpRequest);
        RateLimitResult rateLimitResult = rateLimiterService.checkFileUploadLimit(identifier);
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        FileUploadResponse response = fileUploadService.uploadImage(file, userId, folder, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
    }

    @PostMapping(value = "/upload/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Document", description = "Upload a document file to Cloudinary")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            HttpServletRequest httpRequest) {

        // Check rate limit for file upload
        String identifier = userId != null ? String.valueOf(userId) : getClientIp(httpRequest);
        RateLimitResult rateLimitResult = rateLimiterService.checkFileUploadLimit(identifier);
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        FileUploadResponse response = fileUploadService.uploadDocument(file, userId, folder, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", response));
    }

    @PostMapping(value = "/upload/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Video", description = "Upload a video file to Cloudinary")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            HttpServletRequest httpRequest) {

        // Check rate limit for file upload
        String identifier = userId != null ? String.valueOf(userId) : getClientIp(httpRequest);
        RateLimitResult rateLimitResult = rateLimiterService.checkFileUploadLimit(identifier);
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        FileUploadResponse response = fileUploadService.uploadVideo(file, userId, folder, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Video uploaded successfully", response));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload File", description = "Upload any file type to Cloudinary")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            HttpServletRequest httpRequest) {

        // Check rate limit for file upload
        String identifier = userId != null ? String.valueOf(userId) : getClientIp(httpRequest);
        RateLimitResult rateLimitResult = rateLimiterService.checkFileUploadLimit(identifier);
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException(rateLimitResult);
        }

        FileUploadResponse response = fileUploadService.uploadFile(file, userId, folder, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", response));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get File", description = "Get file details by public ID")
    public ResponseEntity<ApiResponse<FileUploadResponse>> getFile(@PathVariable String publicId) {
        FileUploadResponse response = fileUploadService.getFileByPublicId(publicId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get Files by Entity", description = "Get all files associated with an entity")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> getFilesByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<FileUploadResponse> response = fileUploadService.getFilesByEntity(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete File", description = "Delete a file from Cloudinary")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String publicId) {
        fileUploadService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    /**
     * Serve files stored on local disk when Cloudinary isn't configured.
     * URL format: /api/v1/files/local/{any/path/to/file.ext}
     * Uses a wildcard mapping so folder paths with '/' (e.g. avatars/2/uuid.png)
     * are accepted as a single relative path.
     */
    @GetMapping("/local/**")
    @Operation(summary = "Serve local file", description = "Serve a locally stored upload (fallback when Cloudinary is not configured)")
    public ResponseEntity<Resource> serveLocal(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/api/v1/files/local/";
        int idx = uri.indexOf(prefix);
        if (idx < 0) return ResponseEntity.notFound().build();
        String relative = uri.substring(idx + prefix.length());
        Path path = fileUploadService.resolveLocalFileByRelative(relative);
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = MediaTypeFactory.getMediaType(path.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(new FileSystemResource(path));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
