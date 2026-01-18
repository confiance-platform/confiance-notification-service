package com.confiance.notification.repository;

import com.confiance.notification.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findByRecipient(String recipient, Pageable pageable);

    Page<EmailLog> findByUserId(Long userId, Pageable pageable);

    Page<EmailLog> findByStatus(String status, Pageable pageable);

    List<EmailLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);
}
