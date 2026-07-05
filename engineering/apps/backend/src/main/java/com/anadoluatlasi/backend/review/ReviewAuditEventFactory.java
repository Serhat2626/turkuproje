package com.anadoluatlasi.backend.review;

import java.time.Instant;

/**
 * FP-012 Task 03: Factory that maps review actions to safe audit records.
 *
 * <p>All produced records are safe — no secrets, provider URLs, storage keys,
 * raw payloads, or base64.</p>
 */
public final class ReviewAuditEventFactory {

    private ReviewAuditEventFactory() {
        // Utility class
    }

    /**
     * Create an audit record for a successful review action.
     */
    public static ReviewAuditRecord createSuccessRecord(ReviewCommand command,
                                                         ReviewState resultState) {
        return ReviewAuditRecord.builder()
                .eventType(mapActionToEventType(command.getActionType()))
                .fromState(command.getCurrentState())
                .toState(resultState)
                .actionType(command.getActionType())
                .actorRole(command.getActorRole())
                .actorReference(command.getActorReference())
                .occurredAt(Instant.now())
                .safeReasonCode(command.getReasonCode())
                .safeMessage(buildSafeSuccessMessage(command.getActionType(), resultState))
                .build();
    }

    /**
     * Create an audit record for a rejected/forbidden transition.
     */
    public static ReviewAuditRecord createRejectionRecord(ReviewCommand command,
                                                            String safeRejectionReason) {
        return ReviewAuditRecord.builder()
                .eventType(ReviewAuditEventType.REVIEW_TRANSITION_REJECTED)
                .fromState(command.getCurrentState())
                .toState(command.getRequestedState())
                .actionType(command.getActionType())
                .actorRole(command.getActorRole())
                .actorReference(command.getActorReference())
                .occurredAt(Instant.now())
                .safeReasonCode("TRANSITION_REJECTED")
                .safeMessage(safeRejectionReason != null ? safeRejectionReason : "Transition not allowed")
                .build();
    }

    /**
     * Create an audit record for ADD_REVIEW_NOTE (no state change).
     */
    public static ReviewAuditRecord createNoteRecord(ReviewCommand command) {
        return ReviewAuditRecord.builder()
                .eventType(ReviewAuditEventType.REVIEW_NOTE_ADDED)
                .fromState(command.getCurrentState())
                .toState(command.getCurrentState())
                .actionType(ReviewActionType.ADD_REVIEW_NOTE)
                .actorRole(command.getActorRole())
                .actorReference(command.getActorReference())
                .occurredAt(Instant.now())
                .safeReasonCode(command.getReasonCode())
                .safeMessage("Review note added")
                .build();
    }

    // ── Internal mappings ───────────────────────────────────────────

    /**
     * Map a review action to its corresponding audit event type.
     */
    static ReviewAuditEventType mapActionToEventType(ReviewActionType action) {
        return switch (action) {
            case SUBMIT_FOR_REVIEW -> ReviewAuditEventType.REVIEW_SUBMITTED;
            case START_REVIEW -> ReviewAuditEventType.REVIEW_STARTED;
            case REQUEST_CHANGES -> ReviewAuditEventType.CHANGES_REQUESTED;
            case APPROVE_FOR_INTERNAL_USE -> ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED;
            case REJECT -> ReviewAuditEventType.REVIEW_REJECTED;
            case RETURN_TO_DRAFT -> ReviewAuditEventType.RETURNED_TO_DRAFT;
            case ADD_REVIEW_NOTE -> ReviewAuditEventType.REVIEW_NOTE_ADDED;
        };
    }

    private static String buildSafeSuccessMessage(ReviewActionType action, ReviewState resultState) {
        return "Review action " + action.name() + " completed, state is now " + resultState.name();
    }
}
