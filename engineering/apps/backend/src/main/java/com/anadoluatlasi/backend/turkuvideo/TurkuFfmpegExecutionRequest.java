package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-024 Task 04: FFmpeg execution request.
 * Contains only opaque refs and validated metadata — no raw paths.
 */
public final class TurkuFfmpegExecutionRequest {
    private final UUID projectId;
    private final UUID jobId;
    private final TurkuRenderSecureConfig config;
    private final TurkuRenderWorkspaceRef workspaceRef;
    private final TurkuFfmpegCommandPlan commandPlan;
    private final TurkuRenderExecutionPlan renderPlan;

    public TurkuFfmpegExecutionRequest(UUID projectId, UUID jobId,
                                        TurkuRenderSecureConfig config,
                                        TurkuRenderWorkspaceRef workspaceRef,
                                        TurkuFfmpegCommandPlan commandPlan,
                                        TurkuRenderExecutionPlan renderPlan) {
        this.projectId = projectId;
        this.jobId = jobId;
        this.config = config;
        this.workspaceRef = workspaceRef;
        this.commandPlan = commandPlan;
        this.renderPlan = renderPlan;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getJobId() { return jobId; }
    public TurkuRenderSecureConfig getConfig() { return config; }
    public TurkuRenderWorkspaceRef getWorkspaceRef() { return workspaceRef; }
    public TurkuFfmpegCommandPlan getCommandPlan() { return commandPlan; }
    public TurkuRenderExecutionPlan getRenderPlan() { return renderPlan; }
}
