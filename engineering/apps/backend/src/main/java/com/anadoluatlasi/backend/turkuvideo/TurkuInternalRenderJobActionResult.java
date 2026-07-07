package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-025 Task 03: Action result wrapper.
 * Contains updated summary + reason code. No raw exceptions.
 */
public final class TurkuInternalRenderJobActionResult {
    private final boolean allowed;
    private final TurkuInternalRenderJobSummary summary;
    private final TurkuInternalRenderJobAction action;
    private final TurkuInternalRenderJobReasonCode reasonCode;

    private TurkuInternalRenderJobActionResult(boolean allowed,
            TurkuInternalRenderJobSummary summary,
            TurkuInternalRenderJobAction action,
            TurkuInternalRenderJobReasonCode reasonCode) {
        this.allowed = allowed;
        this.summary = summary;
        this.action = action;
        this.reasonCode = reasonCode;
    }

    public static TurkuInternalRenderJobActionResult allowed(
            TurkuInternalRenderJobSummary updated, TurkuInternalRenderJobAction action) {
        return new TurkuInternalRenderJobActionResult(
                true, updated, action, TurkuInternalRenderJobReasonCode.TRANSITION_ALLOWED);
    }

    public static TurkuInternalRenderJobActionResult denied(
            TurkuInternalRenderJobSummary current, TurkuInternalRenderJobAction action,
            TurkuInternalRenderJobReasonCode reason) {
        return new TurkuInternalRenderJobActionResult(false, current, action, reason);
    }

    public boolean isAllowed() { return allowed; }
    public TurkuInternalRenderJobSummary getSummary() { return summary; }
    public TurkuInternalRenderJobAction getAction() { return action; }
    public TurkuInternalRenderJobReasonCode getReasonCode() { return reasonCode; }
}
