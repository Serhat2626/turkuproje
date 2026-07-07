package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-022 Task 03: Render execution response DTO.
 * No rendered video. No downloadable file. No public/signed/CDN URL.
 */
public final class TurkuRenderExecutionResponse {
    private final UUID projectId;
    private final UUID jobId;
    private final UUID attemptId;
    private final TurkuRenderJobStatus status;
    private final TurkuVideoWorkflowStage currentStage;
    private final String safeTraceRef;
    private final String artifactRef;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> reviewFlags;
    private final String summary;
    private final TurkuRenderFailureCode failureCode;

    public TurkuRenderExecutionResponse(UUID projectId, UUID jobId, UUID attemptId,
                                          TurkuRenderJobStatus status,
                                          TurkuVideoWorkflowStage currentStage,
                                          String safeTraceRef, String artifactRef,
                                          List<String> reviewFlags, String summary,
                                          TurkuRenderFailureCode failureCode) {
        this.projectId = projectId;
        this.jobId = jobId;
        this.attemptId = attemptId;
        this.status = status;
        this.currentStage = currentStage;
        this.safeTraceRef = safeTraceRef;
        this.artifactRef = artifactRef;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
        this.summary = summary;
        this.failureCode = failureCode;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getJobId() { return jobId; }
    public UUID getAttemptId() { return attemptId; }
    public TurkuRenderJobStatus getStatus() { return status; }
    public TurkuVideoWorkflowStage getCurrentStage() { return currentStage; }
    public String getSafeTraceRef() { return safeTraceRef; }
    public String getArtifactRef() { return artifactRef; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getReviewFlags() { return reviewFlags; }
    public String getSummary() { return summary; }
    public TurkuRenderFailureCode getFailureCode() { return failureCode; }
}
