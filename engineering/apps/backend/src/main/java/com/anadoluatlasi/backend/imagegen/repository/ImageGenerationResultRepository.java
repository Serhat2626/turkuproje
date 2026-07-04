package com.anadoluatlasi.backend.imagegen.repository;

import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-007: Image generation result repository.
 *
 * <p>Normalized provider generation result sorgulama desteği sağlar.
 * Raw provider payload/response bu katmanda saklanmaz.</p>
 */
@Repository
public interface ImageGenerationResultRepository extends JpaRepository<ImageGenerationResult, UUID> {

    /**
     * Belirli generation request'e ait sonuçları listeler.
     */
    List<ImageGenerationResult> findByImageGenerationRequestId(UUID imageGenerationRequestId);

    /**
     * Correlation ID ile arama. Observability/debugging için.
     */
    Optional<ImageGenerationResult> findByCorrelationId(UUID correlationId);

    /**
     * Provider request ID ile arama. Internal debugging için.
     */
    Optional<ImageGenerationResult> findByProviderRequestId(String providerRequestId);

    /**
     * Belirli VisualAssetPlan'a ait tüm sonuçları listeler.
     */
    List<ImageGenerationResult> findByVisualAssetPlanId(UUID visualAssetPlanId);

    /**
     * Belirli VisualAssetItem'a ait tüm sonuçları listeler.
     */
    List<ImageGenerationResult> findByVisualAssetItemId(UUID visualAssetItemId);
}
