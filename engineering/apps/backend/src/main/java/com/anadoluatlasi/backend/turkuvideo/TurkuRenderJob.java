package com.anadoluatlasi.backend.turkuvideo;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TurkuRenderJob {
    private final UUID jobId;
    private final UUID projectId;
    private final String manifestRef;
    private TurkuRenderJobStatus status;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<TurkuRenderAttempt> attempts;
    private TurkuRenderArtifact artifact;
    private final List<String> reviewFlags;

    public TurkuRenderJob(UUID jobId, UUID projectId, String manifestRef,
                           List<String> reviewFlags) {
        this.jobId = jobId;
        this.projectId = projectId;
        this.manifestRef = manifestRef;
        this.status = TurkuRenderJobStatus.PREPARED;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.attempts = new ArrayList<>();
        this.artifact = null;
        this.reviewFlags = reviewFlags != null ? new ArrayList<>(reviewFlags) : new ArrayList<>();
    }

    public UUID getJobId() { return jobId; }
    public UUID getProjectId() { return projectId; }
    public String getManifestRef() { return manifestRef; }
    public TurkuRenderJobStatus getStatus() { return status; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<TurkuRenderAttempt> getAttempts() { return List.copyOf(attempts); }
    public TurkuRenderArtifact getArtifact() { return artifact; }
    public List<String> getReviewFlags() { return List.copyOf(reviewFlags); }

    public void setStatus(TurkuRenderJobStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    public void addAttempt(TurkuRenderAttempt attempt) {
        this.attempts.add(attempt);
        this.updatedAt = Instant.now();
    }
    public void setArtifact(TurkuRenderArtifact artifact) {
        this.artifact = artifact;
        this.updatedAt = Instant.now();
    }
}
