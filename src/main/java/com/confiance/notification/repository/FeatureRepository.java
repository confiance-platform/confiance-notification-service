package com.confiance.notification.repository;

import com.confiance.notification.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    Optional<Feature> findByCode(String code);

    List<Feature> findByEnabled(Boolean enabled);

    List<Feature> findByCategory(String category);

    List<Feature> findByCategoryAndEnabled(String category, Boolean enabled);

    boolean existsByCode(String code);
}
