package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 02: Exception for forbidden/invalid review state transitions.
 *
 * <p>Messages are safe — no secrets, provider URLs, storage keys,
 * raw payloads, base64, or stack traces in user/admin-facing context.</p>
 */
public class ReviewTransitionViolationException extends RuntimeException {

    private final ReviewState fromState;
    private final ReviewState toState;
    private final ReviewActionType action;

    public ReviewTransitionViolationException(ReviewState fromState,
                                               ReviewState toState,
                                               ReviewActionType action) {
        super(buildSafeMessage(fromState, toState, action));
        this.fromState = fromState;
        this.toState = toState;
        this.action = action;
    }

    public ReviewTransitionViolationException(String safeMessage) {
        super(safeMessage);
        this.fromState = null;
        this.toState = null;
        this.action = null;
    }

    public ReviewState getFromState() {
        return fromState;
    }

    public ReviewState getToState() {
        return toState;
    }

    public ReviewActionType getAction() {
        return action;
    }

    private static String buildSafeMessage(ReviewState from, ReviewState to, ReviewActionType action) {
        return "Review transition not allowed: " + from + " -> " + to + " via " + action;
    }
}
