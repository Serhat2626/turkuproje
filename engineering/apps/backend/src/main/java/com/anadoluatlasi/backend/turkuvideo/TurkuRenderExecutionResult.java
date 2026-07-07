package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-022 Task 03: Render execution result.
 * No binary data. No file path. No public URL.
 */
public final class TurkuRenderExecutionResult {
    private final boolean success;
    private final long durationMs;
    private final TurkuRenderFailureCode failureCode;
    private final String failureMessage;
    private final TurkuRenderArtifact artifact;
    private final String safeTraceRef;

    public TurkuRenderExecutionResult(boolean success, long durationMs,
                                       TurkuRenderFailureCode failureCode,
                                       String failureMessage,
                                       TurkuRenderArtifact artifact,
                                       String safeTraceRef) {
        this.success = success;
        this.durationMs = durationMs;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.artifact = artifact;
        this.safeTraceRef = safeTraceRef;
    }

    public boolean isSuccess() { return success; }
    public long getDurationMs() { return durationMs; }
    public TurkuRenderFailureCode getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public TurkuRenderArtifact getArtifact() { return artifact; }
    public String getSafeTraceRef() { return safeTraceRef; }
}
