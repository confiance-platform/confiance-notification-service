package com.confiance.notification.repository;

import com.confiance.notification.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<UserNotification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, boolean isRead, Pageable pageable);

    long countByUserIdAndIsRead(Long userId, boolean isRead);

    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.readAt = :now WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(Long userId, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.createdAt < :before")
    int deleteOldNotifications(LocalDateTime before);
}
