package com.confiance.notification.repository;

import com.confiance.notification.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByCodeAndIsActiveTrue(String code);

    Optional<EmailTemplate> findByCode(String code);

    List<EmailTemplate> findByIsActiveTrue();

    List<EmailTemplate> findByCategory(String category);

    List<EmailTemplate> findByCategoryAndIsActiveTrue(String category);

    boolean existsByCode(String code);
}
