package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Queue action result wrapper.
 */
public final class TurkuInternalRenderQueueActionResult {
    private final boolean allowed;
    private final TurkuInternalRenderQueueSummary summary;
    private final TurkuInternalRenderQueueAction action;
    private final TurkuInternalRenderQueueReasonCode reasonCode;

    private TurkuInternalRenderQueueActionResult(boolean allowed,
            TurkuInternalRenderQueueSummary summary,
            TurkuInternalRenderQueueAction action,
            TurkuInternalRenderQueueReasonCode reasonCode) {
        this.allowed = allowed; this.summary = summary;
        this.action = action; this.reasonCode = reasonCode;
    }

    public static TurkuInternalRenderQueueActionResult allowed(
            TurkuInternalRenderQueueSummary updated,
            TurkuInternalRenderQueueAction action,
            TurkuInternalRenderQueueReasonCode reason) {
        return new TurkuInternalRenderQueueActionResult(true, updated, action, reason);
    }

    public static TurkuInternalRenderQueueActionResult denied(
            TurkuInternalRenderQueueSummary current,
            TurkuInternalRenderQueueAction action,
            TurkuInternalRenderQueueReasonCode reason) {
        return new TurkuInternalRenderQueueActionResult(false, current, action, reason);
    }

    public boolean isAllowed() { return allowed; }
    public TurkuInternalRenderQueueSummary getSummary() { return summary; }
    public TurkuInternalRenderQueueAction getAction() { return action; }
    public TurkuInternalRenderQueueReasonCode getReasonCode() { return reasonCode; }
}
