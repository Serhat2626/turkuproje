package com.anadoluatlasi.backend.application.platformpackage;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageSceneUsage;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageStatus;
import com.anadoluatlasi.backend.platformpackage.repository.PlatformPackageDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FP-004: Platform paketi taslağı review iş akışını yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>editPlatformPackageDraft — Paket içeriğini düzenle</li>
 *   <li>approvePlatformPackageDraft — Explicit admin onayı</li>
 *   <li>rejectPlatformPackageDraft — Explicit admin reddi</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>AI gateway çağırmaz</li>
 *   <li>Otomatik onay/red yapmaz</li>
 *   <li>READY_FOR_PUBLISH ayarlamaz</li>
 *   <li>Raw prompt/payload/secret saklamaz</li>
 *   <li>Media dosyası üretmez</li>
 *   <li>Platform'a yayın yapmaz</li>
 *   <li>Generic status update davranışı sunmaz</li>
 *   <li>Yeni version oluşturmaz (edit mevcut draft'ı günceller)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformPackageReviewService {

    private static final Set<PlatformPackageStatus> EDITABLE_STATUSES =
            Set.of(PlatformPackageStatus.AI_GENERATED, PlatformPackageStatus.NEEDS_REVIEW);
    private static final Set<PlatformPackageStatus> APPROVABLE_STATUSES =
            Set.of(PlatformPackageStatus.AI_GENERATED, PlatformPackageStatus.NEEDS_REVIEW);
    private static final Set<PlatformPackageStatus> REJECTABLE_STATUSES =
            Set.of(PlatformPackageStatus.AI_GENERATED, PlatformPackageStatus.NEEDS_REVIEW);

    private final PlatformPackageDraftRepository platformPackageDraftRepository;

    // ── Edit Platform Package ──────────────────────────────

    /**
     * Platform paketi taslağını düzenler.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki paketler düzenlenebilir</li>
     *   <li>AI_GENERATED düzenlendiğinde NEEDS_REVIEW'a geçer</li>
     *   <li>NEEDS_REVIEW düzenlendiğinde NEEDS_REVIEW kalır</li>
     *   <li>APPROVED, REJECTED, FAILED paketler düzenlenemez</li>
     *   <li>Düzenleme asla APPROVED yapmaz</li>
     *   <li>editedAt ve editedBy güncellenir</li>
     *   <li>AI gateway çağrılmaz</li>
     *   <li>Yeni version oluşturulmaz</li>
     * </ul>
     */
    @Transactional
    public PlatformPackageReviewResult editPlatformPackageDraft(
            UUID packageDraftId,
            PlatformPackageDraftEditCommand command,
            String adminId) {

        PlatformPackageDraft draft = findDraftOrThrow(packageDraftId);

        // Status kontrolü
        if (!EDITABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Platform paketi düzenlenemez. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), EDITABLE_STATUSES));
        }

        // İçerik alanlarını güncelle
        applyContentEdits(draft, command);

        // Scene usage düzenlemeleri
        if (command.getSceneUsages() != null && !command.getSceneUsages().isEmpty()) {
            applySceneUsageEdits(draft, command);
        }

        // Lifecycle: AI_GENERATED → NEEDS_REVIEW, NEEDS_REVIEW → NEEDS_REVIEW
        draft.setStatus(PlatformPackageStatus.NEEDS_REVIEW);

        // Audit metadata
        draft.setEditedAt(LocalDateTime.now());
        draft.setEditedBy(adminId);

        draft = platformPackageDraftRepository.save(draft);

        log.info("Platform paketi düzenlendi: packageDraftId={}, packageType={}, status={}, editedBy={}",
                draft.getId(), draft.getPackageType(), draft.getStatus(), adminId);

        return buildResult(draft, "Platform paketi başarıyla düzenlendi");
    }

    // ── Approve Platform Package ───────────────────────────

    /**
     * Platform paketi taslağını explicit admin eylemiyle onaylar.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki paketler onaylanabilir</li>
     *   <li>Aynı contentProject + packageType için zaten APPROVED paket varsa onay reddedilir</li>
     *   <li>approvedAt ve approvedBy güncellenir</li>
     *   <li>READY_FOR_PUBLISH ayarlanmaz</li>
     *   <li>Eski approved paket otomatik unapprove edilmez</li>
     * </ul>
     */
    @Transactional
    public PlatformPackageReviewResult approvePlatformPackageDraft(UUID packageDraftId, String adminId) {
        PlatformPackageDraft draft = findDraftOrThrow(packageDraftId);

        // Status kontrolü
        if (!APPROVABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Platform paketi onaylanamaz. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), APPROVABLE_STATUSES));
        }

        // One-approved-per-platform kuralı
        UUID projectId = draft.getContentProject().getId();
        long approvedCount = platformPackageDraftRepository.countByProjectAndTypeAndStatus(
                projectId, draft.getPackageType(), PlatformPackageStatus.APPROVED);
        if (approvedCount > 0) {
            throw new BusinessException(
                    String.format("Bu proje için %s tipinde zaten onaylanmış bir paket mevcut. "
                                    + "Yeni paket onaylamak için önce mevcut onayı kaldırın. "
                                    + "Proje ID: %s, Platform: %s",
                            draft.getPackageType(), projectId, draft.getPackageType()));
        }

        // Approve
        draft.setStatus(PlatformPackageStatus.APPROVED);
        draft.setApprovedAt(LocalDateTime.now());
        draft.setApprovedBy(adminId);

        draft = platformPackageDraftRepository.save(draft);

        log.info("Platform paketi onaylandı: packageDraftId={}, packageType={}, version={}, approvedBy={}",
                draft.getId(), draft.getPackageType(), draft.getVersion(), adminId);

        return buildResult(draft, "Platform paketi başarıyla onaylandı");
    }

    // ── Reject Platform Package ────────────────────────────

    /**
     * Platform paketi taslağını explicit admin eylemiyle reddeder.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki paketler reddedilebilir</li>
     *   <li>rejectedAt, rejectedBy ve rejectionReason güncellenir</li>
     *   <li>Paket silinmez — audit trail korunur</li>
     * </ul>
     */
    @Transactional
    public PlatformPackageReviewResult rejectPlatformPackageDraft(
            UUID packageDraftId, String adminId, String rejectionReason) {

        PlatformPackageDraft draft = findDraftOrThrow(packageDraftId);

        // Status kontrolü
        if (!REJECTABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Platform paketi reddedilemez. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), REJECTABLE_STATUSES));
        }

        // Reject
        draft.setStatus(PlatformPackageStatus.REJECTED);
        draft.setRejectedAt(LocalDateTime.now());
        draft.setRejectedBy(adminId);

        // Red gerekçesi
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            draft.setRejectionReason(rejectionReason.trim());
        }

        draft = platformPackageDraftRepository.save(draft);

        log.info("Platform paketi reddedildi: packageDraftId={}, packageType={}, version={}, rejectedBy={}",
                draft.getId(), draft.getPackageType(), draft.getVersion(), adminId);

        return buildResult(draft, "Platform paketi reddedildi");
    }

    // ── Content Edit Helpers ───────────────────────────────

    private void applyContentEdits(PlatformPackageDraft draft, PlatformPackageDraftEditCommand command) {
        // Common fields
        if (command.getTitle() != null) draft.setTitle(command.getTitle());
        if (command.getCaptionOrDescription() != null) draft.setCaptionOrDescription(command.getCaptionOrDescription());
        if (command.getHashtagsJson() != null) draft.setHashtagsJson(command.getHashtagsJson());
        if (command.getCallToAction() != null) draft.setCallToAction(command.getCallToAction());
        if (command.getTargetDurationSeconds() != null) draft.setTargetDurationSeconds(command.getTargetDurationSeconds());
        if (command.getAspectRatio() != null) draft.setAspectRatio(command.getAspectRatio());
        if (command.getContentStructureNotes() != null) draft.setContentStructureNotes(command.getContentStructureNotes());
        if (command.getSceneUsageNotes() != null) draft.setSceneUsageNotes(command.getSceneUsageNotes());
        if (command.getReviewNotes() != null) draft.setReviewNotes(command.getReviewNotes());

        // YouTube Long-form
        if (command.getVideoTitle() != null) draft.setVideoTitle(command.getVideoTitle());
        if (command.getLongDescription() != null) draft.setLongDescription(command.getLongDescription());
        if (command.getChapterOutlineJson() != null) draft.setChapterOutlineJson(command.getChapterOutlineJson());
        if (command.getPinnedCommentSuggestion() != null) draft.setPinnedCommentSuggestion(command.getPinnedCommentSuggestion());
        if (command.getThumbnailTextSuggestion() != null) draft.setThumbnailTextSuggestion(command.getThumbnailTextSuggestion());

        // Short-form / Reels
        if (command.getShortHook() != null) draft.setShortHook(command.getShortHook());
        if (command.getShortCaption() != null) draft.setShortCaption(command.getShortCaption());
        if (command.getOverlayTextSuggestionsJson() != null) draft.setOverlayTextSuggestionsJson(command.getOverlayTextSuggestionsJson());
        if (command.getShortHashtagSetJson() != null) draft.setShortHashtagSetJson(command.getShortHashtagSetJson());
        if (command.getShortCta() != null) draft.setShortCta(command.getShortCta());
        if (command.getDurationGuidance() != null) draft.setDurationGuidance(command.getDurationGuidance());
    }

    private void applySceneUsageEdits(PlatformPackageDraft draft, PlatformPackageDraftEditCommand command) {
        Map<Integer, PlatformPackageSceneUsage> existingUsages = draft.getSceneUsages().stream()
                .collect(Collectors.toMap(PlatformPackageSceneUsage::getSceneNumber, Function.identity()));

        for (PlatformPackageSceneUsageEditCommand cmd : command.getSceneUsages()) {
            PlatformPackageSceneUsage usage = existingUsages.get(cmd.getSceneNumber());
            if (usage == null) {
                log.warn("Scene usage numarası bulunamadı: sceneNumber={}, packageDraftId={}",
                        cmd.getSceneNumber(), draft.getId());
                continue;
            }
            if (cmd.getUsageType() != null) usage.setUsageType(cmd.getUsageType());
            if (cmd.getIncluded() != null) usage.setIncluded(cmd.getIncluded());
            if (cmd.getAdaptationNote() != null) usage.setAdaptationNote(cmd.getAdaptationNote());
        }
    }

    // ── Common Helpers ──────────────────────────────────────

    private PlatformPackageDraft findDraftOrThrow(UUID id) {
        return platformPackageDraftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PlatformPackageDraft", id));
    }

    private PlatformPackageReviewResult buildResult(PlatformPackageDraft draft, String message) {
        return PlatformPackageReviewResult.builder()
                .success(true)
                .packageDraftId(draft.getId())
                .contentProjectId(draft.getContentProject().getId())
                .status(draft.getStatus().name())
                .packageType(draft.getPackageType().name())
                .title(draft.getTitle())
                .message(message)
                .build();
    }
}
