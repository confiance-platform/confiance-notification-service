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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
