package com.anadoluatlasi.backend.turkuvideo;

import java.util.Map;
import java.util.Set;

import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderJobStatus.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderJobAction.*;

/**
 * FP-025 Task 02: Transition validator.
 * Explicit matrix. Deny-by-default. No generic status update.
 */
public class TurkuInternalRenderJobTransitionValidator {

    // Explicit transition matrix: (from, action) -> to
    private static final Map<TransitionKey, TurkuInternalRenderJobStatus> ALLOWED = Map.ofEntries(
            // Normal forward flow
            Map.entry(key(DRAFT, VALIDATE_FOR_INTERNAL_RENDER), READY_FOR_INTERNAL_RENDER),
            Map.entry(key(READY_FOR_INTERNAL_RENDER, ENQUEUE_INTERNAL_RENDER), QUEUED_INTERNAL),
            Map.entry(key(QUEUED_INTERNAL, START_INTERNAL_RENDER), RUNNING_INTERNAL),
            Map.entry(key(RUNNING_INTERNAL, COMPLETE_INTERNAL_RENDER), SUCCEEDED_INTERNAL),
            Map.entry(key(RUNNING_INTERNAL, FAIL_INTERNAL_RENDER), FAILED_INTERNAL),
            // Cancel from any non-terminal active state
            Map.entry(key(DRAFT, CANCEL_INTERNAL_RENDER), CANCELLED_INTERNAL),
            Map.entry(key(READY_FOR_INTERNAL_RENDER, CANCEL_INTERNAL_RENDER), CANCELLED_INTERNAL),
            Map.entry(key(QUEUED_INTERNAL, CANCEL_INTERNAL_RENDER), CANCELLED_INTERNAL),
            Map.entry(key(RUNNING_INTERNAL, CANCEL_INTERNAL_RENDER), CANCELLED_INTERNAL),
            // Expire from any non-terminal state
            Map.entry(key(DRAFT, EXPIRE_INTERNAL_RENDER), EXPIRED_INTERNAL),
            Map.entry(key(READY_FOR_INTERNAL_RENDER, EXPIRE_INTERNAL_RENDER), EXPIRED_INTERNAL),
            Map.entry(key(QUEUED_INTERNAL, EXPIRE_INTERNAL_RENDER), EXPIRED_INTERNAL),
            Map.entry(key(RUNNING_INTERNAL, EXPIRE_INTERNAL_RENDER), EXPIRED_INTERNAL),
            Map.entry(key(SUCCEEDED_INTERNAL, EXPIRE_INTERNAL_RENDER), EXPIRED_INTERNAL)
    );

    /**
     * Validate a transition. Deny-by-default.
     */
    public TurkuInternalRenderJobTransitionResult validate(
            TurkuInternalRenderJobStatus currentState,
            TurkuInternalRenderJobAction action) {

        if (currentState == null) {
            return TurkuInternalRenderJobTransitionResult.denied(
                    null, action, TurkuInternalRenderJobReasonCode.UNKNOWN_CURRENT_STATE);
        }
        if (action == null) {
            return TurkuInternalRenderJobTransitionResult.denied(
                    currentState, null, TurkuInternalRenderJobReasonCode.UNKNOWN_ACTION);
        }

        // Terminal states deny all transitions except EXPIRE from SUCCEEDED
        if (currentState.isTerminal()) {
            return TurkuInternalRenderJobTransitionResult.denied(
                    currentState, action, TurkuInternalRenderJobReasonCode.TERMINAL_STATE);
        }

        var key = key(currentState, action);
        var target = ALLOWED.get(key);
        if (target != null) {
            return TurkuInternalRenderJobTransitionResult.allowed(currentState, action, target);
        }

        return TurkuInternalRenderJobTransitionResult.denied(
                currentState, action, TurkuInternalRenderJobReasonCode.TRANSITION_NOT_ALLOWED);
    }

    /**
     * Get all allowed transitions for reference/testing.
     */
    public Set<TransitionKey> getAllowedTransitions() {
        return ALLOWED.keySet();
    }

    public int getAllowedTransitionCount() {
        return ALLOWED.size();
    }

    // ── Internal key ──

    private static TransitionKey key(TurkuInternalRenderJobStatus from,
                                      TurkuInternalRenderJobAction action) {
        return new TransitionKey(from, action);
    }

    public record TransitionKey(TurkuInternalRenderJobStatus from,
                                 TurkuInternalRenderJobAction action) {}
}
