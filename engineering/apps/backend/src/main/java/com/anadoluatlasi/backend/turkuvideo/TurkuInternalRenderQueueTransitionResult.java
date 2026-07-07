package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Queue transition result.
 */
public final class TurkuInternalRenderQueueTransitionResult {
    private final boolean allowed;
    private final TurkuInternalRenderQueueState fromState;
    private final TurkuInternalRenderQueueAction action;
    private final TurkuInternalRenderQueueState toState;
    private final TurkuInternalRenderQueueReasonCode reasonCode;

    private TurkuInternalRenderQueueTransitionResult(boolean allowed,
            TurkuInternalRenderQueueState from, TurkuInternalRenderQueueAction action,
            TurkuInternalRenderQueueState to, TurkuInternalRenderQueueReasonCode reason) {
        this.allowed = allowed; this.fromState = from; this.action = action;
        this.toState = to; this.reasonCode = reason;
    }

    public static TurkuInternalRenderQueueTransitionResult allowed(
            TurkuInternalRenderQueueState from, TurkuInternalRenderQueueAction action,
            TurkuInternalRenderQueueState to, TurkuInternalRenderQueueReasonCode reason) {
        return new TurkuInternalRenderQueueTransitionResult(true, from, action, to, reason);
    }

    public static TurkuInternalRenderQueueTransitionResult denied(
            TurkuInternalRenderQueueState from, TurkuInternalRenderQueueAction action,
            TurkuInternalRenderQueueReasonCode reason) {
        return new TurkuInternalRenderQueueTransitionResult(false, from, action, null, reason);
    }

    public boolean isAllowed() { return allowed; }
    public TurkuInternalRenderQueueState getFromState() { return fromState; }
    public TurkuInternalRenderQueueAction getAction() { return action; }
    public TurkuInternalRenderQueueState getToState() { return toState; }
    public TurkuInternalRenderQueueReasonCode getReasonCode() { return reasonCode; }
}
