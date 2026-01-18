package com.confiance.notification.controller;

import com.confiance.common.dto.ApiResponse;
import com.confiance.common.dto.PageResponse;
import com.confiance.notification.dto.UserNotificationResponse;
import com.confiance.notification.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "User Notifications", description = "User notification management APIs")
public class UserNotificationController {

    private final UserNotificationService notificationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Notifications", description = "Get paginated list of user notifications")
    public ResponseEntity<ApiResponse<PageResponse<UserNotificationResponse>>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<UserNotificationResponse> response = notificationService.getUserNotifications(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get Unread Notifications", description = "Get paginated list of unread notifications")
    public ResponseEntity<ApiResponse<PageResponse<UserNotificationResponse>>> getUnreadNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<UserNotificationResponse> response = notificationService.getUnreadNotifications(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}/unread-count")
    @Operation(summary = "Get Unread Count", description = "Get count of unread notifications")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark as Read", description = "Mark a notification as read")
    public ResponseEntity<ApiResponse<UserNotificationResponse>> markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long userId) {
        UserNotificationResponse response = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @PutMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark All as Read", description = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(@PathVariable Long userId) {
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("markedCount", count)));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete Notification", description = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long userId) {
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }
}
