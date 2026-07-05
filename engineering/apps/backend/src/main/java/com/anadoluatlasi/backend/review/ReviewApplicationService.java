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
