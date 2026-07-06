package com.anadoluatlasi.backend.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-014 Task 02: Internal service that derives approval readiness from
 * existing review records and safe preview context.
 *
 * <p>This service is read-only — it does NOT mutate review state,
 * approve/reject targets, create audit records, call external providers,
 * inspect storage, generate public/signed URLs, or trigger delivery.</p>
 *
 * <p>Critical rules:</p>
 * <ul>
 *   <li>Never exposes provider URLs, storage keys, raw payloads, base64</li>
 *   <li>Never creates public or signed URLs</li>
 *   <li>Never implies publication — readiness is internal-only</li>
 *   <li>Never approves, rejects, or changes review state</li>
 *   <li>Never publishes or exports</li>
 *   <li>APPROVED_FOR_INTERNAL_USE readiness is NOT publish/export eligibility</li>
 * </ul>
 */
@Service
public class InternalApprovalReadinessService {

    private static final Logger log = LoggerFactory.getLogger(InternalApprovalReadinessService.class);

    private final ReviewRecordRepository reviewRecordRepository;
    private final AdminPreviewService adminPreviewService;

    public InternalApprovalReadinessService(ReviewRecordRepository reviewRecordRepository,
                                            AdminPreviewService adminPreviewService) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.adminPreviewService = adminPreviewService;
    }

    /**
     * Get internal approval readiness for a review target.
     *
     * <p>Read-only derivation. Does not call storage/provider.
     * Does not create URLs. Does not mutate state. Does not approve.</p>
     *
     * @param targetType review target type (must not be null)
     * @param targetId   target entity ID (must not be null)
     * @return safe internal approval readiness DTO
     */
    @Transactional(readOnly = true)
    public SafeInternalApprovalReadiness getReadiness(ReviewTargetType targetType, UUID targetId) {
        if (targetType == null) {
            return SafeInternalApprovalReadiness.unavailable(null, targetId,
                    InternalApprovalReadinessAvailability.UNAVAILABLE,
                    "READINESS_TARGET_TYPE_NULL", "Hedef türü belirtilmedi");
        }
        if (targetId == null) {
            return SafeInternalApprovalReadiness.unavailable(targetType, null,
                    InternalApprovalReadinessAvailability.MISSING_TARGET,
                    "READINESS_TARGET_ID_NULL", "Hedef kimliği belirtilmedi");
        }

        var recordOpt = reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId);

        if (recordOpt.isEmpty()) {
            log.debug("FP-014: Review kaydı bulunamadı: targetType={}, targetId={}", targetType, targetId);
            return SafeInternalApprovalReadiness.unavailable(targetType, targetId,
                    InternalApprovalReadinessAvailability.MISSING_TARGET,
                    "READINESS_RECORD_NOT_FOUND", "İnceleme kaydı bulunamadı");
        }

        ReviewRecordEntity record = recordOpt.get();
        return buildReadiness(record);
    }

    /**
     * Get internal approval readiness for all approved artifacts of a target type.
     *
     * <p>Read-only. Returns only APPROVED_FOR_INTERNAL_USE records.</p>
     *
     * @param targetType optional filter by target type (null returns all types)
     * @return list of safe readiness DTOs for approved artifacts
     */
    @Transactional(readOnly = true)
    public List<SafeInternalApprovalReadiness> getApprovedReadinessList(ReviewTargetType targetType) {
        List<ReviewRecordEntity> records;

        if (targetType != null) {
            records = reviewRecordRepository.findByCurrentStateAndReviewTargetType(
                    ReviewState.APPROVED_FOR_INTERNAL_USE, targetType);
        } else {
            records = reviewRecordRepository.findByCurrentState(
                    ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        return records.stream()
                .map(this::buildReadiness)
                .toList();
    }

    // ── Internal ────────────────────────────────────────────────────

    private SafeInternalApprovalReadiness buildReadiness(ReviewRecordEntity record) {
        ReviewState state = record.getCurrentState();
        InternalApprovalReadinessAvailability availability = resolveAvailability(state);
        List<SafeInternalAction> actions = resolveActions(state);
        List<String> warnings = resolveWarnings(state, record.getReviewTargetType());

        // Safe preview availability check — non-fatal
        boolean previewAvailable = checkPreviewAvailable(record.getReviewTargetType(),
                record.getReviewTargetId());

        // Approval metadata (only when approved)
        String approvedByRole = null;
        if (state == ReviewState.APPROVED_FOR_INTERNAL_USE) {
            approvedByRole = resolveApprovedByRole(record);
        }

        String safeMessage = buildSafeMessage(state);

        return SafeInternalApprovalReadiness.builder()
                .targetType(record.getReviewTargetType())
                .targetId(record.getReviewTargetId())
                .reviewState(state)
                .availability(availability)
                .safeTitle(ReviewTextSafety.sanitize(buildSafeTitle(record)))
                .safeSummary(ReviewTextSafety.sanitize(buildSafeSummary(record)))
                .safeReasonCode(ReviewTextSafety.sanitizeReasonCode(record.getSafeReasonCode()))
                .safeMessage(ReviewTextSafety.sanitize(safeMessage))
                .safePreviewAvailable(previewAvailable)
                .approvedAt(state == ReviewState.APPROVED_FOR_INTERNAL_USE
                        ? record.getLastActionAt() : null)
                .approvedByRole(approvedByRole)
                .safeNextInternalActions(actions)
                .safeWarnings(warnings)
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private InternalApprovalReadinessAvailability resolveAvailability(ReviewState state) {
        return switch (state) {
            case APPROVED_FOR_INTERNAL_USE -> InternalApprovalReadinessAvailability.READY;
            case REJECTED -> InternalApprovalReadinessAvailability.REJECTED;
            case DRAFT, AI_GENERATED, NEEDS_REVIEW, IN_REVIEW, CHANGES_REQUESTED ->
                    InternalApprovalReadinessAvailability.NOT_APPROVED;
        };
    }

    private List<SafeInternalAction> resolveActions(ReviewState state) {
        if (state == ReviewState.APPROVED_FOR_INTERNAL_USE) {
            return List.of(
                    SafeInternalAction.VIEW_SAFE_PREVIEW,
                    SafeInternalAction.REVIEW_APPROVAL_DETAILS,
                    SafeInternalAction.REQUEST_INTERNAL_REVISION,
                    SafeInternalAction.PREPARE_FOR_FUTURE_EXPORT,
                    SafeInternalAction.MARK_AS_INTERNAL_READY_CANDIDATE
            );
        }
        // Non-approved states: limited actions
        if (state == ReviewState.REJECTED) {
            return List.of(SafeInternalAction.REVIEW_APPROVAL_DETAILS);
        }
        // Pending review states
        return List.of(SafeInternalAction.VIEW_SAFE_PREVIEW);
    }

    private List<String> resolveWarnings(ReviewState state, ReviewTargetType targetType) {
        List<String> warnings = new ArrayList<>();

        if (state == ReviewState.APPROVED_FOR_INTERNAL_USE) {
            warnings.add("Dahili onay yayın/dışa aktarma onayı değildir");
        }
        if (state == ReviewState.REJECTED) {
            warnings.add("Bu öğe reddedilmiştir ve dahili kullanıma uygun değildir");
        }
        if (targetType == ReviewTargetType.IMAGE_ASSET) {
            warnings.add("Görsel varlık önizlemesi yalnızca meta veri içerir");
        }

        return List.copyOf(warnings);
    }

    private String buildSafeMessage(ReviewState state) {
        return switch (state) {
            case APPROVED_FOR_INTERNAL_USE ->
                    "Bu öğe dahili kullanım için onaylanmıştır. " +
                    "Bu onay yayın veya dışa aktarma anlamına gelmez.";
            case REJECTED ->
                    "Bu öğe reddedilmiştir. Dahili kullanım için uygun değildir.";
            case DRAFT ->
                    "Bu öğe taslak aşamasındadır. Manuel onay gereklidir.";
            case AI_GENERATED ->
                    "Bu öğe AI tarafından üretilmiştir. İnsan incelemesi gereklidir.";
            case NEEDS_REVIEW ->
                    "Bu öğe inceleme beklemektedir. Manuel onay gereklidir.";
            case IN_REVIEW ->
                    "Bu öğe şu anda inceleniyor. Manuel onay gereklidir.";
            case CHANGES_REQUESTED ->
                    "Bu öğe için değişiklik istenmiştir. Revizyon ve yeniden onay gereklidir.";
        };
    }

    private boolean checkPreviewAvailable(ReviewTargetType targetType, UUID targetId) {
        try {
            SafeAdminPreview preview = adminPreviewService.getPreview(targetType, targetId);
            return preview != null && preview.isPreviewAvailable();
        } catch (Exception e) {
            log.debug("FP-014: Önizleme kontrol başarısız (non-fatal): targetType={}, targetId={}",
                    targetType, targetId);
            return false;
        }
    }

    private String resolveApprovedByRole(ReviewRecordEntity record) {
        // Derive from last action actor reference if available
        String actorRef = record.getLastActionByActorReference();
        if (actorRef == null || actorRef.isBlank()) {
            return null;
        }
        // Safe extraction — role hint from actor reference format
        // Actor references follow pattern: "role:identifier" (e.g., "admin:user1")
        if (actorRef.startsWith("system:")) {
            return ReviewActorRole.SYSTEM.name();
        }
        if (actorRef.startsWith("admin:")) {
            return ReviewActorRole.ADMIN.name();
        }
        if (actorRef.startsWith("reviewer:")) {
            return ReviewActorRole.REVIEWER.name();
        }
        // Default — safe generic label, no raw actor reference leak
        return "REVIEWER";
    }

    private String buildSafeTitle(ReviewRecordEntity record) {
        String prefix = switch (record.getReviewTargetType()) {
            case CONTENT_PROJECT -> "Proje";
            case SCENARIO -> "Senaryo";
            case STORYBOARD -> "Storyboard";
            case PACKAGE -> "Paket";
            case IMAGE_ASSET -> "Görsel";
        };
        return prefix + ": " + record.getReviewTargetId();
    }

    private String buildSafeSummary(ReviewRecordEntity record) {
        return record.getReviewTargetType().name() + " — Durum: " + record.getCurrentState().name();
    }
}
