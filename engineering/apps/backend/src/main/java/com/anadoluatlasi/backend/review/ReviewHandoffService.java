package com.anadoluatlasi.backend.review;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * FP-013 Task 02: Internal service that creates or preserves review records
 * for generated outputs.
 *
 * <p>This service is internal only — NOT exposed via controller/API endpoint.
 * NOT wired into orchestration yet (Task 04).</p>
 *
 * <p>This service bypasses {@link ReviewDomainService} for the initial handoff
 * because SYSTEM cannot call SUBMIT_FOR_REVIEW via policy. Instead, it creates
 * the review record directly with NEEDS_REVIEW state and persists a safe audit
 * event using {@link ReviewAuditEventFactory}.</p>
 *
 * <p>Critical rules:</p>
 * <ul>
 *   <li>Idempotent: calling twice for the same target is safe</li>
 *   <li>Never auto-approves</li>
 *   <li>Never overwrites APPROVED_FOR_INTERNAL_USE or REJECTED</li>
 *   <li>Never resets IN_REVIEW to NEEDS_REVIEW</li>
 *   <li>SYSTEM may initiate handoff but cannot approve</li>
 *   <li>No publish/export/public URL/signed URL/CDN behavior</li>
 *   <li>No provider URL or storage key exposure</li>
 *   <li>No storage/provider calls</li>
 * </ul>
 */
@Service
public class ReviewHandoffService {

    /** States that are preserved — handoff does not disturb them. */
    private static final Set<ReviewState> PRESERVED_STATES = Set.of(
            ReviewState.NEEDS_REVIEW,
            ReviewState.IN_REVIEW,
            ReviewState.CHANGES_REQUESTED,
            ReviewState.APPROVED_FOR_INTERNAL_USE,
            ReviewState.REJECTED
    );

    /** States that can be safely advanced to NEEDS_REVIEW. */
    private static final Set<ReviewState> ADVANCEABLE_STATES = Set.of(
            ReviewState.DRAFT,
            ReviewState.AI_GENERATED
    );

    private final ReviewRecordRepository reviewRecordRepository;
    private final ReviewAuditEventRepository auditEventRepository;

    public ReviewHandoffService(ReviewRecordRepository reviewRecordRepository,
                                 ReviewAuditEventRepository auditEventRepository) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Hand off a generated output into the review workflow.
     *
     * <p>Idempotent: safe to call multiple times for the same target.</p>
     *
     * @param request safe handoff request (must not be null)
     * @return safe handoff result indicating created/updated/preserved
     * @throws ReviewHandoffException if request is invalid
     */
    @Transactional
    public ReviewHandoffResult handoffForReview(ReviewHandoffRequest request) {
        validateRequest(request);

        Optional<ReviewRecordEntity> existing = reviewRecordRepository
                .findByReviewTargetTypeAndReviewTargetId(request.getTargetType(), request.getTargetId());

        if (existing.isPresent()) {
            return handleExisting(existing.get(), request);
        }

        return createNew(request);
    }

    // ── Internal: Create new ────────────────────────────────────────

    /**
     * Create a new review record directly in NEEDS_REVIEW state.
     *
     * <p>Does NOT go through ReviewDomainService because SYSTEM cannot call
     * SUBMIT_FOR_REVIEW via ReviewTransitionPolicy. Instead, the record is
     * created directly with NEEDS_REVIEW state and a REVIEW_SUBMITTED audit
     * event is persisted.</p>
     */
    private ReviewHandoffResult createNew(ReviewHandoffRequest request) {
        ReviewRecordEntity record = ReviewPersistenceMapper.createNewRecord(
                request.getTargetType(), request.getTargetId(),
                ReviewState.NEEDS_REVIEW, request.getActorReference());
        reviewRecordRepository.save(record);

        // Persist safe audit event
        ReviewAuditRecord auditRecord = buildHandoffAuditRecord(
                null, ReviewState.NEEDS_REVIEW,
                request.getActorRole(), request.getActorReference());
        ReviewAuditEventEntity auditEntity = ReviewPersistenceMapper.toAuditEventEntity(
                auditRecord, record.getId());
        auditEventRepository.save(auditEntity);

        return ReviewHandoffResult.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .previousState(null)
                .currentState(ReviewState.NEEDS_REVIEW)
                .created(true)
                .updated(false)
                .preserved(false)
                .auditEventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                .safeReasonCode(null)
                .safeMessage("Review record created and submitted for review")
                .build();
    }

