package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 03: Pure domain service for review workflow decisions.
 *
 * <p>Applies ReviewTransitionPolicy, sanitizes text via ReviewTextSafety,
 * creates audit records via ReviewAuditEventFactory, returns ReviewDecisionResult.</p>
 *
 * <p>This service does NOT persist anything, does NOT call repositories,
 * does NOT call external services, does NOT call storage/provider components.</p>
 */
public final class ReviewDomainService {

    private ReviewDomainService() {
        // Utility class — stateless pure logic
    }

    /**
     * Process a review command and return the decision result.
     *
     * @param command the review command to process
     * @return the decision result including audit record
     * @throws ReviewTransitionViolationException if the transition is not allowed
     */
    public static ReviewDecisionResult processCommand(ReviewCommand command) {
        validateCommand(command);

        // Handle ADD_REVIEW_NOTE — non-state-changing
        if (command.getActionType() == ReviewActionType.ADD_REVIEW_NOTE) {
            return handleAddNote(command);
        }

        // Resolve target state
        ReviewState targetState = resolveTargetState(command);

        // Validate transition with actor policy
        ReviewTransitionPolicy.validateTransition(
                command.getCurrentState(), targetState,
                command.getActionType(), command.getActorRole());

        // Validate required rejection reason
        if (command.getActionType() == ReviewActionType.REJECT) {
            ReviewTextSafety.requireNonBlank(command.getNote(), "rejection reason");
        }

        // Create audit record
        ReviewAuditRecord auditRecord = ReviewAuditEventFactory.createSuccessRecord(command, targetState);

        return ReviewDecisionResult.builder()
                .previousState(command.getCurrentState())
                .newState(targetState)
                .actionType(command.getActionType())
                .actorRole(command.getActorRole())
                .changed(true)
                .auditRecord(auditRecord)
                .safeMessage(buildSafeMessage(command.getActionType(), targetState))
                .safeReasonCode(command.getReasonCode())
                .build();
    }

    // ── Internal ────────────────────────────────────────────────────

    private static void validateCommand(ReviewCommand command) {
        if (command == null) {
            throw new ReviewTransitionViolationException("Review command must not be null");
        }
    }

    private static ReviewState resolveTargetState(ReviewCommand command) {
        // If requestedState is provided, use it; otherwise derive from policy
        if (command.getRequestedState() != null) {
            return command.getRequestedState();
        }
        ReviewState target = ReviewTransitionPolicy.targetFor(
                command.getCurrentState(), command.getActionType());
        if (target == null) {
            throw new ReviewTransitionViolationException(
                    command.getCurrentState(), command.getCurrentState(), command.getActionType());
        }
        return target;
    }

    private static ReviewDecisionResult handleAddNote(ReviewCommand command) {
        // Validate ADD_REVIEW_NOTE is allowed for this state/actor
        ReviewTransitionPolicy.validateTransition(
                command.getCurrentState(), command.getCurrentState(),
                ReviewActionType.ADD_REVIEW_NOTE, command.getActorRole());

        ReviewTextSafety.requireNonBlank(command.getNote(), "review note");

        ReviewAuditRecord auditRecord = ReviewAuditEventFactory.createNoteRecord(command);

        return ReviewDecisionResult.builder()
                .previousState(command.getCurrentState())
                .newState(command.getCurrentState())
                .actionType(ReviewActionType.ADD_REVIEW_NOTE)
                .actorRole(command.getActorRole())
                .changed(false)
                .auditRecord(auditRecord)
                .safeMessage("Review note added")
                .safeReasonCode(command.getReasonCode())
                .build();
    }

    private static String buildSafeMessage(ReviewActionType action, ReviewState newState) {
        return "Review action " + action.name() + " completed, state is now " + newState.name();
    }
}
