package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 06: Mapper from internal application/persistence objects to admin-safe DTOs.
 *
 * <p>Uses ReviewTextSafety for note/message/reason sanitization.
 * Does not expose forbidden fields, provider URLs, storage keys,
 * public/signed URLs, or publish/export status.</p>
 */
public final class AdminReviewDtoMapper {

    private AdminReviewDtoMapper() {
        // Utility class
    }

    /**
     * Map ReviewApplicationResult to AdminReviewActionResponse.
     */
    public static AdminReviewActionResponse toActionResponse(ReviewApplicationResult result) {
        return AdminReviewActionResponse.builder()
                .targetType(result.getTargetType())
                .targetId(result.getTargetId())
                .previousState(result.getPreviousState())
                .currentState(result.getCurrentState())
                .actionType(result.getActionType())
                .actorRole(result.getActorRole())
                .auditEventType(result.getAuditEventType())
                .changed(result.isChanged())
                .safeMessage(ReviewTextSafety.sanitize(result.getSafeMessage()))
                .safeReasonCode(ReviewTextSafety.sanitizeReasonCode(result.getSafeReasonCode()))
                .occurredAt(result.getOccurredAt())
                .build();
    }

    /**
     * Map ReviewRecordEntity to AdminReviewStateSummary.
     */
    public static AdminReviewStateSummary toStateSummary(ReviewRecordEntity entity) {
        return AdminReviewStateSummary.builder()
                .targetType(entity.getReviewTargetType())
                .targetId(entity.getReviewTargetId())
                .currentState(entity.getCurrentState())
                .currentReviewerActorReference(entity.getCurrentReviewerActorReference())
                .lastActionType(entity.getLastActionType())
                .lastActionAt(entity.getLastActionAt())
                .lastActionByActorReference(entity.getLastActionByActorReference())
                .safeMessage(ReviewTextSafety.sanitize(entity.getSafeMessage()))
                .safeReasonCode(ReviewTextSafety.sanitizeReasonCode(entity.getSafeReasonCode()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Map ReviewAuditEventEntity to AdminReviewAuditTimelineItem.
     */
    public static AdminReviewAuditTimelineItem toTimelineItem(ReviewAuditEventEntity entity) {
        return AdminReviewAuditTimelineItem.builder()
                .eventType(entity.getEventType())
                .fromState(entity.getFromState())
                .toState(entity.getToState())
                .actionType(entity.getActionType())
                .actorRole(entity.getActorRole())
                .actorReference(entity.getActorReference())
                .occurredAt(entity.getOccurredAt())
                .safeReasonCode(ReviewTextSafety.sanitizeReasonCode(entity.getSafeReasonCode()))
                .safeMessage(ReviewTextSafety.sanitize(entity.getSafeMessage()))
                .sanitizedNote(ReviewTextSafety.sanitize(entity.getSanitizedNote()))
                .build();
    }
}
