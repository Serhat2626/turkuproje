package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-024 Task 03: Composite workspace model.
 * Groups workspace ref, policy, file refs, cleanup metadata.
 * No filesystem access. No real files.
 */
public final class TurkuRenderWorkspace {
    private final TurkuRenderWorkspaceRef workspaceRef;
    private final TurkuRenderWorkspacePolicy policy;
    private final UUID projectId;
    private final UUID jobId;
    private final boolean cleanupEligible;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;

    public TurkuRenderWorkspace(TurkuRenderWorkspaceRef workspaceRef,
                                 TurkuRenderWorkspacePolicy policy,
                                 UUID projectId, UUID jobId) {
        this.workspaceRef = workspaceRef;
        this.policy = policy;
        this.projectId = projectId;
        this.jobId = jobId;
        this.cleanupEligible = true;
        this.humanReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
    }

    public TurkuRenderWorkspaceRef getWorkspaceRef() { return workspaceRef; }
    public TurkuRenderWorkspacePolicy getPolicy() { return policy; }
    public UUID getProjectId() { return projectId; }
    public UUID getJobId() { return jobId; }
    public boolean isCleanupEligible() { return cleanupEligible; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
}
