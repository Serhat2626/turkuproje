package com.anadoluatlasi.backend.review;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * FP-012 Task 04: Mapper between domain model and persistence model.
 *
 * <p>Sanitizes messages/notes through ReviewTextSafety.
 * Preserves internal approval semantics only.
 * APPROVED_FOR_INTERNAL_USE does not become publish/export status.</p>
 */
public final class ReviewPersistenceMapper {

    private ReviewPersistenceMapper() {
        // Utility class
    }

    /**
     * Apply a ReviewDecisionResult to a ReviewRecordEntity.
     * Updates state and action tracking fields.
     */
    public static void applyDecisionToRecord(ReviewDecisionResult result,
                                              ReviewRecordEntity entity) {
        entity.setCurrentState(result.getNewState());
        entity.setLastActionType(result.getActionType());
        entity.setLastActionAt(LocalDateTime.now());
        entity.setLastActionByActorReference(
                result.getAuditRecord() != null ? result.getAuditRecord().getActorReference() : null);
        entity.setSafeReasonCode(
                ReviewTextSafety.sanitizeReasonCode(result.getSafeReasonCode()));
        entity.setSafeMessage(
                ReviewTextSafety.sanitize(result.getSafeMessage()));

        // Set reviewer reference if a review action was started
        if (result.getActionType() == ReviewActionType.START_REVIEW
                && result.getAuditRecord() != null) {
            entity.setCurrentReviewerActorReference(result.getAuditRecord().getActorReference());
        }
    }

    /**
     * Map a ReviewAuditRecord to a ReviewAuditEventEntity.
     */
    public static ReviewAuditEventEntity toAuditEventEntity(ReviewAuditRecord auditRecord,
                                                             java.util.UUID reviewRecordId) {
        return ReviewAuditEventEntity.builder()
                .reviewRecordId(reviewRecordId)
                .eventType(auditRecord.getEventType())
                .fromState(auditRecord.getFromState())
                .toState(auditRecord.getToState())
                .actionType(auditRecord.getActionType())
                .actorRole(auditRecord.getActorRole())
                .actorReference(auditRecord.getActorReference())
                .occurredAt(auditRecord.getOccurredAt() != null
                        ? LocalDateTime.ofInstant(auditRecord.getOccurredAt(), ZoneOffset.UTC)
                        : LocalDateTime.now())
                .safeReasonCode(ReviewTextSafety.sanitizeReasonCode(auditRecord.getSafeReasonCode()))
                .safeMessage(ReviewTextSafety.sanitize(auditRecord.getSafeMessage()))
                .sanitizedNote(null) // Note is sanitized separately if provided
                .build();
    }

    /**
     * Map a ReviewAuditRecord to a ReviewAuditEventEntity, including a sanitized note.
     */
    public static ReviewAuditEventEntity toAuditEventEntityWithNote(ReviewAuditRecord auditRecord,
                                                                      java.util.UUID reviewRecordId,
                                                                      String rawNote) {
        ReviewAuditEventEntity entity = toAuditEventEntity(auditRecord, reviewRecordId);
        entity.setSanitizedNote(ReviewTextSafety.sanitize(rawNote));
        return entity;
    }

    /**
     * Create a new ReviewRecordEntity for a new review target.
     */
    public static ReviewRecordEntity createNewRecord(ReviewTargetType targetType,
                                                      java.util.UUID targetId,
                                                      ReviewState initialState,
                                                      String createdByActorReference) {
        return ReviewRecordEntity.builder()
                .reviewTargetType(targetType)
                .reviewTargetId(targetId)
                .currentState(initialState)
                .createdByActorReference(createdByActorReference)
                .build();
    }
}
