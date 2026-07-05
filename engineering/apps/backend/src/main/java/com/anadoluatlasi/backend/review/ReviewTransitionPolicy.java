package com.anadoluatlasi.backend.review;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * FP-012 Task 02: Review state transition policy with actor role enforcement.
 *
 * <p>Pure logic — no Spring bean, no DB, no external calls, no side effects.</p>
 *
 * <p>Critical rules:</p>
 * <ul>
 *   <li>APPROVE_FOR_INTERNAL_USE only works from IN_REVIEW by REVIEWER or ADMIN</li>
 *   <li>CREATOR cannot approve</li>
 *   <li>SYSTEM cannot approve, reject, or perform human review actions</li>
 *   <li>REJECTED is terminal for state-changing transitions</li>
 *   <li>APPROVED_FOR_INTERNAL_USE is terminal for state-changing transitions</li>
 *   <li>ADD_REVIEW_NOTE does not change state</li>
 *   <li>Generic arbitrary status updates are forbidden</li>
 *   <li>Null/unknown inputs fail safely</li>
 * </ul>
 */
public final class ReviewTransitionPolicy {

    /**
     * Allowed state-changing transitions: Map of (fromState, action) -> toState.
     */
    private static final Map<TransitionKey, ReviewState> ALLOWED_TRANSITIONS = Map.of(
            key(ReviewState.DRAFT, ReviewActionType.SUBMIT_FOR_REVIEW),
            ReviewState.NEEDS_REVIEW,

            key(ReviewState.AI_GENERATED, ReviewActionType.SUBMIT_FOR_REVIEW),
            ReviewState.NEEDS_REVIEW,

            key(ReviewState.NEEDS_REVIEW, ReviewActionType.START_REVIEW),
            ReviewState.IN_REVIEW,

            key(ReviewState.IN_REVIEW, ReviewActionType.REQUEST_CHANGES),
            ReviewState.CHANGES_REQUESTED,

            key(ReviewState.IN_REVIEW, ReviewActionType.APPROVE_FOR_INTERNAL_USE),
            ReviewState.APPROVED_FOR_INTERNAL_USE,

            key(ReviewState.IN_REVIEW, ReviewActionType.REJECT),
            ReviewState.REJECTED,

            key(ReviewState.CHANGES_REQUESTED, ReviewActionType.SUBMIT_FOR_REVIEW),
            ReviewState.NEEDS_REVIEW,

            key(ReviewState.CHANGES_REQUESTED, ReviewActionType.RETURN_TO_DRAFT),
            ReviewState.DRAFT
    );

    /**
     * States that allow ADD_REVIEW_NOTE (non-state-changing).
     */
    private static final Set<ReviewState> NOTE_ALLOWED_STATES = Set.of(
            ReviewState.DRAFT,
            ReviewState.NEEDS_REVIEW,
            ReviewState.IN_REVIEW,
            ReviewState.CHANGES_REQUESTED,
            ReviewState.APPROVED_FOR_INTERNAL_USE,
            ReviewState.REJECTED
    );

    private ReviewTransitionPolicy() {
        // Utility class — no instantiation
    }

    /**
     * Check if a transition is allowed for the given actor role.
     *
     * @param from      current state (must not be null)
     * @param to        target state (must not be null)
     * @param action    review action (must not be null)
     * @param actorRole actor performing the action (must not be null)
     * @return true if the transition is allowed
     * @throws ReviewTransitionViolationException if any parameter is null
     */
    public static boolean canTransition(ReviewState from, ReviewState to,
                                         ReviewActionType action, ReviewActorRole actorRole) {
        validateNotNull(from, to, action, actorRole);

        // ADD_REVIEW_NOTE is non-state-changing
        if (action == ReviewActionType.ADD_REVIEW_NOTE) {
            return from == to && NOTE_ALLOWED_STATES.contains(from) && actorRole.canAddReviewNote();
        }

        // Check state transition is defined
        ReviewState expected = ALLOWED_TRANSITIONS.get(key(from, action));
        if (expected == null || expected != to) {
            return false;
        }

        // Check actor role permissions
        return isActorAllowedForAction(action, actorRole);
    }

    /**
     * Validate a transition — throws if not allowed.
     */
    public static void validateTransition(ReviewState from, ReviewState to,
                                           ReviewActionType action, ReviewActorRole actorRole) {
        if (!canTransition(from, to, action, actorRole)) {
            throw new ReviewTransitionViolationException(from, to, action);
        }
    }

    /**
     * Get the set of states reachable from a given state via any allowed action for the given role.
     */
    public static Set<ReviewState> allowedTargets(ReviewState from, ReviewActorRole actorRole) {
        if (from == null) {
            throw new ReviewTransitionViolationException("Review state must not be null");
        }
        if (actorRole == null) {
            throw new ReviewTransitionViolationException("Actor role must not be null");
        }
        Set<ReviewState> targets = new HashSet<>();
        for (var entry : ALLOWED_TRANSITIONS.entrySet()) {
            if (entry.getKey().from == from
                    && isActorAllowedForAction(entry.getKey().action, actorRole)) {
                targets.add(entry.getValue());
            }
        }
        return Set.copyOf(targets);
    }

    /**
     * Whether the given action changes state (vs. ADD_REVIEW_NOTE which does not).
     */
    public static boolean isStateChangingAction(ReviewActionType action) {
        return action != null && action != ReviewActionType.ADD_REVIEW_NOTE;
    }

    /**
     * Get the expected target state for a given (from, action) pair.
     * Returns null for ADD_REVIEW_NOTE or unknown combinations.
     */
    public static ReviewState targetFor(ReviewState from, ReviewActionType action) {
        if (from == null || action == null) return null;
        if (action == ReviewActionType.ADD_REVIEW_NOTE) return null;
        return ALLOWED_TRANSITIONS.get(key(from, action));
    }

    // ── Actor role policy ───────────────────────────────────────────

    private static boolean isActorAllowedForAction(ReviewActionType action, ReviewActorRole actorRole) {
        return switch (action) {
            case SUBMIT_FOR_REVIEW -> actorRole.canSubmitForReview();
            case START_REVIEW -> actorRole.canPerformReview();
            case REQUEST_CHANGES -> actorRole.canPerformReview();
            case APPROVE_FOR_INTERNAL_USE -> actorRole.canPerformReview();
            case REJECT -> actorRole.canPerformReview();
            case RETURN_TO_DRAFT -> actorRole == ReviewActorRole.CREATOR
                    || actorRole == ReviewActorRole.REVIEWER
                    || actorRole == ReviewActorRole.ADMIN;
            case ADD_REVIEW_NOTE -> actorRole.canAddReviewNote();
        };
    }

    // ── Internal ────────────────────────────────────────────────────

    private static void validateNotNull(ReviewState from, ReviewState to,
                                         ReviewActionType action, ReviewActorRole actorRole) {
        if (from == null || to == null || action == null || actorRole == null) {
            throw new ReviewTransitionViolationException(
                    "Review transition parameters must not be null");
        }
    }

    private static TransitionKey key(ReviewState from, ReviewActionType action) {
        return new TransitionKey(from, action);
    }

    private record TransitionKey(ReviewState from, ReviewActionType action) {}
}
