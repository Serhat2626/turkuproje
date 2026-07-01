package com.anadoluatlasi.backend.application.scenario;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeat;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.scenario.repository.ScenarioDraftRepository;
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
 * FP-002: Senaryo taslağı review iş akışını yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>editScenarioDraft — Senaryo içeriğini ve beat'lerini düzenle</li>
 *   <li>approveScenarioDraft — Explicit admin onayı</li>
 *   <li>rejectScenarioDraft — Explicit admin reddi</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>AI gateway çağırmaz</li>
 *   <li>Otomatik onay/red yapmaz</li>
 *   <li>ContentProject status'ünü değiştirmez</li>
 *   <li>READY_FOR_PUBLISH ayarlamaz</li>
 *   <li>Raw prompt/payload/secret saklamaz</li>
 *   <li>Generic status update davranışı sunmaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioReviewService {

    private static final int REQUIRED_BEAT_COUNT = 6;
    private static final Set<ScenarioDraftStatus> EDITABLE_STATUSES =
            Set.of(ScenarioDraftStatus.AI_GENERATED, ScenarioDraftStatus.NEEDS_REVIEW);
    private static final Set<ScenarioDraftStatus> APPROVABLE_STATUSES =
            Set.of(ScenarioDraftStatus.AI_GENERATED, ScenarioDraftStatus.NEEDS_REVIEW);
    private static final Set<ScenarioDraftStatus> REJECTABLE_STATUSES =
            Set.of(ScenarioDraftStatus.AI_GENERATED, ScenarioDraftStatus.NEEDS_REVIEW);

    private final ScenarioDraftRepository scenarioDraftRepository;

    // ── Edit Scenario ───────────────────────────────────────

    /**
     * Senaryo taslağını düzenler.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki senaryolar düzenlenebilir</li>
     *   <li>AI_GENERATED düzenlendiğinde NEEDS_REVIEW'a geçer</li>
     *   <li>NEEDS_REVIEW düzenlendiğinde NEEDS_REVIEW kalır</li>
     *   <li>Düzenleme asla APPROVED yapmaz</li>
     *   <li>editedAt ve editedBy güncellenir</li>
     * </ul>
     *
     * @param scenarioDraftId düzenlenecek senaryo
     * @param command düzenleme komut nesnesi
     * @param adminId düzenleyen admin
     * @return düzenleme sonucu
     */
    @Transactional
    public ScenarioReviewResult editScenarioDraft(UUID scenarioDraftId,
                                                   ScenarioDraftEditCommand command,
                                                   String adminId) {
        ScenarioDraft draft = findDraftOrThrow(scenarioDraftId);

        // Status kontrolü
        if (!EDITABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Senaryo düzenlenemez. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), EDITABLE_STATUSES));
        }

        // İçerik alanlarını güncelle
        applyContentEdits(draft, command);

        // Beat düzenlemeleri (varsa)
        if (command.getBeats() != null && !command.getBeats().isEmpty()) {
            applyBeatEdits(draft, command.getBeats());
        }

        // Lifecycle: AI_GENERATED → NEEDS_REVIEW, NEEDS_REVIEW → NEEDS_REVIEW
        draft.setStatus(ScenarioDraftStatus.NEEDS_REVIEW);

        // Audit metadata
        draft.setEditedAt(LocalDateTime.now());
        draft.setEditedBy(adminId);

        draft = scenarioDraftRepository.save(draft);

        log.info("Senaryo düzenlendi: scenarioDraftId={}, status={}, editedBy={}",
                draft.getId(), draft.getStatus(), adminId);

        return buildResult(draft, "Senaryo başarıyla düzenlendi");
    }

    // ── Approve Scenario ────────────────────────────────────

    /**
     * Senaryo taslağını explicit admin eylemiyle onaylar.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki senaryolar onaylanabilir</li>
     *   <li>Onay öncesi içerik validasyonu yapılır (title, 6 beat, body non-empty)</li>
     *   <li>Aynı ContentProject'te zaten APPROVED senaryo varsa onay reddedilir</li>
     *   <li>approvedAt ve approvedBy güncellenir</li>
     *   <li>ContentProject status'ü değiştirilmez</li>
     *   <li>READY_FOR_PUBLISH ayarlanmaz</li>
     * </ul>
     *
     * @param scenarioDraftId onaylanacak senaryo
     * @param adminId onaylayan admin
     * @return onay sonucu
     */
    @Transactional
    public ScenarioReviewResult approveScenarioDraft(UUID scenarioDraftId, String adminId) {
        ScenarioDraft draft = findDraftOrThrow(scenarioDraftId);

        // Status kontrolü
        if (!APPROVABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Senaryo onaylanamaz. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), APPROVABLE_STATUSES));
        }

        // İçerik validasyonu
        List<String> validationErrors = validateForApproval(draft);
        if (!validationErrors.isEmpty()) {
            throw new BusinessException(
                    "Senaryo onay validasyonu başarısız: " + String.join(", ", validationErrors));
        }

        // Aynı project'te zaten approved senaryo var mı?
        UUID projectId = draft.getContentProject().getId();
        List<ScenarioDraft> existingApproved = scenarioDraftRepository
                .findByContentProjectIdAndStatus(projectId, ScenarioDraftStatus.APPROVED);
        if (!existingApproved.isEmpty()) {
            throw new BusinessException(
                    String.format("Bu proje için zaten onaylanmış bir senaryo mevcut (v%d). "
                                    + "Yeni senaryo onaylamak için önce mevcut onayı kaldırın. Proje ID: %s",
                            existingApproved.get(0).getVersion(), projectId));
        }

        // Approve
        draft.setStatus(ScenarioDraftStatus.APPROVED);
        draft.setApprovedAt(LocalDateTime.now());
        draft.setApprovedBy(adminId);

        draft = scenarioDraftRepository.save(draft);

        log.info("Senaryo onaylandı: scenarioDraftId={}, version={}, approvedBy={}",
                draft.getId(), draft.getVersion(), adminId);

        return buildResult(draft, "Senaryo başarıyla onaylandı");
    }

    // ── Reject Scenario ─────────────────────────────────────

    /**
     * Senaryo taslağını explicit admin eylemiyle reddeder.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>Sadece AI_GENERATED veya NEEDS_REVIEW status'teki senaryolar reddedilebilir</li>
     *   <li>rejectedAt, rejectedBy ve reviewNotes güncellenir</li>
     *   <li>Source ContentDraft status'ü değiştirilmez</li>
     *   <li>ContentProject status'ü değiştirilmez</li>
     *   <li>Senaryo silinmez — audit trail korunur</li>
     * </ul>
     *
     * @param scenarioDraftId reddedilecek senaryo
     * @param adminId reddeden admin
     * @param rejectionReason red sebebi (sanitized olarak saklanır)
     * @return red sonucu
     */
    @Transactional
    public ScenarioReviewResult rejectScenarioDraft(UUID scenarioDraftId,
                                                     String adminId,
                                                     String rejectionReason) {
        ScenarioDraft draft = findDraftOrThrow(scenarioDraftId);

        // Status kontrolü
        if (!REJECTABLE_STATUSES.contains(draft.getStatus())) {
            throw new BusinessException(
                    String.format("Senaryo reddedilemez. Mevcut statü: %s, İzin verilen: %s",
                            draft.getStatus(), REJECTABLE_STATUSES));
        }

        // Reject
        draft.setStatus(ScenarioDraftStatus.REJECTED);
        draft.setRejectedAt(LocalDateTime.now());
        draft.setRejectedBy(adminId);

        // Red sebebini reviewNotes'a yaz
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            draft.setReviewNotes(rejectionReason.trim());
        }

        draft = scenarioDraftRepository.save(draft);

        log.info("Senaryo reddedildi: scenarioDraftId={}, version={}, rejectedBy={}",
                draft.getId(), draft.getVersion(), adminId);

        return buildResult(draft, "Senaryo reddedildi");
    }

    // ── Content Edit Helpers ────────────────────────────────

    private void applyContentEdits(ScenarioDraft draft, ScenarioDraftEditCommand command) {
        if (command.getTitle() != null) {
            draft.setTitle(command.getTitle());
        }
        if (command.getSummary() != null) {
            draft.setSummary(command.getSummary());
        }
        if (command.getTargetAudience() != null) {
            draft.setTargetAudience(command.getTargetAudience());
        }
        if (command.getCulturalObjective() != null) {
            draft.setCulturalObjective(command.getCulturalObjective());
        }
        if (command.getTone() != null) {
            draft.setTone(command.getTone());
        }
        if (command.getEstimatedDurationSeconds() != null) {
            draft.setEstimatedDurationSeconds(command.getEstimatedDurationSeconds());
        }
        if (command.getSetting() != null) {
            draft.setSetting(command.getSetting());
        }
        if (command.getCharacters() != null) {
            draft.setCharacters(command.getCharacters());
        }
        if (command.getReviewNotes() != null) {
            draft.setReviewNotes(command.getReviewNotes());
        }
    }

    private void applyBeatEdits(ScenarioDraft draft, List<ScenarioBeatEditCommand> beatCommands) {
        // Mevcut beat'leri type'a göre index'le
        Map<ScenarioBeatType, ScenarioBeat> existingBeats = draft.getBeats().stream()
                .collect(Collectors.toMap(ScenarioBeat::getType, Function.identity()));

        for (ScenarioBeatEditCommand cmd : beatCommands) {
            ScenarioBeat beat = existingBeats.get(cmd.getType());
            if (beat == null) {
                log.warn("Beat tipi bulunamadı: type={}, scenarioDraftId={}",
                        cmd.getType(), draft.getId());
                continue;
            }
            if (cmd.getLabel() != null) {
                beat.setLabel(cmd.getLabel());
            }
            if (cmd.getBody() != null) {
                beat.setBody(cmd.getBody());
            }
            if (cmd.getOrderIndex() != null) {
                beat.setOrderIndex(cmd.getOrderIndex());
            }
            if (cmd.getEstimatedDurationSeconds() != null) {
                beat.setEstimatedDurationSeconds(cmd.getEstimatedDurationSeconds());
            }
        }
    }

    // ── Approval Validation ─────────────────────────────────

    private List<String> validateForApproval(ScenarioDraft draft) {
        List<String> errors = new ArrayList<>();

        // Title zorunlu
        if (draft.getTitle() == null || draft.getTitle().isBlank()) {
            errors.add("TITLE_REQUIRED");
        }

        // 6 beat zorunlu
        if (draft.getBeats() == null || draft.getBeats().isEmpty()) {
            errors.add("BEATS_REQUIRED");
            return errors;
        }

        if (draft.getBeats().size() != REQUIRED_BEAT_COUNT) {
            errors.add("BEAT_COUNT_MUST_BE_" + REQUIRED_BEAT_COUNT);
        }

        // Her required beat tipi mevcut mu
        Set<ScenarioBeatType> presentTypes = draft.getBeats().stream()
                .map(ScenarioBeat::getType)
                .collect(Collectors.toSet());

        for (ScenarioBeatType requiredType : ScenarioBeatType.values()) {
            if (!presentTypes.contains(requiredType)) {
                errors.add("MISSING_BEAT_" + requiredType.name());
            }
        }

        // Beat body'leri boş mu
        for (ScenarioBeat beat : draft.getBeats()) {
            if (beat.getBody() == null || beat.getBody().isBlank()) {
                errors.add("EMPTY_BEAT_BODY_" + beat.getType().name());
            }
        }

        return errors;
    }

    // ── Common Helpers ──────────────────────────────────────

    private ScenarioDraft findDraftOrThrow(UUID id) {
        return scenarioDraftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ScenarioDraft", id));
    }

    private ScenarioReviewResult buildResult(ScenarioDraft draft, String message) {
        return ScenarioReviewResult.builder()
                .success(true)
                .scenarioDraftId(draft.getId())
                .contentProjectId(draft.getContentProject().getId())
                .status(draft.getStatus().name())
                .scenarioTitle(draft.getTitle())
                .message(message)
                .build();
    }
}
