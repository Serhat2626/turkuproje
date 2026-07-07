package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-022 Task 03: Render execution safety response DTO.
 * Read-only safety status. No rendered/binary/URL data.
 */
public final class TurkuRenderExecutionSafetyResponse {
    private final UUID projectId;
    private final UUID jobId;
    private final TurkuRenderJobStatus jobStatus;
    private final TurkuVideoWorkflowStage currentStage;
    private final TurkuRenderExecutionSafetyEnvelope safetyEnvelope;
    private final String summary;

    public TurkuRenderExecutionSafetyResponse(UUID projectId, UUID jobId,
                                                TurkuRenderJobStatus jobStatus,
                                                TurkuVideoWorkflowStage currentStage,
                                                TurkuRenderExecutionSafetyEnvelope safetyEnvelope,
                                                String summary) {
        this.projectId = projectId;
        this.jobId = jobId;
        this.jobStatus = jobStatus;
        this.currentStage = currentStage;
        this.safetyEnvelope = safetyEnvelope;
        this.summary = summary;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getJobId() { return jobId; }
    public TurkuRenderJobStatus getJobStatus() { return jobStatus; }
    public TurkuVideoWorkflowStage getCurrentStage() { return currentStage; }
    public TurkuRenderExecutionSafetyEnvelope getSafetyEnvelope() { return safetyEnvelope; }
    public String getSummary() { return summary; }
}
