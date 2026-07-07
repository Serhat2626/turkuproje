package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-021 Task 03: Render readiness report.
 * Internal readiness only — NOT publish/export readiness.
 */
public final class TurkuRenderReadinessReport {

    private final UUID projectId;
    private final TurkuRenderReadinessStatus status;
    private final boolean compositionReady;
    private final boolean manifestReady;
    private final boolean allScenesHaveImages;
    private final boolean allNarrationPresent;
    private final boolean timingPlanComplete;
    private final boolean copyrightReviewDone;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;
    private final int readinessScore;
    private final List<TurkuRenderReadinessIssue> issues;
    private final List<String> reviewFlags;

    public TurkuRenderReadinessReport(UUID projectId, TurkuRenderReadinessStatus status,
                                       boolean compositionReady, boolean manifestReady,
                                       boolean allScenesHaveImages, boolean allNarrationPresent,
                                       boolean timingPlanComplete, boolean copyrightReviewDone,
                                       int readinessScore,
                                       List<TurkuRenderReadinessIssue> issues,
                                       List<String> reviewFlags) {
        this.projectId = projectId;
        this.status = status;
        this.compositionReady = compositionReady;
        this.manifestReady = manifestReady;
        this.allScenesHaveImages = allScenesHaveImages;
        this.allNarrationPresent = allNarrationPresent;
        this.timingPlanComplete = timingPlanComplete;
        this.copyrightReviewDone = copyrightReviewDone;
        this.humanReviewRequired = true;
        this.autoApproved = false;
        this.readinessScore = readinessScore;
        this.issues = issues != null ? List.copyOf(issues) : List.of();
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getProjectId() { return projectId; }
    public TurkuRenderReadinessStatus getStatus() { return status; }
    public boolean isCompositionReady() { return compositionReady; }
    public boolean isManifestReady() { return manifestReady; }
    public boolean isAllScenesHaveImages() { return allScenesHaveImages; }
    public boolean isAllNarrationPresent() { return allNarrationPresent; }
    public boolean isTimingPlanComplete() { return timingPlanComplete; }
    public boolean isCopyrightReviewDone() { return copyrightReviewDone; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public int getReadinessScore() { return readinessScore; }
    public List<TurkuRenderReadinessIssue> getIssues() { return issues; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
