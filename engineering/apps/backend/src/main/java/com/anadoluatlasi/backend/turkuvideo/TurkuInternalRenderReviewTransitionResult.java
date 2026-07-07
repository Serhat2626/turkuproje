package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-026 Task 02: Review transition result.
 * Immutable, safe — no raw exception messages.
 */
public final class TurkuInternalRenderReviewTransitionResult {
    private final boolean allowed;
    private final TurkuInternalRenderReviewState fromState;
    private final TurkuInternalRenderReviewAction action;
    private final TurkuInternalRenderReviewState toState;
    private final TurkuInternalRenderReviewReasonCode reasonCode;

    private TurkuInternalRenderReviewTransitionResult(boolean allowed,
            TurkuInternalRenderReviewState fromState, TurkuInternalRenderReviewAction action,
            TurkuInternalRenderReviewState toState, TurkuInternalRenderReviewReasonCode reasonCode) {
        this.allowed = allowed;
        this.fromState = fromState;
        this.action = action;
        this.toState = toState;
        this.reasonCode = reasonCode;
    }

    public static TurkuInternalRenderReviewTransitionResult allowed(
            TurkuInternalRenderReviewState from, TurkuInternalRenderReviewAction action,
            TurkuInternalRenderReviewState to, TurkuInternalRenderReviewReasonCode reason) {
        return new TurkuInternalRenderReviewTransitionResult(true, from, action, to, reason);
    }

    public static TurkuInternalRenderReviewTransitionResult denied(
            TurkuInternalRenderReviewState from, TurkuInternalRenderReviewAction action,
            TurkuInternalRenderReviewReasonCode reason) {
        return new TurkuInternalRenderReviewTransitionResult(false, from, action, null, reason);
    }

    public boolean isAllowed() { return allowed; }
    public TurkuInternalRenderReviewState getFromState() { return fromState; }
    public TurkuInternalRenderReviewAction getAction() { return action; }
    public TurkuInternalRenderReviewState getToState() { return toState; }
    public TurkuInternalRenderReviewReasonCode getReasonCode() { return reasonCode; }
}
