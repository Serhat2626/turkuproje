package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonCreator
    public TurkuRenderAttempt(@JsonProperty("attemptId") UUID attemptId,
                               @JsonProperty("attemptNo") int attemptNo,
                               @JsonProperty("startedAt") Instant startedAt,
                               @JsonProperty("completedAt") Instant completedAt,
                               @JsonProperty("status") TurkuRenderJobStatus status,
                               @JsonProperty("failureCode") TurkuRenderFailureCode failureCode,
                               @JsonProperty("durationMs") long durationMs,
                               @JsonProperty("rendererAlias") String rendererAlias) {
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
