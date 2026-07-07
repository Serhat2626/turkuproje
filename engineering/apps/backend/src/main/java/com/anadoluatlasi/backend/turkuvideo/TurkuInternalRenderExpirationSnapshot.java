package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Expiration snapshot.
 */
public final class TurkuInternalRenderExpirationSnapshot {
    private final boolean expired;
    private final TurkuInternalRenderExpirationReasonCode reasonCode;
    private final int maxQueueDurationSeconds;
    private final int maxRetryWaitDurationSeconds;

    public TurkuInternalRenderExpirationSnapshot(boolean expired,
            TurkuInternalRenderExpirationReasonCode reasonCode,
            TurkuInternalRenderExpirationPolicy policy) {
        this.expired = expired;
        this.reasonCode = reasonCode;
        this.maxQueueDurationSeconds = policy.getMaxQueueDurationSeconds();
        this.maxRetryWaitDurationSeconds = policy.getMaxRetryWaitDurationSeconds();
    }

    public boolean isExpired() { return expired; }
    public TurkuInternalRenderExpirationReasonCode getReasonCode() { return reasonCode; }
    public int getMaxQueueDurationSeconds() { return maxQueueDurationSeconds; }
    public int getMaxRetryWaitDurationSeconds() { return maxRetryWaitDurationSeconds; }
}
