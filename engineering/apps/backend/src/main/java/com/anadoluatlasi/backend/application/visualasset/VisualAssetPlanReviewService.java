package com.anadoluatlasi.backend.application.visualasset;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.visualasset.entity.SceneVisualPlan;
import com.anadoluatlasi.backend.visualasset.entity.ThumbnailPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlanStatus;
import com.anadoluatlasi.backend.visualasset.repository.VisualAssetPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * FP-006: Visual Asset Plan review iş akışını yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>updatePlan — Parent VisualAssetPlan alanlarını düzenle</li>
 *   <li>updateScenePlan — SceneVisualPlan prompt/stil alanlarını düzenle</li>
 *   <li>updateThumbnailPlan — ThumbnailPlan prompt/stil alanlarını düzenle</li>
 *   <li>approve — Explicit admin onayı</li>
 *   <li>reject — Explicit admin reddi</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>AI gateway çağırmaz</li>
 *   <li>Otomatik onay/red yapmaz</li>
 *   <li>READY_FOR_PUBLISH ayarlamaz</li>
 *   <li>Raw prompt/payload/secret saklamaz</li>
 *   <li>Media dosyası (image/video/audio) üretmez</li>
 *   <li>MediaAsset/GeneratedAsset oluşturmaz</li>
 *   <li>Generic status update davranışı sunmaz</li>
 *   <li>Image generation API çağırmaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualAssetPlanReviewService {

    private static final Set<VisualAssetPlanStatus> EDITABLE_STATUSES =
            Set.of(VisualAssetPlanStatus.AI_GENERATED, VisualAssetPlanStatus.NEEDS_REVIEW,
                    VisualAssetPlanStatus.APPROVED, VisualAssetPlanStatus.REJECTED);
    private static final Set<VisualAssetPlanStatus> APPROVABLE_STATUSES =
            Set.of(VisualAssetPlanStatus.AI_GENERATED, VisualAssetPlanStatus.NEEDS_REVIEW);
    private static final Set<VisualAssetPlanStatus> REJECTABLE_STATUSES =
            Set.of(VisualAssetPlanStatus.AI_GENERATED, VisualAssetPlanStatus.NEEDS_REVIEW);

    private final VisualAssetPlanRepository visualAssetPlanRepository;

    // ── Edit Parent Plan ────────────────────────────────────

    /**
     * VisualAssetPlan parent alanlarını düzenler.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>AI_GENERATED, NEEDS_REVIEW, APPROVED, REJECTED düzenlenebilir</li>
     *   <li>FAILED düzenlenemez</li>
     *   <li>Düzenleme sonrası status NEEDS_REVIEW olur</li>
     *   <li>editedAt/editedBy güncellenir</li>
     *   <li>Düzenleme asla APPROVED yapmaz</li>
     *   <li>providerType/modelName değiştirilmez</li>
     * </ul>
     */
    @Transactional
    public VisualAssetPlanReviewResult updatePlan(UUID planId,
                                                    UpdateVisualAssetPlanCommand command,
                                                    String adminId) {
        VisualAssetPlan plan = findPlanOrThrow(planId);
        validateEditable(plan);

        // İçerik alanlarını güncelle
        applyPlanEdits(plan, command);

        // Status transition
        plan.setStatus(VisualAssetPlanStatus.NEEDS_REVIEW);
        plan.setEditedAt(LocalDateTime.now());
        plan.setEditedBy(adminId);

        plan = visualAssetPlanRepository.save(plan);

        log.info("FP-006: VisualAssetPlan düzenlendi: planId={}, status={}, editedBy={}",
                plan.getId(), plan.getStatus(), adminId);

        return buildResult(plan, "Görsel varlık planı başarıyla düzenlendi");
    }

    // ── Edit Scene Plan ─────────────────────────────────────

    /**
     * SceneVisualPlan prompt/stil alanlarını düzenler.
     * Parent VisualAssetPlan status'ünü NEEDS_REVIEW yapar.
     */
    @Transactional
    public VisualAssetPlanReviewResult updateScenePlan(UUID planId,
                                                        int sceneNumber,
                                                        UpdateSceneVisualPlanCommand command,
                                                        String adminId) {
        VisualAssetPlan plan = findPlanOrThrow(planId);
        validateEditable(plan);

        SceneVisualPlan scenePlan = plan.getScenePlans().stream()
                .filter(sp -> sp.getSceneNumber() == sceneNumber)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "SceneVisualPlan (sceneNumber=" + sceneNumber + ")", planId));

        applySceneEdits(scenePlan, command);

        // Parent status transition
        plan.setStatus(VisualAssetPlanStatus.NEEDS_REVIEW);
        plan.setEditedAt(LocalDateTime.now());
        plan.setEditedBy(adminId);

        plan = visualAssetPlanRepository.save(plan);

        log.info("FP-006: SceneVisualPlan düzenlendi: planId={}, sceneNumber={}, editedBy={}",
                plan.getId(), sceneNumber, adminId);

        return buildResult(plan, "Sahne görsel planı başarıyla düzenlendi");
    }

    // ── Edit Thumbnail Plan ─────────────────────────────────

    /**
     * ThumbnailPlan prompt/stil alanlarını düzenler.
     * Parent VisualAssetPlan status'ünü NEEDS_REVIEW yapar.
     */
    @Transactional
    public VisualAssetPlanReviewResult updateThumbnailPlan(UUID planId,
                                                            UpdateThumbnailPlanCommand command,
                                                            String adminId) {
        VisualAssetPlan plan = findPlanOrThrow(planId);
        validateEditable(plan);

        ThumbnailPlan thumbnailPlan = plan.getThumbnailPlan();
        if (thumbnailPlan == null) {
            throw new NotFoundException("ThumbnailPlan", planId);
        }

        applyThumbnailEdits(thumbnailPlan, command);

        // Parent status transition
        plan.setStatus(VisualAssetPlanStatus.NEEDS_REVIEW);
        plan.setEditedAt(LocalDateTime.now());
        plan.setEditedBy(adminId);

        plan = visualAssetPlanRepository.save(plan);

        log.info("FP-006: ThumbnailPlan düzenlendi: planId={}, editedBy={}",
                plan.getId(), adminId);

        return buildResult(plan, "Thumbnail planı başarıyla düzenlendi");
    }

    // ── Approve ─────────────────────────────────────────────

    /**
     * VisualAssetPlan'ı explicit admin eylemiyle onaylar.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW onaylanabilir</li>
     *   <li>REJECTED doğrudan onaylanamaz — önce düzenlenmeli</li>
     *   <li>FAILED onaylanamaz</li>
     *   <li>Aynı storyboard için zaten APPROVED plan varsa onay reddedilir</li>
     *   <li>approvedAt/approvedBy güncellenir</li>
     *   <li>rejectedAt/rejectedBy/rejectionReason temizlenir</li>
     *   <li>READY_FOR_PUBLISH ayarlanmaz</li>
     *   <li>Image generation tetiklenmez</li>
     * </ul>
     */
    @Transactional
    public VisualAssetPlanReviewResult approve(UUID planId, String adminId) {
        VisualAssetPlan plan = findPlanOrThrow(planId);

        // Status kontrolü
        if (!APPROVABLE_STATUSES.contains(plan.getStatus())) {
            throw new BusinessException(
                    String.format("Görsel varlık planı onaylanamaz. Mevcut statü: %s, İzin verilen: %s",
                            plan.getStatus(), APPROVABLE_STATUSES));
        }

        // One-approved-per-storyboard kontrolü
        UUID storyboardId = plan.getSourceStoryboardDraft().getId();
        boolean alreadyApproved = visualAssetPlanRepository
                .existsBySourceStoryboardDraftIdAndStatus(storyboardId, VisualAssetPlanStatus.APPROVED);
        if (alreadyApproved) {
            throw new BusinessException(
                    String.format("Bu storyboard için zaten onaylanmış bir görsel plan mevcut. "
                            + "Yeni plan onaylamak için önce mevcut onayı kaldırın. "
                            + "StoryboardDraft ID: %s", storyboardId));
        }

        // Approve
        plan.setStatus(VisualAssetPlanStatus.APPROVED);
        plan.setApprovedAt(LocalDateTime.now());
        plan.setApprovedBy(adminId);

        // Önceki rejection bilgisini temizle
        plan.setRejectedAt(null);
        plan.setRejectedBy(null);
        plan.setRejectionReason(null);

        plan = visualAssetPlanRepository.save(plan);

        log.info("FP-006: VisualAssetPlan onaylandı: planId={}, version={}, approvedBy={}",
                plan.getId(), plan.getVersion(), adminId);

        return buildResult(plan, "Görsel varlık planı başarıyla onaylandı");
    }

    // ── Reject ──────────────────────────────────────────────

    /**
     * VisualAssetPlan'ı explicit admin eylemiyle reddeder.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW reddedilebilir</li>
     *   <li>APPROVED doğrudan reddedilemez</li>
     *   <li>REJECTED tekrar reddedilemez</li>
     *   <li>FAILED reddedilemez</li>
     *   <li>rejectedAt/rejectedBy/rejectionReason güncellenir</li>
     *   <li>Image generation tetiklenmez</li>
     * </ul>
     */
    @Transactional
    public VisualAssetPlanReviewResult reject(UUID planId, String adminId, String rejectionReason) {
        VisualAssetPlan plan = findPlanOrThrow(planId);

        // Status kontrolü
        if (!REJECTABLE_STATUSES.contains(plan.getStatus())) {
            throw new BusinessException(
                    String.format("Görsel varlık planı reddedilemez. Mevcut statü: %s, İzin verilen: %s",
                            plan.getStatus(), REJECTABLE_STATUSES));
        }

        // Reject
        plan.setStatus(VisualAssetPlanStatus.REJECTED);
        plan.setRejectedAt(LocalDateTime.now());
        plan.setRejectedBy(adminId);

        if (rejectionReason != null && !rejectionReason.isBlank()) {
            plan.setRejectionReason(rejectionReason.trim());
        }

        plan = visualAssetPlanRepository.save(plan);

        log.info("FP-006: VisualAssetPlan reddedildi: planId={}, version={}, rejectedBy={}",
                plan.getId(), plan.getVersion(), adminId);

        return buildResult(plan, "Görsel varlık planı reddedildi");
    }

    // ── Content Edit Helpers ────────────────────────────────

    private void applyPlanEdits(VisualAssetPlan plan, UpdateVisualAssetPlanCommand command) {
        if (command.getTitle() != null) {
            plan.setTitle(command.getTitle());
        }
        if (command.getSummary() != null) {
            plan.setSummary(command.getSummary());
        }
        if (command.getOverallVisualStyle() != null) {
            plan.setOverallVisualStyle(command.getOverallVisualStyle());
        }
        if (command.getAspectRatioStrategy() != null) {
            plan.setAspectRatioStrategy(command.getAspectRatioStrategy());
        }
        if (command.getCulturalAuthenticityGuidance() != null) {
            plan.setCulturalAuthenticityGuidance(command.getCulturalAuthenticityGuidance());
        }
        if (command.getCharacterContinuityGuidance() != null) {
            plan.setCharacterContinuityGuidance(command.getCharacterContinuityGuidance());
        }
        if (command.getLocationContinuityGuidance() != null) {
            plan.setLocationContinuityGuidance(command.getLocationContinuityGuidance());
        }
        if (command.getColorPaletteGuidance() != null) {
            plan.setColorPaletteGuidance(command.getColorPaletteGuidance());
        }
        if (command.getReviewNotes() != null) {
            plan.setReviewNotes(command.getReviewNotes());
        }
    }

    private void applySceneEdits(SceneVisualPlan scene, UpdateSceneVisualPlanCommand command) {
        if (command.getEnglishImagePrompt() != null) {
            scene.setEnglishImagePrompt(command.getEnglishImagePrompt());
        }
        if (command.getTurkishVisualContext() != null) {
            scene.setTurkishVisualContext(command.getTurkishVisualContext());
        }
        if (command.getNegativePrompt() != null) {
            scene.setNegativePrompt(command.getNegativePrompt());
        }
        if (command.getStyleDirective() != null) {
            scene.setStyleDirective(command.getStyleDirective());
        }
        if (command.getAspectRatio() != null) {
            scene.setAspectRatio(command.getAspectRatio());
        }
        if (command.getCompositionNotes() != null) {
            scene.setCompositionNotes(command.getCompositionNotes());
        }
        if (command.getCameraFraming() != null) {
            scene.setCameraFraming(command.getCameraFraming());
        }
        if (command.getLightingNotes() != null) {
            scene.setLightingNotes(command.getLightingNotes());
        }
        if (command.getColorPalette() != null) {
            scene.setColorPalette(command.getColorPalette());
        }
        if (command.getMood() != null) {
            scene.setMood(command.getMood());
        }
        if (command.getLocation() != null) {
            scene.setLocation(command.getLocation());
        }
        if (command.getCharacters() != null) {
            scene.setCharacters(command.getCharacters());
        }
        if (command.getCulturalDetails() != null) {
            scene.setCulturalDetails(command.getCulturalDetails());
        }
        if (command.getTextOverlayGuidance() != null) {
            scene.setTextOverlayGuidance(command.getTextOverlayGuidance());
        }
        if (command.getImageGenerationToolNotes() != null) {
            scene.setImageGenerationToolNotes(command.getImageGenerationToolNotes());
        }
        if (command.getSafetyNotes() != null) {
            scene.setSafetyNotes(command.getSafetyNotes());
        }
    }

    private void applyThumbnailEdits(ThumbnailPlan thumb, UpdateThumbnailPlanCommand command) {
        if (command.getThumbnailPrompt() != null) {
            thumb.setThumbnailPrompt(command.getThumbnailPrompt());
        }
        if (command.getNegativePrompt() != null) {
            thumb.setNegativePrompt(command.getNegativePrompt());
        }
        if (command.getTextOverlayGuidance() != null) {
            thumb.setTextOverlayGuidance(command.getTextOverlayGuidance());
        }
        if (command.getCompositionNotes() != null) {
            thumb.setCompositionNotes(command.getCompositionNotes());
        }
        if (command.getEmotionalHook() != null) {
            thumb.setEmotionalHook(command.getEmotionalHook());
        }
        if (command.getAspectRatio() != null) {
            thumb.setAspectRatio(command.getAspectRatio());
        }
        if (command.getStyleDirective() != null) {
            thumb.setStyleDirective(command.getStyleDirective());
        }
        if (command.getColorPalette() != null) {
            thumb.setColorPalette(command.getColorPalette());
        }
    }

    // ── Common Helpers ──────────────────────────────────────

    private void validateEditable(VisualAssetPlan plan) {
        if (!EDITABLE_STATUSES.contains(plan.getStatus())) {
            throw new BusinessException(
                    String.format("Görsel varlık planı düzenlenemez. Mevcut statü: %s, İzin verilen: %s",
                            plan.getStatus(), EDITABLE_STATUSES));
        }
    }

    private VisualAssetPlan findPlanOrThrow(UUID planId) {
        VisualAssetPlan plan = visualAssetPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("VisualAssetPlan", planId));
        // Force lazy load children
        plan.getScenePlans().size();
        if (plan.getThumbnailPlan() != null) {
            plan.getThumbnailPlan().getId();
        }
        return plan;
    }

    private VisualAssetPlanReviewResult buildResult(VisualAssetPlan plan, String message) {
        return VisualAssetPlanReviewResult.builder()
                .success(true)
                .visualAssetPlanId(plan.getId())
                .contentProjectId(plan.getContentProject().getId())
                .storyboardDraftId(plan.getSourceStoryboardDraft().getId())
                .status(plan.getStatus().name())
                .planTitle(plan.getTitle())
                .message(message)
                .build();
    }
}
