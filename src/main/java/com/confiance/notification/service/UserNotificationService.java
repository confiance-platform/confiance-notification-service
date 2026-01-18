package com.confiance.notification.service;

import com.confiance.common.dto.PageResponse;
import com.confiance.common.exception.ResourceNotFoundException;
import com.confiance.notification.dto.UserNotificationResponse;
import com.confiance.notification.entity.UserNotification;
import com.confiance.notification.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationService {

    private final UserNotificationRepository notificationRepository;

    public PageResponse<UserNotificationResponse> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return buildPageResponse(notifications);
    }

    public PageResponse<UserNotificationResponse> getUnreadNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserNotification> notifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable);
        return buildPageResponse(notifications);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public UserNotificationResponse markAsRead(Long notificationId, Long userId) {
        UserNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        UserNotification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        UserNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notificationRepository.delete(notification);
    }

    @Transactional
    public UserNotification createNotification(Long userId, String title, String message, String type) {
        return createNotification(userId, title, message, type, null, null);
    }

    @Transactional
    public UserNotification createNotification(Long userId, String title, String message, String type, String actionUrl, String icon) {
        UserNotification notification = UserNotification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .actionUrl(actionUrl)
                .icon(icon)
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    private PageResponse<UserNotificationResponse> buildPageResponse(Page<UserNotification> page) {
        return PageResponse.<UserNotificationResponse>builder()
                .content(page.getContent().stream()
                        .map(this::toResponse)
                        .toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .empty(page.isEmpty())
                .build();
    }

    private UserNotificationResponse toResponse(UserNotification notification) {
        return UserNotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .actionUrl(notification.getActionUrl())
                .icon(notification.getIcon())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
