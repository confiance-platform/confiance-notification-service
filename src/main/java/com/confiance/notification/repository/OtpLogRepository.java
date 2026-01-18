package com.confiance.notification.repository;

import com.confiance.notification.entity.OtpLog;
import com.confiance.notification.enums.OtpPurpose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpLogRepository extends JpaRepository<OtpLog, Long> {

    Page<OtpLog> findByIdentifier(String identifier, Pageable pageable);

    Page<OtpLog> findByUserId(Long userId, Pageable pageable);

    Optional<OtpLog> findTopByIdentifierAndPurposeOrderByCreatedAtDesc(String identifier, OtpPurpose purpose);

    List<OtpLog> findByIdentifierAndPurposeAndCreatedAtAfter(String identifier, OtpPurpose purpose, LocalDateTime after);

    long countByIdentifierAndPurposeAndCreatedAtAfter(String identifier, OtpPurpose purpose, LocalDateTime after);
}
