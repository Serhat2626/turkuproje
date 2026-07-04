package com.anadoluatlasi.backend.imagegen.repository;

import com.anadoluatlasi.backend.imagegen.entity.GeneratedAssetStatus;
import com.anadoluatlasi.backend.imagegen.entity.ImageGeneratedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * FP-007: Generated image asset repository.
 *
 * <p>AI tarafından üretilmiş image asset'lerin review lifecycle sorgulama
 * desteği sağlar. Pending review queue, lineage ve status bazlı sorgular içerir.</p>
 */
@Repository
public interface ImageGeneratedAssetRepository extends JpaRepository<ImageGeneratedAsset, UUID> {

    /**
     * Status bazlı generated asset listesi.
     * Pending review queue: findByStatus(GENERATED_PENDING_REVIEW) ile kullanılır.
     */
    List<ImageGeneratedAsset> findByStatus(GeneratedAssetStatus status);

    /**
     * Belirli VisualAssetPlan'a ait generated asset'leri listeler.
     */
    List<ImageGeneratedAsset> findByVisualAssetPlanId(UUID visualAssetPlanId);

    /**
     * Belirli VisualAssetItem'a ait generated asset'leri listeler.
     */
    List<ImageGeneratedAsset> findByVisualAssetItemId(UUID visualAssetItemId);

    /**
     * Belirli generation request'e ait generated asset'leri listeler.
     */
    List<ImageGeneratedAsset> findByImageGenerationRequestId(UUID imageGenerationRequestId);

    /**
     * Belirli generation result'a ait generated asset'leri listeler.
     */
    List<ImageGeneratedAsset> findByImageGenerationResultId(UUID imageGenerationResultId);

    /**
     * Belirli media asset'e ait generated asset'i bulur.
     */
    List<ImageGeneratedAsset> findByImageMediaAssetId(UUID imageMediaAssetId);

    /**
     * Belirli content project'e ait generated asset'leri listeler.
     */
    List<ImageGeneratedAsset> findByContentProjectId(UUID contentProjectId);

    /**
     * Belirli plan ve status kombinasyonuyla sorgulama.
     */
    List<ImageGeneratedAsset> findByVisualAssetPlanIdAndStatus(
            UUID visualAssetPlanId, GeneratedAssetStatus status);
}
