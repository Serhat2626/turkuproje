package com.anadoluatlasi.backend.application.storyboard;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import com.anadoluatlasi.backend.storyboard.repository.StoryboardDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FP-003: Storyboard taslağı review iş akışını yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>editStoryboardDraft — Storyboard içeriğini ve scene'lerini düzenle</li>
 *   <li>approveStoryboardDraft — Explicit admin onayı</li>
 *   <li>rejectStoryboardDraft — Explicit admin reddi</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>AI gateway çağırmaz</li>
 *   <li>Otomatik onay/red yapmaz</li>
 *   <li>ContentProject status'ünü değiştirmez</li>
 *   <li>READY_FOR_PUBLISH ayarlamaz</li>
 *   <li>Raw prompt/payload/secret saklamaz</li>
 *   <li>Media dosyası (image/video/audio) üretmez</li>
 *   <li>Generic status update davranışı sunmaz</li>
 *   <li>Yeni storyboard version oluşturmaz (edit mevcut draft'ı günceller)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardReviewService {

    private static final Set<StoryboardDraftStatus> EDITABLE_STATUSES =
            Set.of(StoryboardDraftStatus.AI_GENERATED, StoryboardDraftStatus.NEEDS_REVIEW);
    private static final Set<StoryboardDraftStatus> APPROVABLE_STATUSES =
            Set.of(StoryboardDraftStatus.AI_GENERATED, StoryboardDraftStatus.NEEDS_REVIEW);
    private static final Set<StoryboardDraftStatus> REJECTABLE_STATUSES =
            Set.of(StoryboardDraftStatus.AI_GENERATED, StoryboardDraftStatus.NEEDS_REVIEW);

    private final StoryboardDraftRepository storyboardDraftRepository;

    // ── Edit Storyboard ─────────────────────────────────────

    /**
     * Storyboard taslağını düzenler.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki storyboard'lar düzenlenebilir</li>
     *   <li>AI_GENERATED düzenlendiğinde NEEDS_REVIEW'a geçer</li>
     *   <li>NEEDS_REVIEW düzenlendiğinde NEEDS_REVIEW kalır</li>
     *   <li>APPROVED, REJECTED, FAILED storyboard'lar düzenlenemez</li>
     *   <li>Düzenleme asla APPROVED yapmaz</li>
     *   <li>editedAt ve editedBy güncellenir</li>
     *   <li>AI gateway çağrılmaz</li>
     *   <li>Yeni version oluşturulmaz</li>
     * </ul>
     *
     * @param storyboardDraftId düzenlenecek storyboard
     * @param command düzenleme komut nesnesi
     * @param adminId düzenleyen admin
     * @return düzenleme sonucu
     */
    @Transactional
    public StoryboardReviewResult editStoryboardDraft(UUID storyboardDraftId,
                                                       StoryboardDraftEditCommand command,
                                                       String adminId) {
        StoryboardDraft draft = findDraftOrThrow(storyboardDraftId);

        // Status kontrolü
        if (!EDITABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Storyboard düzenlenemez. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), EDITABLE_STATUSES));
        }

        // İçerik alanlarını güncelle
        applyContentEdits(draft, command);

        // Scene düzenlemeleri (varsa)
        if (command.getScenes() != null && !command.getScenes().isEmpty()) {
            applySceneEdits(draft, command.getScenes());
        }

        // Lifecycle: AI_GENERATED → NEEDS_REVIEW, NEEDS_REVIEW → NEEDS_REVIEW
        draft.setStatus(StoryboardDraftStatus.NEEDS_REVIEW);

        // Audit metadata
        draft.setEditedAt(LocalDateTime.now());
        draft.setEditedBy(adminId);

        draft = storyboardDraftRepository.save(draft);

        log.info("Storyboard düzenlendi: storyboardDraftId={}, status={}, editedBy={}",
                draft.getId(), draft.getStatus(), adminId);

        return buildResult(draft, "Storyboard başarıyla düzenlendi");
    }

    // ── Approve Storyboard ──────────────────────────────────

    /**
     * Storyboard taslağını explicit admin eylemiyle onaylar.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki storyboard'lar onaylanabilir</li>
     *   <li>Onay öncesi içerik validasyonu yapılır (title, en az 1 scene, visualDescription)</li>
     *   <li>Aynı ContentProject'te zaten APPROVED storyboard varsa onay reddedilir</li>
     *   <li>approvedAt ve approvedBy güncellenir</li>
     *   <li>ContentProject status'ü değiştirilmez</li>
     *   <li>READY_FOR_PUBLISH ayarlanmaz</li>
     *   <li>Eski approved storyboard otomatik unapprove edilmez</li>
     * </ul>
     *
     * @param storyboardDraftId onaylanacak storyboard
     * @param adminId onaylayan admin
     * @return onay sonucu
     */
    @Transactional
    public StoryboardReviewResult approveStoryboardDraft(UUID storyboardDraftId, String adminId) {
        StoryboardDraft draft = findDraftOrThrow(storyboardDraftId);

        // Status kontrolü
        if (!APPROVABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Storyboard onaylanamaz. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), APPROVABLE_STATUSES));
        }

        // İçerik validasyonu
        List<String> validationErrors = validateForApproval(draft);
        if (!validationErrors.isEmpty()) {
            throw new BusinessException(
                    "Storyboard onay validasyonu başarısız: " + String.join(", ", validationErrors));
        }

        // Aynı project'te zaten approved storyboard var mı?
        UUID projectId = draft.getContentProject().getId();
        List<StoryboardDraft> existingApproved = storyboardDraftRepository
                .findByContentProjectIdAndStatus(projectId, StoryboardDraftStatus.APPROVED);
        if (!existingApproved.isEmpty()) {
            throw new BusinessException(
                    String.format("Bu proje için zaten onaylanmış bir storyboard mevcut (v%d). "
                                    + "Yeni storyboard onaylamak için önce mevcut onayı kaldırın. Proje ID: %s",
                            existingApproved.get(0).getVersion(), projectId));
        }

        // Approve
        draft.setStatus(StoryboardDraftStatus.APPROVED);
        draft.setApprovedAt(LocalDateTime.now());
        draft.setApprovedBy(adminId);

        draft = storyboardDraftRepository.save(draft);

        log.info("Storyboard onaylandı: storyboardDraftId={}, version={}, approvedBy={}",
                draft.getId(), draft.getVersion(), adminId);

        return buildResult(draft, "Storyboard başarıyla onaylandı");
    }

    // ── Reject Storyboard ───────────────────────────────────

    /**
     * Storyboard taslağını explicit admin eylemiyle reddeder.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki storyboard'lar reddedilebilir</li>
     *   <li>rejectedAt, rejectedBy ve reviewNotes güncellenir</li>
     *   <li>Source ScenarioDraft status'ü değiştirilmez</li>
     *   <li>ContentProject status'ü değiştirilmez</li>
     *   <li>Storyboard silinmez — audit trail korunur</li>
     * </ul>
     *
     * @param storyboardDraftId reddedilecek storyboard
     * @param adminId reddeden admin
     * @param rejectionReason red sebebi (sanitized olarak saklanır)
     * @return red sonucu
     */
    @Transactional
    public StoryboardReviewResult rejectStoryboardDraft(UUID storyboardDraftId,
                                                         String adminId,
                                                         String rejectionReason) {
        StoryboardDraft draft = findDraftOrThrow(storyboardDraftId);

        // Status kontrolü
        if (!REJECTABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Storyboard reddedilemez. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), REJECTABLE_STATUSES));
        }

        // Reject
        draft.setStatus(StoryboardDraftStatus.REJECTED);
        draft.setRejectedAt(LocalDateTime.now());
        draft.setRejectedBy(adminId);

        // Red sebebini reviewNotes'a yaz
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            draft.setReviewNotes(rejectionReason.trim());
        }

        draft = storyboardDraftRepository.save(draft);

        log.info("Storyboard reddedildi: storyboardDraftId={}, version={}, rejectedBy={}",
                draft.getId(), draft.getVersion(), adminId);

        return buildResult(draft, "Storyboard reddedildi");
    }

    // ── Content Edit Helpers ────────────────────────────────

    private void applyContentEdits(StoryboardDraft draft, StoryboardDraftEditCommand command) {
        if (command.getTitle() != null) {
            draft.setTitle(command.getTitle());
        }
        if (command.getSummary() != null) {
            draft.setSummary(command.getSummary());
        }
        if (command.getTargetPlatform() != null) {
            draft.setTargetPlatform(command.getTargetPlatform());
        }
        if (command.getAspectRatio() != null) {
            draft.setAspectRatio(command.getAspectRatio());
        }
        if (command.getEstimatedDurationSeconds() != null) {
            draft.setEstimatedDurationSeconds(command.getEstimatedDurationSeconds());
        }
        if (command.getVisualStyleGuide() != null) {
            draft.setVisualStyleGuide(command.getVisualStyleGuide());
        }
        if (command.getNarrationStyle() != null) {
            draft.setNarrationStyle(command.getNarrationStyle());
        }
        if (command.getReviewNotes() != null) {
            draft.setReviewNotes(command.getReviewNotes());
        }
    }

    private void applySceneEdits(StoryboardDraft draft, List<StoryboardSceneEditCommand> sceneCommands) {
        // Mevcut scene'leri sceneNumber'a göre index'le
        Map<Integer, StoryboardScene> existingScenes = draft.getScenes().stream()
                .collect(Collectors.toMap(StoryboardScene::getSceneNumber, Function.identity()));

        for (StoryboardSceneEditCommand cmd : sceneCommands) {
            StoryboardScene scene = existingScenes.get(cmd.getSceneNumber());
            if (scene == null) {
                log.warn("Scene numarası bulunamadı: sceneNumber={}, storyboardDraftId={}",
                        cmd.getSceneNumber(), draft.getId());
                continue;
            }
            if (cmd.getSceneTitle() != null) {
                scene.setSceneTitle(cmd.getSceneTitle());
            }
            if (cmd.getVisualDescription() != null) {
                scene.setVisualDescription(cmd.getVisualDescription());
            }
            if (cmd.getNarrationText() != null) {
                scene.setNarrationText(cmd.getNarrationText());
            }
            if (cmd.getOnScreenText() != null) {
                scene.setOnScreenText(cmd.getOnScreenText());
            }
            if (cmd.getCameraDirection() != null) {
                scene.setCameraDirection(cmd.getCameraDirection());
            }
            if (cmd.getTransitionNote() != null) {
                scene.setTransitionNote(cmd.getTransitionNote());
            }
            if (cmd.getMood() != null) {
                scene.setMood(cmd.getMood());
            }
            if (cmd.getLocation() != null) {
                scene.setLocation(cmd.getLocation());
            }
            if (cmd.getCharacters() != null) {
                scene.setCharacters(cmd.getCharacters());
            }
            if (cmd.getEstimatedDurationSeconds() != null) {
                scene.setEstimatedDurationSeconds(cmd.getEstimatedDurationSeconds());
            }
            if (cmd.getMediaGenerationNotes() != null) {
                scene.setMediaGenerationNotes(cmd.getMediaGenerationNotes());
            }
        }
    }

    // ── Approval Validation ─────────────────────────────────

    private List<String> validateForApproval(StoryboardDraft draft) {
        List<String> errors = new ArrayList<>();

        // Title zorunlu
        if (draft.getTitle() == null || draft.getTitle().isBlank()) {
            errors.add("TITLE_REQUIRED");
        }

        // En az 1 scene zorunlu
        if (draft.getScenes() == null || draft.getScenes().isEmpty()) {
            errors.add("SCENES_REQUIRED");
            return errors;
        }

        // Her scene'de zorunlu alanlar
        for (StoryboardScene scene : draft.getScenes()) {
            // Scene title
            if (scene.getSceneTitle() == null || scene.getSceneTitle().isBlank()) {
                errors.add("EMPTY_SCENE_TITLE_" + scene.getSceneNumber());
            }
            // Visual description zorunlu
            if (scene.getVisualDescription() == null || scene.getVisualDescription().isBlank()) {
                errors.add("EMPTY_VISUAL_DESCRIPTION_" + scene.getSceneNumber());
            }
        }

        return errors;
    }

    // ── Common Helpers ──────────────────────────────────────

    private StoryboardDraft findDraftOrThrow(UUID id) {
        return storyboardDraftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("StoryboardDraft", id));
    }

    private StoryboardReviewResult buildResult(StoryboardDraft draft, String message) {
        return StoryboardReviewResult.builder()
                .success(true)
                .storyboardDraftId(draft.getId())
                .contentProjectId(draft.getContentProject().getId())
                .status(draft.getStatus().name())
                .storyboardTitle(draft.getTitle())
                .message(message)
                .build();
    }
}
