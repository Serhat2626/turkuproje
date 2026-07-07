package com.anadoluatlasi.backend.turkuvideo;

import java.time.Instant;
import java.util.UUID;

public final class TurkuRenderAttempt {
    private final UUID attemptId;
    private final int attemptNo;
    private final Instant startedAt;
    private final Instant completedAt;
    private final TurkuRenderJobStatus status;
    private final TurkuRenderFailureCode failureCode;
    private final long durationMs;
    private final String rendererAlias;

    public TurkuRenderAttempt(UUID attemptId, int attemptNo, Instant startedAt,
                               Instant completedAt, TurkuRenderJobStatus status,
                               TurkuRenderFailureCode failureCode, long durationMs,
                               String rendererAlias) {
        this.attemptId = attemptId;
        this.attemptNo = attemptNo;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.status = status;
        this.failureCode = failureCode;
        this.durationMs = durationMs;
        this.rendererAlias = rendererAlias;
    }

    public UUID getAttemptId() { return attemptId; }
    public int getAttemptNo() { return attemptNo; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public TurkuRenderJobStatus getStatus() { return status; }
    public TurkuRenderFailureCode getFailureCode() { return failureCode; }
    public long getDurationMs() { return durationMs; }
    public String getRendererAlias() { return rendererAlias; }
}
