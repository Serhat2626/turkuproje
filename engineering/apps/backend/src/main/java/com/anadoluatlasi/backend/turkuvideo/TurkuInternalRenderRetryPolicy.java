package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Bounded retry policy value object.
 */
public final class TurkuInternalRenderRetryPolicy {
    private final int maxAttempts;
    private final int retryDelaySeconds;

    public TurkuInternalRenderRetryPolicy(int maxAttempts, int retryDelaySeconds) {
        this.maxAttempts = Math.max(0, Math.min(5, maxAttempts));
        this.retryDelaySeconds = Math.max(30, Math.min(600, retryDelaySeconds));
    }

    public static TurkuInternalRenderRetryPolicy defaults() {
        return new TurkuInternalRenderRetryPolicy(3, 60);
    }

    public int getMaxAttempts() { return maxAttempts; }
    public int getRetryDelaySeconds() { return retryDelaySeconds; }
}
