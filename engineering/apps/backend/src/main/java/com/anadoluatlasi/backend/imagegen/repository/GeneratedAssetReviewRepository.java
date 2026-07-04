package com.anadoluatlasi.backend.imagegen.repository;

import com.anadoluatlasi.backend.imagegen.entity.GeneratedAssetReview;
import com.anadoluatlasi.backend.imagegen.entity.GeneratedAssetReviewDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * FP-007: Generated asset review repository.
 *
 * <p>Human review kararlarının audit kaydı sorgulama desteği sağlar.
 * Her review explicit admin aksiyonuyla oluşturulur — otomatik decision yoktur.</p>
 */
@Repository
public interface GeneratedAssetReviewRepository extends JpaRepository<GeneratedAssetReview, UUID> {

    /**
     * Belirli generated asset'e ait tüm review kayıtlarını listeler.
     */
    List<GeneratedAssetReview> findByGeneratedAssetId(UUID generatedAssetId);

    /**
     * Decision bazlı listeleme.
     */
    List<GeneratedAssetReview> findByDecision(GeneratedAssetReviewDecision decision);

    /**
     * Reviewer bazlı listeleme.
     */
    List<GeneratedAssetReview> findByReviewedBy(String reviewedBy);
}
