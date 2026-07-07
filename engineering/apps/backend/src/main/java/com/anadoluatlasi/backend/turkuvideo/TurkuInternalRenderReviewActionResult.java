package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-026 Task 03: Review action result wrapper.
 * Contains updated summary + safe reason. No raw exceptions.
 */
public final class TurkuInternalRenderReviewActionResult {
    private final boolean allowed;
    private final TurkuInternalRenderReviewSummary summary;
    private final TurkuInternalRenderReviewAction action;
    private final TurkuInternalRenderReviewReasonCode reasonCode;

    private TurkuInternalRenderReviewActionResult(boolean allowed,
            TurkuInternalRenderReviewSummary summary,
            TurkuInternalRenderReviewAction action,
            TurkuInternalRenderReviewReasonCode reasonCode) {
        this.allowed = allowed;
        this.summary = summary;
        this.action = action;
        this.reasonCode = reasonCode;
    }

    public static TurkuInternalRenderReviewActionResult allowed(
            TurkuInternalRenderReviewSummary updated,
            TurkuInternalRenderReviewAction action,
            TurkuInternalRenderReviewReasonCode reason) {
        return new TurkuInternalRenderReviewActionResult(true, updated, action, reason);
    }

    public static TurkuInternalRenderReviewActionResult denied(
            TurkuInternalRenderReviewSummary current,
            TurkuInternalRenderReviewAction action,
            TurkuInternalRenderReviewReasonCode reason) {
        return new TurkuInternalRenderReviewActionResult(false, current, action, reason);
    }

    public boolean isAllowed() { return allowed; }
    public TurkuInternalRenderReviewSummary getSummary() { return summary; }
    public TurkuInternalRenderReviewAction getAction() { return action; }
    public TurkuInternalRenderReviewReasonCode getReasonCode() { return reasonCode; }
}
