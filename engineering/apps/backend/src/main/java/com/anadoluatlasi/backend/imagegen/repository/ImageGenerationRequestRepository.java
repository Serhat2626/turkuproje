package com.anadoluatlasi.backend.imagegen.repository;

import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequest;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-007: Image generation request repository.
 *
 * <p>Generation request CRUD ve status/lineage bazlı sorgulama desteği sağlar.
 * Idempotency key ile duplicate generation engelleme sorguları içerir.</p>
 */
@Repository
public interface ImageGenerationRequestRepository extends JpaRepository<ImageGenerationRequest, UUID> {

    /**
     * Idempotency key ile request arar. Duplicate prevention için kullanılır.
     */
    Optional<ImageGenerationRequest> findByIdempotencyKey(String idempotencyKey);

    /**
     * Idempotency key mevcutluğunu kontrol eder.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Belirli VisualAssetPlan'a ait tüm generation request'leri listeler.
     */
    List<ImageGenerationRequest> findByVisualAssetPlanId(UUID visualAssetPlanId);

    /**
     * Belirli VisualAssetItem'a ait tüm generation request'leri listeler.
     */
    List<ImageGenerationRequest> findByVisualAssetItemId(UUID visualAssetItemId);

    /**
     * Status bazlı generation request listesi.
     */
    List<ImageGenerationRequest> findByStatus(ImageGenerationRequestStatus status);

    /**
     * Correlation ID ile arama. Observability/debugging için.
     */
    Optional<ImageGenerationRequest> findByCorrelationId(UUID correlationId);

    /**
     * Belirli plan ve status kombinasyonuyla sorgulama.
     */
    List<ImageGenerationRequest> findByVisualAssetPlanIdAndStatus(
            UUID visualAssetPlanId, ImageGenerationRequestStatus status);

    /**
     * Belirli item ve status kombinasyonu — aktif request var mı kontrolü.
     */
    boolean existsByVisualAssetItemIdAndStatusIn(
            UUID visualAssetItemId, List<ImageGenerationRequestStatus> statuses);
}
