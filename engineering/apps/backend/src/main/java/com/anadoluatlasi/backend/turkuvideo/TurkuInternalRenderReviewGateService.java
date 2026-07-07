package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-026 Task 03: Review gate service.
 * Stateless. No persistence, no filesystem, no provider, no auto-approval.
 */
public class TurkuInternalRenderReviewGateService {

    private final TurkuInternalRenderReviewTransitionValidator transitionValidator;

    public TurkuInternalRenderReviewGateService() {
        this.transitionValidator = new TurkuInternalRenderReviewTransitionValidator();
    }

    public TurkuInternalRenderReviewGateService(TurkuInternalRenderReviewTransitionValidator validator) {
        this.transitionValidator = validator;
    }

    // ── Review Creation ──

    /**
     * Create initial review summary in REVIEW_NOT_REQUIRED state.
     */
    public TurkuInternalRenderReviewSummary createReview(String jobRef) {
        var reviewRef = TurkuInternalRenderReviewRef.create();
        return new TurkuInternalRenderReviewSummary(
                reviewRef.toOpaqueRef(), jobRef,
                TurkuInternalRenderReviewState.REVIEW_NOT_REQUIRED,
                TurkuInternalRenderReviewReasonCode.REVIEW_NOT_REQUIRED);
    }

    // ── Explicit Action Dispatcher ──

    /**
     * Apply explicit review action. No generic setStatus/updateStatus.
     */
    public TurkuInternalRenderReviewActionResult applyAction(
            TurkuInternalRenderReviewSummary current,
            TurkuInternalRenderReviewAction action) {

        if (current == null) {
            return TurkuInternalRenderReviewActionResult.denied(
                    null, action, TurkuInternalRenderReviewReasonCode.REVIEW_REF_INVALID);
        }

        var transitionResult = transitionValidator.validate(current.getReviewState(), action);

        if (!transitionResult.isAllowed()) {
            return TurkuInternalRenderReviewActionResult.denied(
                    current, action, transitionResult.getReasonCode());
        }

        var updated = new TurkuInternalRenderReviewSummary(
                current.getReviewRef(), current.getJobRef(),
                transitionResult.getToState(), transitionResult.getReasonCode());

        return TurkuInternalRenderReviewActionResult.allowed(
                updated, action, transitionResult.getReasonCode());
    }

    // ── FP-025 Readiness Integration ──

    /**
     * Derive review readiness from FP-025 snapshots.
     */
    public TurkuInternalRenderReviewReadinessSnapshot reviewReadiness(
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain) {
        return TurkuInternalRenderReviewReadinessSnapshot.fromFP025(readiness, safetyChain);
    }
}
