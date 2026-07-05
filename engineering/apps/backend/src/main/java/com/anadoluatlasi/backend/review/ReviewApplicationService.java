package com.anadoluatlasi.backend.review;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-012 Task 05: Internal application service for human/admin review actions.
 *
 * <p>Combines ReviewDomainService, ReviewPersistenceMapper, and repositories
 * within a transaction boundary.</p>
 *
 * <p>This service is internal only — NOT exposed via controller/API endpoint.</p>
 *
 * <p>Critical rules:</p>
 * <ul>
 *   <li>No automatic approval</li>
 *   <li>No publish/export behavior</li>
 *   <li>No storage/provider calls</li>
 *   <li>No external API calls</li>
 *   <li>APPROVED_FOR_INTERNAL_USE is NOT publish/export approval</li>
 * </ul>
 */
@Service
public class ReviewApplicationService {

    private final ReviewRecordRepository reviewRecordRepository;
    private final ReviewAuditEventRepository auditEventRepository;

    public ReviewApplicationService(ReviewRecordRepository reviewRecordRepository,
                                     ReviewAuditEventRepository auditEventRepository) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.auditEventRepository = auditEventRepository;
    }

    // ── Review Actions ──────────────────────────────────────────────

    /**
     * Submit content for review. Creates a new review record if one does not exist.
     */
    @Transactional
    public ReviewApplicationResult submitForReview(ReviewTargetType targetType, UUID targetId,
                                                    String actorReference, ReviewActorRole actorRole,
                                                    String note) {
        ReviewRecordEntity record = findOrCreateRecord(targetType, targetId, actorReference);
        return executeAction(record, targetType, targetId, ReviewActionType.SUBMIT_FOR_REVIEW,
                actorRole, actorReference, null, note);
    }

    /**
     * Start reviewing content.
     */
    @Transactional
    public ReviewApplicationResult startReview(ReviewTargetType targetType, UUID targetId,
                                                String actorReference, ReviewActorRole actorRole,
                                                String note) {
        ReviewRecordEntity record = findExistingOrFail(targetType, targetId);
        return executeAction(record, targetType, targetId, ReviewActionType.START_REVIEW,
                actorRole, actorReference, null, note);
    }

    /**
     * Request changes on content under review.
     */
    @Transactional
    public ReviewApplicationResult requestChanges(ReviewTargetType targetType, UUID targetId,
                                                   String actorReference, ReviewActorRole actorRole,
                                                   String reasonCode, String note) {
        ReviewRecordEntity record = findExistingOrFail(targetType, targetId);
        return executeAction(record, targetType, targetId, ReviewActionType.REQUEST_CHANGES,
                actorRole, actorReference, reasonCode, note);
    }

    /**
     * Approve content for internal use only — NOT publish/export approval.
     */
    @Transactional
    public ReviewApplicationResult approveForInternalUse(ReviewTargetType targetType, UUID targetId,
                                                          String actorReference, ReviewActorRole actorRole,
                                                          String reasonCode, String note) {
        ReviewRecordEntity record = findExistingOrFail(targetType, targetId);
        return executeAction(record, targetType, targetId, ReviewActionType.APPROVE_FOR_INTERNAL_USE,
                actorRole, actorReference, reasonCode, note);
    }

    /**
     * Reject content.
     */
    @Transactional
    public ReviewApplicationResult reject(ReviewTargetType targetType, UUID targetId,
                                           String actorReference, ReviewActorRole actorRole,
                                           String reasonCode, String note) {
        ReviewRecordEntity record = findExistingOrFail(targetType, targetId);
        return executeAction(record, targetType, targetId, ReviewActionType.REJECT,
                actorRole, actorReference, reasonCode, note);
    }

    /**
     * Return content to draft state for rework.
     */
    @Transactional
    public ReviewApplicationResult returnToDraft(ReviewTargetType targetType, UUID targetId,
                                                  String actorReference, ReviewActorRole actorRole,
                                                  String reasonCode, String note) {
        ReviewRecordEntity record = findExistingOrFail(targetType, targetId);
        return executeAction(record, targetType, targetId, ReviewActionType.RETURN_TO_DRAFT,
                actorRole, actorReference, reasonCode, note);
    }

    /**
     * Add a review note without changing state.
     */
    @Transactional
    public ReviewApplicationResult addReviewNote(ReviewTargetType targetType, UUID targetId,
                                                  String actorReference, ReviewActorRole actorRole,
                                                  String note) {
        ReviewRecordEntity record = findExistingOrFail(targetType, targetId);
        return executeAction(record, targetType, targetId, ReviewActionType.ADD_REVIEW_NOTE,
                actorRole, actorReference, null, note);
    }

    // ── Read-Only Queries (internal only) ───────────────────────────

    /**
     * Get current review state for a target. Internal only — no endpoint.
     */
    @Transactional(readOnly = true)
    public Optional<ReviewState> getReviewState(ReviewTargetType targetType, UUID targetId) {
        return reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId)
                .map(ReviewRecordEntity::getCurrentState);
    }

    /**
     * Get audit event timeline for a target. Internal only — no endpoint.
     */
    @Transactional(readOnly = true)
    public List<ReviewAuditEventEntity> getAuditTimeline(ReviewTargetType targetType, UUID targetId) {
        Optional<ReviewRecordEntity> record =
                reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId);
        if (record.isEmpty()) {
            return List.of();
        }
        return auditEventRepository.findByReviewRecordIdOrderByOccurredAtAsc(record.get().getId());
    }

    // ── FP-012 Task 10: Worklist & Target Context Queries ────────────

    /**
     * List worklist items with optional filters. Bounded, safe, read-only.
     * No provider/storage/external calls. No unbounded query.
     */
    @Transactional(readOnly = true)
    public List<AdminReviewWorklistItem> listWorkItems(ReviewState stateFilter,
                                                        ReviewTargetType targetTypeFilter,
                                                        String reviewerFilter,
                                                        int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<ReviewRecordEntity> records = findFilteredRecords(stateFilter, targetTypeFilter, reviewerFilter);
        return records.stream()
                .limit(safeLimit)
                .map(AdminReviewDtoMapper::toWorklistItem)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get safe target context. Derived from ReviewRecordEntity only.
     * No provider/storage/external calls.
     */
    @Transactional(readOnly = true)
    public Optional<AdminReviewTargetContext> getTargetContext(ReviewTargetType targetType, UUID targetId) {
        return reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId)
                .map(AdminReviewDtoMapper::toTargetContext);
    }

    /**
     * Get full review record as state summary. Internal use.
     */
    @Transactional(readOnly = true)
    public Optional<AdminReviewStateSummary> getReviewStateSummary(ReviewTargetType targetType, UUID targetId) {
        return reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId)
                .map(AdminReviewDtoMapper::toStateSummary);
    }

    private List<ReviewRecordEntity> findFilteredRecords(ReviewState stateFilter,
                                                          ReviewTargetType targetTypeFilter,
                                                          String reviewerFilter) {
        if (stateFilter != null && targetTypeFilter != null) {
            return reviewRecordRepository.findByCurrentStateAndReviewTargetType(stateFilter, targetTypeFilter);
        }
        if (stateFilter != null) {
            return reviewRecordRepository.findByCurrentState(stateFilter);
        }
        if (targetTypeFilter != null) {
            return reviewRecordRepository.findByReviewTargetType(targetTypeFilter);
        }
        if (reviewerFilter != null && !reviewerFilter.isBlank()) {
            return reviewRecordRepository.findByCurrentReviewerActorReference(reviewerFilter);
        }
        return reviewRecordRepository.findAll();
    }

    // ── FP-012 Task 11: Action Availability ─────────────────────────

    /**
     * Get available actions for a target and actor role.
     * Uses ReviewTransitionPolicy as source of truth.
     * CREATOR/SYSTEM cannot approve. Terminal states allow notes only.
     */
    @Transactional(readOnly = true)
    public Optional<AdminReviewActionAvailability> getAvailableActions(
            ReviewTargetType targetType, UUID targetId, ReviewActorRole actorRole) {
        return reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId)
                .map(record -> buildAvailability(record, actorRole));
    }

    private AdminReviewActionAvailability buildAvailability(ReviewRecordEntity record,
                                                             ReviewActorRole actorRole) {
        ReviewState currentState = record.getCurrentState();
        List<AdminReviewAvailableAction> actions = new java.util.ArrayList<>();

        for (ReviewActionType actionType : ReviewActionType.values()) {
            actions.add(buildActionAvailability(currentState, actionType, actorRole));
        }

        return AdminReviewActionAvailability.builder()
                .targetType(record.getReviewTargetType())
                .targetId(record.getReviewTargetId())
                .currentState(currentState)
                .actorRole(actorRole)
                .availableActions(actions)
                .safeMessage(null)
                .build();
    }

    private AdminReviewAvailableAction buildActionAvailability(ReviewState currentState,
                                                                ReviewActionType actionType,
                                                                ReviewActorRole actorRole) {
        // ADD_REVIEW_NOTE: enabled if actorRole can add notes (non-state-changing)
        if (actionType == ReviewActionType.ADD_REVIEW_NOTE) {
            boolean canNote = actorRole.canAddReviewNote();
            return AdminReviewAvailableAction.builder()
                    .actionType(actionType)
                    .enabled(canNote)
                    .disabledReasonCode(canNote ? null : "ACTOR_NOT_PERMITTED")
                    .safeLabel("Not Ekle")
                    .safeDescription("İnceleme notu ekle")
                    .build();
        }

        // State-changing: check transition policy
        ReviewState target = ReviewTransitionPolicy.targetFor(currentState, actionType);
        if (target == null) {
            return AdminReviewAvailableAction.builder()
                    .actionType(actionType)
                    .enabled(false)
                    .disabledReasonCode("STATE_NOT_ALLOWED")
                    .safeLabel(safeLabelFor(actionType))
                    .safeDescription("Bu durumda kullanılamaz")
                    .build();
        }

        boolean actorAllowed = ReviewTransitionPolicy.canTransition(
                currentState, target, actionType, actorRole);
        String reason = actorAllowed ? null : "ACTOR_NOT_PERMITTED";

        return AdminReviewAvailableAction.builder()
                .actionType(actionType)
                .enabled(actorAllowed)
                .disabledReasonCode(reason)
                .safeLabel(safeLabelFor(actionType))
                .safeDescription(actorAllowed ? null : "Bu işlem için yetkiniz yok")
                .build();
    }

    private String safeLabelFor(ReviewActionType actionType) {
        return switch (actionType) {
            case SUBMIT_FOR_REVIEW -> "İncelemeye Gönder";
            case START_REVIEW -> "İncelemeyi Başlat";
            case REQUEST_CHANGES -> "Değişiklik İste";
            case APPROVE_FOR_INTERNAL_USE -> "Dahili Onayla (İç Kullanım)";
            case REJECT -> "Reddet";
            case RETURN_TO_DRAFT -> "Taslağa Döndür";
            case ADD_REVIEW_NOTE -> "Not Ekle";
        };
    }

    // ── Internal ────────────────────────────────────────────────────

    private ReviewApplicationResult executeAction(ReviewRecordEntity record,
                                                   ReviewTargetType targetType, UUID targetId,
                                                   ReviewActionType actionType,
                                                   ReviewActorRole actorRole, String actorReference,
                                                   String reasonCode, String note) {
        ReviewState previousState = record.getCurrentState();

        // Build domain command
        ReviewCommand command = ReviewCommand.builder()
                .currentState(previousState)
                .actionType(actionType)
                .actorRole(actorRole)
                .actorReference(actorReference)
                .reasonCode(reasonCode)
                .note(note)
                .build();

        // Execute domain logic
        ReviewDecisionResult decision = ReviewDomainService.processCommand(command);

        // Apply to persistence
        ReviewPersistenceMapper.applyDecisionToRecord(decision, record);
        reviewRecordRepository.save(record);

        // Persist audit event
        ReviewAuditEventEntity auditEntity = (note != null && !note.isBlank())
                ? ReviewPersistenceMapper.toAuditEventEntityWithNote(
                        decision.getAuditRecord(), record.getId(), note)
                : ReviewPersistenceMapper.toAuditEventEntity(
                        decision.getAuditRecord(), record.getId());
        auditEventRepository.save(auditEntity);

        // Build safe application result
        return ReviewApplicationResult.builder()
                .targetType(targetType)
                .targetId(targetId)
                .previousState(previousState)
                .currentState(decision.getNewState())
                .actionType(actionType)
                .actorRole(actorRole)
                .changed(decision.isChanged())
                .auditEventType(decision.getAuditRecord().getEventType())
                .safeMessage(decision.getSafeMessage())
                .safeReasonCode(decision.getSafeReasonCode())
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private ReviewRecordEntity findExistingOrFail(ReviewTargetType targetType, UUID targetId) {
        return reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId)
                .orElseThrow(() -> new ReviewApplicationException(
                        "Review record not found for " + targetType + " " + targetId,
                        targetType, targetId));
    }

    private ReviewRecordEntity findOrCreateRecord(ReviewTargetType targetType, UUID targetId,
                                                    String actorReference) {
        return reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId)
                .orElseGet(() -> ReviewPersistenceMapper.createNewRecord(
                        targetType, targetId, ReviewState.DRAFT, actorReference));
    }
}
