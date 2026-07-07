package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-023 Task 02: Render adapter request.
 * Contains only safe internal references. No file paths, URLs, or secrets.
 */
public final class TurkuRenderAdapterRequest {
    private final UUID projectId;
    private final UUID jobId;
    private final TurkuRenderExecutionPlan executionPlan;
    private final TurkuRenderAdapterMode requestedMode;

    public TurkuRenderAdapterRequest(UUID projectId, UUID jobId,
                                      TurkuRenderExecutionPlan executionPlan,
                                      TurkuRenderAdapterMode requestedMode) {
        this.projectId = projectId;
        this.jobId = jobId;
        this.executionPlan = executionPlan;
        this.requestedMode = requestedMode;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getJobId() { return jobId; }
    public TurkuRenderExecutionPlan getExecutionPlan() { return executionPlan; }
    public TurkuRenderAdapterMode getRequestedMode() { return requestedMode; }
}
