package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Retry snapshot.
 */
public final class TurkuInternalRenderRetrySnapshot {
    private final int currentAttempt;
    private final int maxAttempts;
    private final int retryDelaySeconds;
    private final boolean retriable;
    private final TurkuInternalRenderRetryReasonCode reasonCode;

    public TurkuInternalRenderRetrySnapshot(int currentAttempt, TurkuInternalRenderRetryPolicy policy,
            TurkuInternalRenderRetryReasonCode reasonCode) {
        this.currentAttempt = Math.max(0, Math.min(5, currentAttempt));
        this.maxAttempts = policy.getMaxAttempts();
        this.retryDelaySeconds = policy.getRetryDelaySeconds();
        this.retriable = currentAttempt < maxAttempts
                && reasonCode != TurkuInternalRenderRetryReasonCode.NOT_RETRIABLE_PERMANENT
                && reasonCode != TurkuInternalRenderRetryReasonCode.MAX_RETRIES_EXHAUSTED
                && reasonCode != TurkuInternalRenderRetryReasonCode.CANCELLED_NO_RETRY
                && reasonCode != TurkuInternalRenderRetryReasonCode.EXPIRED_NO_RETRY;
        this.reasonCode = reasonCode;
    }

    public int getCurrentAttempt() { return currentAttempt; }
    public int getMaxAttempts() { return maxAttempts; }
    public int getRetryDelaySeconds() { return retryDelaySeconds; }
    public boolean isRetriable() { return retriable; }
    public TurkuInternalRenderRetryReasonCode getReasonCode() { return reasonCode; }
}
