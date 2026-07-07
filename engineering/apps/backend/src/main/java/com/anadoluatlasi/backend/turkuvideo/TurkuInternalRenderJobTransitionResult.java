package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-025 Task 02: Transition decision result.
 * Immutable, safe — no raw exception messages.
 */
public final class TurkuInternalRenderJobTransitionResult {
    private final boolean allowed;
    private final TurkuInternalRenderJobStatus fromState;
    private final TurkuInternalRenderJobAction action;
    private final TurkuInternalRenderJobStatus toState;
    private final TurkuInternalRenderJobReasonCode reasonCode;

    private TurkuInternalRenderJobTransitionResult(boolean allowed,
            TurkuInternalRenderJobStatus fromState, TurkuInternalRenderJobAction action,
            TurkuInternalRenderJobStatus toState, TurkuInternalRenderJobReasonCode reasonCode) {
        this.allowed = allowed;
        this.fromState = fromState;
        this.action = action;
        this.toState = toState;
        this.reasonCode = reasonCode;
    }

    public static TurkuInternalRenderJobTransitionResult allowed(
            TurkuInternalRenderJobStatus from, TurkuInternalRenderJobAction action,
            TurkuInternalRenderJobStatus to) {
        return new TurkuInternalRenderJobTransitionResult(
                true, from, action, to, TurkuInternalRenderJobReasonCode.TRANSITION_ALLOWED);
    }

    public static TurkuInternalRenderJobTransitionResult denied(
            TurkuInternalRenderJobStatus from, TurkuInternalRenderJobAction action,
            TurkuInternalRenderJobReasonCode reason) {
        return new TurkuInternalRenderJobTransitionResult(false, from, action, null, reason);
    }

    public boolean isAllowed() { return allowed; }
    public TurkuInternalRenderJobStatus getFromState() { return fromState; }
    public TurkuInternalRenderJobAction getAction() { return action; }
    public TurkuInternalRenderJobStatus getToState() { return toState; }
    public TurkuInternalRenderJobReasonCode getReasonCode() { return reasonCode; }
}
