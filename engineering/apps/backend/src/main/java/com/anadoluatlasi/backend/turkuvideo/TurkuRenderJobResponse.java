package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

public final class TurkuRenderJobResponse {
    private final UUID projectId;
    private final UUID jobId;
    private final TurkuRenderJobStatus status;
    private final TurkuVideoWorkflowStage currentStage;
    private final String manifestRef;
    private final int attemptCount;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> reviewFlags;
    private final String summary;

    public TurkuRenderJobResponse(UUID projectId, UUID jobId, TurkuRenderJobStatus status,
                                    TurkuVideoWorkflowStage currentStage, String manifestRef,
                                    int attemptCount, List<String> reviewFlags, String summary) {
        this.projectId = projectId;
        this.jobId = jobId;
        this.status = status;
        this.currentStage = currentStage;
        this.manifestRef = manifestRef;
        this.attemptCount = attemptCount;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
        this.summary = summary;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getJobId() { return jobId; }
    public TurkuRenderJobStatus getStatus() { return status; }
    public TurkuVideoWorkflowStage getCurrentStage() { return currentStage; }
    public String getManifestRef() { return manifestRef; }
    public int getAttemptCount() { return attemptCount; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getReviewFlags() { return reviewFlags; }
    public String getSummary() { return summary; }
}
