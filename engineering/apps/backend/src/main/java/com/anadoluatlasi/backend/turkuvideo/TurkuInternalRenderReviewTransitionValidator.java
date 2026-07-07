package com.anadoluatlasi.backend.turkuvideo;

import java.util.Map;
import java.util.Set;

import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderReviewState.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderReviewAction.*;

/**
 * FP-026 Task 02: Review transition validator.
 * Explicit matrix. Deny-by-default. No auto-approve. No generic status update.
 */
public class TurkuInternalRenderReviewTransitionValidator {

    private record TransitionKey(TurkuInternalRenderReviewState from,
                                  TurkuInternalRenderReviewAction action) {}

    private record TransitionTarget(TurkuInternalRenderReviewState to,
                                     TurkuInternalRenderReviewReasonCode reason) {}

    private static final Map<TransitionKey, TransitionTarget> ALLOWED = Map.ofEntries(
            // Require review
            Map.entry(new TransitionKey(REVIEW_NOT_REQUIRED, REQUIRE_HUMAN_REVIEW),
                    new TransitionTarget(REVIEW_REQUIRED, TurkuInternalRenderReviewReasonCode.REVIEW_REQUIRED)),
            // Mark ready
            Map.entry(new TransitionKey(REVIEW_REQUIRED, MARK_READY_FOR_REVIEW),
                    new TransitionTarget(READY_FOR_REVIEW, TurkuInternalRenderReviewReasonCode.READY_FOR_REVIEW)),
            // Start review
            Map.entry(new TransitionKey(READY_FOR_REVIEW, START_HUMAN_REVIEW),
                    new TransitionTarget(IN_REVIEW, TurkuInternalRenderReviewReasonCode.REVIEW_STARTED)),
            // Approve (internal only)
            Map.entry(new TransitionKey(IN_REVIEW, APPROVE_FOR_INTERNAL_NEXT_STEP),
                    new TransitionTarget(APPROVED_FOR_INTERNAL_NEXT_STEP, TurkuInternalRenderReviewReasonCode.APPROVED_INTERNAL_ONLY)),
            // Reject
            Map.entry(new TransitionKey(IN_REVIEW, REJECT_HUMAN_REVIEW),
                    new TransitionTarget(REJECTED_BY_HUMAN_REVIEW, TurkuInternalRenderReviewReasonCode.REJECTED_BY_REVIEWER)),
            // Block for safety from multiple states
            Map.entry(new TransitionKey(REVIEW_REQUIRED, BLOCK_FOR_SAFETY),
                    new TransitionTarget(BLOCKED_BY_SAFETY, TurkuInternalRenderReviewReasonCode.BLOCKED_BY_SAFETY)),
            Map.entry(new TransitionKey(READY_FOR_REVIEW, BLOCK_FOR_SAFETY),
                    new TransitionTarget(BLOCKED_BY_SAFETY, TurkuInternalRenderReviewReasonCode.BLOCKED_BY_SAFETY)),
            Map.entry(new TransitionKey(IN_REVIEW, BLOCK_FOR_SAFETY),
                    new TransitionTarget(BLOCKED_BY_SAFETY, TurkuInternalRenderReviewReasonCode.BLOCKED_BY_SAFETY)),
            // Expire from multiple states
            Map.entry(new TransitionKey(REVIEW_REQUIRED, EXPIRE_HUMAN_REVIEW),
                    new TransitionTarget(EXPIRED_REVIEW, TurkuInternalRenderReviewReasonCode.REVIEW_EXPIRED)),
            Map.entry(new TransitionKey(READY_FOR_REVIEW, EXPIRE_HUMAN_REVIEW),
                    new TransitionTarget(EXPIRED_REVIEW, TurkuInternalRenderReviewReasonCode.REVIEW_EXPIRED)),
            Map.entry(new TransitionKey(IN_REVIEW, EXPIRE_HUMAN_REVIEW),
                    new TransitionTarget(EXPIRED_REVIEW, TurkuInternalRenderReviewReasonCode.REVIEW_EXPIRED))
    );

    /**
     * Validate a review transition. Deny-by-default.
     */
    public TurkuInternalRenderReviewTransitionResult validate(
            TurkuInternalRenderReviewState currentState,
            TurkuInternalRenderReviewAction action) {

        if (currentState == null) {
            return TurkuInternalRenderReviewTransitionResult.denied(
                    null, action, TurkuInternalRenderReviewReasonCode.REVIEW_REF_INVALID);
        }
        if (action == null) {
            return TurkuInternalRenderReviewTransitionResult.denied(
                    currentState, null, TurkuInternalRenderReviewReasonCode.TRANSITION_NOT_ALLOWED);
        }

        // Terminal states deny all transitions
        if (currentState.isTerminal()) {
            return TurkuInternalRenderReviewTransitionResult.denied(
                    currentState, action, TurkuInternalRenderReviewReasonCode.TERMINAL_STATE);
        }

        var key = new TransitionKey(currentState, action);
        var target = ALLOWED.get(key);
        if (target != null) {
            return TurkuInternalRenderReviewTransitionResult.allowed(
                    currentState, action, target.to(), target.reason());
        }

        return TurkuInternalRenderReviewTransitionResult.denied(
                currentState, action, TurkuInternalRenderReviewReasonCode.TRANSITION_NOT_ALLOWED);
    }

    public int getAllowedTransitionCount() {
        return ALLOWED.size();
    }

    public Set<TransitionKey> getAllowedTransitions() {
        return ALLOWED.keySet();
    }
}
