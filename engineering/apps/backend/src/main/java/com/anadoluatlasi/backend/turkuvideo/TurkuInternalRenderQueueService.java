package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-027 Task 02: Internal render queue service.
 * Stateless. No DB, no filesystem, no provider, no auto-approval.
 */
public class TurkuInternalRenderQueueService {

    private final TurkuInternalRenderQueueTransitionValidator transitionValidator;

    public TurkuInternalRenderQueueService() {
        this.transitionValidator = new TurkuInternalRenderQueueTransitionValidator();
    }

    public TurkuInternalRenderQueueService(TurkuInternalRenderQueueTransitionValidator validator) {
        this.transitionValidator = validator;
    }

    /**
     * Create initial queue summary in NOT_QUEUED state.
     */
    public TurkuInternalRenderQueueSummary createQueueItem(String jobRef, String reviewRef) {
        var ref = TurkuInternalRenderQueueItemRef.create();
        return new TurkuInternalRenderQueueSummary(
                ref.toOpaqueRef(), jobRef, reviewRef,
                TurkuInternalRenderQueueState.NOT_QUEUED,
                TurkuInternalRenderQueueReasonCode.NOT_QUEUED,
                0, 3, false, false);
    }

    /**
     * Apply explicit queue action. No generic setStatus/updateStatus.
     */
    public TurkuInternalRenderQueueActionResult applyAction(
            TurkuInternalRenderQueueSummary current, TurkuInternalRenderQueueAction action) {
        if (current == null)
            return TurkuInternalRenderQueueActionResult.denied(null, action,
                    TurkuInternalRenderQueueReasonCode.QUEUE_REF_INVALID);

        var result = transitionValidator.validate(current.getQueueState(), action);
        if (!result.isAllowed())
            return TurkuInternalRenderQueueActionResult.denied(current, action, result.getReasonCode());

        int attempt = current.getAttemptNumber();
        if (action == TurkuInternalRenderQueueAction.MARK_RETRY_WAIT) attempt++;

        var updated = new TurkuInternalRenderQueueSummary(
                current.getQueueItemRef(), current.getJobRef(), current.getReviewRef(),
                result.getToState(), result.getReasonCode(),
                attempt, current.getMaxAttempts(),
                attempt < current.getMaxAttempts()
                        && result.getToState() != TurkuInternalRenderQueueState.FAILED_INTERNAL,
                result.getToState() == TurkuInternalRenderQueueState.EXPIRED_INTERNAL);
        return TurkuInternalRenderQueueActionResult.allowed(updated, action, result.getReasonCode());
    }

    /**
     * Check dispatch readiness using FP-025 + FP-026 snapshots.
     */
    public boolean isDispatchReady(
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain,
            TurkuInternalRenderReviewState reviewState) {
        boolean jobReady = readiness != null
                && readiness.getStatus() == TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY;
        boolean chainSafe = safetyChain != null
                && safetyChain.isConfigValid()
                && safetyChain.isWorkspaceSafe()
                && safetyChain.isExecutionBoundarySafe()
                && safetyChain.isArtifactVerified();
        boolean reviewApproved = reviewState == TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP;
        return jobReady && chainSafe && reviewApproved;
    }
}