    // ── Internal: Handle existing ───────────────────────────────────

    private ReviewHandoffResult handleExisting(ReviewRecordEntity record,
                                                ReviewHandoffRequest request) {
        ReviewState currentState = record.getCurrentState();

        // Preserved states: do not disturb
        if (PRESERVED_STATES.contains(currentState)) {
            return ReviewHandoffResult.builder()
                    .targetType(request.getTargetType())
                    .targetId(request.getTargetId())
                    .previousState(currentState)
                    .currentState(currentState)
                    .created(false)
                    .updated(false)
                    .preserved(true)
                    .auditEventType(null)
                    .safeReasonCode("ALREADY_IN_PIPELINE")
                    .safeMessage("Review record already exists in state: " + currentState)
                    .build();
        }

        // Advanceable states: DRAFT / AI_GENERATED → NEEDS_REVIEW
        if (ADVANCEABLE_STATES.contains(currentState)) {
            return advanceToNeedsReview(record, request, currentState);
        }

        // Unknown state — should not happen, preserve safely
        return ReviewHandoffResult.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .previousState(currentState)
                .currentState(currentState)
                .created(false)
                .updated(false)
                .preserved(true)
                .auditEventType(null)
                .safeReasonCode("UNKNOWN_STATE_PRESERVED")
                .safeMessage("Existing review state preserved")
                .build();
    }

    /**
     * Advance an existing DRAFT or AI_GENERATED record to NEEDS_REVIEW directly.
     *
     * <p>Bypasses ReviewDomainService because SYSTEM cannot SUBMIT_FOR_REVIEW
     * via policy. Safe because we only advance early states forward.</p>
     */
    private ReviewHandoffResult advanceToNeedsReview(ReviewRecordEntity record,
                                                      ReviewHandoffRequest request,
                                                      ReviewState previousState) {
        record.setCurrentState(ReviewState.NEEDS_REVIEW);
        reviewRecordRepository.save(record);

        ReviewAuditRecord auditRecord = buildHandoffAuditRecord(
                previousState, ReviewState.NEEDS_REVIEW,
                request.getActorRole(), request.getActorReference());
        ReviewAuditEventEntity auditEntity = ReviewPersistenceMapper.toAuditEventEntity(
                auditRecord, record.getId());
        auditEventRepository.save(auditEntity);

        return ReviewHandoffResult.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .previousState(previousState)
                .currentState(ReviewState.NEEDS_REVIEW)
                .created(false)
                .updated(true)
                .preserved(false)
                .auditEventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                .safeReasonCode(null)
                .safeMessage("Review record advanced to NEEDS_REVIEW")
                .build();
    }

    // ── Validation ──────────────────────────────────────────────────

    private void validateRequest(ReviewHandoffRequest request) {
        if (request == null) {
            throw new ReviewHandoffException(
                    "Handoff request must not be null", "HANDOFF_REQUEST_NULL");
        }
        if (request.getTargetType() == null) {
            throw new ReviewHandoffException(
                    "Target type must not be null", "HANDOFF_TARGET_TYPE_NULL");
        }
        if (request.getTargetId() == null) {
            throw new ReviewHandoffException(
                    "Target ID must not be null", "HANDOFF_TARGET_ID_NULL");
        }
        if (request.getActorRole() == null) {
            throw new ReviewHandoffException(
                    "Actor role must not be null", "HANDOFF_ACTOR_ROLE_NULL");
        }
        if (request.getSource() == null) {
            throw new ReviewHandoffException(
                    "Handoff source must not be null", "HANDOFF_SOURCE_NULL");
        }
    }

    // ── Audit Helper ────────────────────────────────────────────────

    /**
     * Build a safe audit record for handoff operations.
     * Uses REVIEW_SUBMITTED event type — semantically correct for handoff.
     */
    private static ReviewAuditRecord buildHandoffAuditRecord(
            ReviewState fromState, ReviewState toState,
            ReviewActorRole actorRole, String actorReference) {
        return ReviewAuditRecord.builder()
                .eventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                .fromState(fromState)
                .toState(toState)
                .actionType(ReviewActionType.SUBMIT_FOR_REVIEW)
                .actorRole(actorRole)
                .actorReference(actorReference)
                .occurredAt(java.time.Instant.now())
                .safeReasonCode(null)
                .safeMessage("Review handoff: state is now " + toState.name())
                .build();
    }
}
