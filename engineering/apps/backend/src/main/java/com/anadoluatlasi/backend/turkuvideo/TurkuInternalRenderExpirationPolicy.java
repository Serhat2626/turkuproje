package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Bounded expiration policy value object.
 */
public final class TurkuInternalRenderExpirationPolicy {
    private final int maxQueueDurationSeconds;
    private final int maxRetryWaitDurationSeconds;

    public TurkuInternalRenderExpirationPolicy(int maxQueueDurationSeconds, int maxRetryWaitDurationSeconds) {
        this.maxQueueDurationSeconds = Math.max(60, Math.min(7200, maxQueueDurationSeconds));
        this.maxRetryWaitDurationSeconds = Math.max(60, Math.min(3600, maxRetryWaitDurationSeconds));
    }

    public static TurkuInternalRenderExpirationPolicy defaults() {
        return new TurkuInternalRenderExpirationPolicy(3600, 1800);
    }

    public int getMaxQueueDurationSeconds() { return maxQueueDurationSeconds; }
    public int getMaxRetryWaitDurationSeconds() { return maxRetryWaitDurationSeconds; }
}
