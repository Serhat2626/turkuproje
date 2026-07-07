package com.anadoluatlasi.backend.turkuvideo;

import java.util.Map;

import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderQueueState.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderQueueAction.*;

/**
 * FP-027 Task 02: Queue transition validator. 17-transition matrix. Deny-by-default.
 */
public class TurkuInternalRenderQueueTransitionValidator {

    private record TK(TurkuInternalRenderQueueState from, TurkuInternalRenderQueueAction action) {}
    private record TT(TurkuInternalRenderQueueState to, TurkuInternalRenderQueueReasonCode reason) {}

    private static final Map<TK, TT> ALLOWED = Map.ofEntries(
            // Happy path
            Map.entry(new TK(NOT_QUEUED, MARK_QUEUE_READY),
                    new TT(QUEUE_READY, TurkuInternalRenderQueueReasonCode.QUEUE_READY)),
            Map.entry(new TK(QUEUE_READY, ENQUEUE_INTERNAL),
                    new TT(QUEUED_INTERNAL, TurkuInternalRenderQueueReasonCode.ENQUEUED)),
            Map.entry(new TK(QUEUED_INTERNAL, DISPATCH_INTERNAL),
                    new TT(DISPATCHING_INTERNAL, TurkuInternalRenderQueueReasonCode.DISPATCHING)),
            Map.entry(new TK(DISPATCHING_INTERNAL, MARK_IN_PROGRESS),
                    new TT(IN_PROGRESS_INTERNAL, TurkuInternalRenderQueueReasonCode.IN_PROGRESS)),
            Map.entry(new TK(IN_PROGRESS_INTERNAL, MARK_SUCCEEDED),
                    new TT(SUCCEEDED_INTERNAL, TurkuInternalRenderQueueReasonCode.SUCCEEDED)),
            Map.entry(new TK(IN_PROGRESS_INTERNAL, MARK_FAILED),
                    new TT(FAILED_INTERNAL, TurkuInternalRenderQueueReasonCode.FAILED_PERMANENT)),
            // Retry
            Map.entry(new TK(IN_PROGRESS_INTERNAL, MARK_RETRY_WAIT),
                    new TT(RETRY_WAIT_INTERNAL, TurkuInternalRenderQueueReasonCode.RETRY_WAIT)),
            Map.entry(new TK(RETRY_WAIT_INTERNAL, ENQUEUE_INTERNAL),
                    new TT(QUEUED_INTERNAL, TurkuInternalRenderQueueReasonCode.ENQUEUED)),
            // Cancel from multiple states
            Map.entry(new TK(NOT_QUEUED, CANCEL_INTERNAL),
                    new TT(CANCELLED_INTERNAL, TurkuInternalRenderQueueReasonCode.CANCELLED)),
            Map.entry(new TK(QUEUE_READY, CANCEL_INTERNAL),
                    new TT(CANCELLED_INTERNAL, TurkuInternalRenderQueueReasonCode.CANCELLED)),
            Map.entry(new TK(QUEUED_INTERNAL, CANCEL_INTERNAL),
                    new TT(CANCELLED_INTERNAL, TurkuInternalRenderQueueReasonCode.CANCELLED)),
            Map.entry(new TK(DISPATCHING_INTERNAL, CANCEL_INTERNAL),
                    new TT(CANCELLED_INTERNAL, TurkuInternalRenderQueueReasonCode.CANCELLED)),
            Map.entry(new TK(IN_PROGRESS_INTERNAL, CANCEL_INTERNAL),
                    new TT(CANCELLED_INTERNAL, TurkuInternalRenderQueueReasonCode.CANCELLED)),
            Map.entry(new TK(RETRY_WAIT_INTERNAL, CANCEL_INTERNAL),
                    new TT(CANCELLED_INTERNAL, TurkuInternalRenderQueueReasonCode.CANCELLED)),
            // Expire from waiting states
            Map.entry(new TK(QUEUE_READY, EXPIRE_INTERNAL),
                    new TT(EXPIRED_INTERNAL, TurkuInternalRenderQueueReasonCode.EXPIRED)),
            Map.entry(new TK(QUEUED_INTERNAL, EXPIRE_INTERNAL),
                    new TT(EXPIRED_INTERNAL, TurkuInternalRenderQueueReasonCode.EXPIRED)),
            Map.entry(new TK(RETRY_WAIT_INTERNAL, EXPIRE_INTERNAL),
                    new TT(EXPIRED_INTERNAL, TurkuInternalRenderQueueReasonCode.EXPIRED))
    );

    public TurkuInternalRenderQueueTransitionResult validate(
            TurkuInternalRenderQueueState currentState, TurkuInternalRenderQueueAction action) {
        if (currentState == null)
            return TurkuInternalRenderQueueTransitionResult.denied(null, action,
                    TurkuInternalRenderQueueReasonCode.QUEUE_REF_INVALID);
        if (action == null)
            return TurkuInternalRenderQueueTransitionResult.denied(currentState, null,
                    TurkuInternalRenderQueueReasonCode.TRANSITION_NOT_ALLOWED);
        if (currentState.isTerminal())
            return TurkuInternalRenderQueueTransitionResult.denied(currentState, action,
                    TurkuInternalRenderQueueReasonCode.TERMINAL_STATE);

        var target = ALLOWED.get(new TK(currentState, action));
        if (target != null)
            return TurkuInternalRenderQueueTransitionResult.allowed(currentState, action, target.to(), target.reason());

        return TurkuInternalRenderQueueTransitionResult.denied(currentState, action,
                TurkuInternalRenderQueueReasonCode.TRANSITION_NOT_ALLOWED);
    }

    public int getAllowedTransitionCount() { return ALLOWED.size(); }
}
