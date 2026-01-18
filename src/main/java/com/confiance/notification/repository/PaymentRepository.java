package com.confiance.notification.repository;

import com.confiance.notification.entity.Payment;
import com.confiance.notification.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status, Pageable pageable);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime before);

    long countByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);
}
