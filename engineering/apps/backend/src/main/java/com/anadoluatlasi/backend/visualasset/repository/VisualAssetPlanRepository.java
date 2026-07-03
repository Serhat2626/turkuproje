package com.anadoluatlasi.backend.visualasset.repository;

import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * FP-006: Visual Asset Plan repository.
 *
 * <p>Görsel varlık planları için veri erişim katmanı.
 * Kaynak storyboard, proje ve status bazlı sorgulama desteği sağlar.</p>
 */
@Repository
public interface VisualAssetPlanRepository extends JpaRepository<VisualAssetPlan, UUID> {

    /**
     * Belirli storyboard'a ait tüm görsel planları versiyon sırasıyla listeler.
     */
    List<VisualAssetPlan> findBySourceStoryboardDraftIdOrderByVersionDesc(UUID sourceStoryboardDraftId);

    /**
     * Belirli projeye ait tüm görsel planları versiyon sırasıyla listeler.
     */
    List<VisualAssetPlan> findByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    /**
     * Belirli storyboard ve status kombinasyonuyla sorgulama.
     */
    List<VisualAssetPlan> findBySourceStoryboardDraftIdAndStatus(
            UUID sourceStoryboardDraftId, VisualAssetPlanStatus status);

    /**
     * Belirli storyboard için en yüksek versiyon numarasını döndürür.
     * Plan yoksa 0 döner.
     */
    @Query("SELECT COALESCE(MAX(v.version), 0) FROM VisualAssetPlan v " +
           "WHERE v.sourceStoryboardDraft.id = :storyboardId")
    int findMaxVersionBySourceStoryboardDraftId(@Param("storyboardId") UUID storyboardId);

    /**
     * Belirli storyboard için belirli status'ta plan var mı kontrol eder.
     * One-approved-per-storyboard kuralı service-level'da bu metotla enforce edilir.
     */
    boolean existsBySourceStoryboardDraftIdAndStatus(
            UUID sourceStoryboardDraftId, VisualAssetPlanStatus status);
}
