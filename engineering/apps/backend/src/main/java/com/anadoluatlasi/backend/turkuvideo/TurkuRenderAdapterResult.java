package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 02: Render adapter result.
 * No binary, no file path, no URL, no secrets.
 */
public final class TurkuRenderAdapterResult {
    private final boolean success;
    private final TurkuRenderAdapterMode adapterMode;
    private final TurkuRenderAdapterFailureCode failureCode;
    private final String sanitizedMessage;
    private final TurkuRenderAdapterTraceReference traceReference;
    private final TurkuRenderAdapterSafetyEnvelope safetyEnvelope;
    private final long durationMs;

    public TurkuRenderAdapterResult(boolean success, TurkuRenderAdapterMode adapterMode,
                                     TurkuRenderAdapterFailureCode failureCode,
                                     String sanitizedMessage,
                                     TurkuRenderAdapterTraceReference traceReference,
                                     TurkuRenderAdapterSafetyEnvelope safetyEnvelope,
                                     long durationMs) {
        this.success = success;
        this.adapterMode = adapterMode;
        this.failureCode = failureCode;
        this.sanitizedMessage = sanitizedMessage;
        this.traceReference = traceReference;
        this.safetyEnvelope = safetyEnvelope;
        this.durationMs = durationMs;
    }

    public boolean isSuccess() { return success; }
    public TurkuRenderAdapterMode getAdapterMode() { return adapterMode; }
    public TurkuRenderAdapterFailureCode getFailureCode() { return failureCode; }
    public String getSanitizedMessage() { return sanitizedMessage; }
    public TurkuRenderAdapterTraceReference getTraceReference() { return traceReference; }
    public TurkuRenderAdapterSafetyEnvelope getSafetyEnvelope() { return safetyEnvelope; }
    public long getDurationMs() { return durationMs; }
}
