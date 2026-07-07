package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-021 Task 04: Composition preview response — safe read-only DTO.
 * Internal preview metadata only.
 * No rendered video, no downloadable file, no public/signed/CDN URL.
 */
public final class TurkuCompositionPreviewResponse {

    private final UUID projectId;
    private final TurkuVideoWorkflowStage currentStage;
    private final boolean compositionReady;
    private final boolean manifestReady;
    private final List<TurkuCompositionPreviewScene> timelineScenes;
    private final double totalDurationSeconds;
    private final int sceneCount;
    private final String readinessStatus;
    private final List<String> readinessIssues;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final List<String> reviewFlags;
    private final String summary;

    public TurkuCompositionPreviewResponse(UUID projectId, TurkuVideoWorkflowStage currentStage,
                                            boolean compositionReady, boolean manifestReady,
                                            List<TurkuCompositionPreviewScene> timelineScenes,
                                            double totalDurationSeconds, int sceneCount,
                                            String readinessStatus, List<String> readinessIssues,
                                            List<String> reviewFlags, String summary) {
        this.projectId = projectId;
        this.currentStage = currentStage;
        this.compositionReady = compositionReady;
        this.manifestReady = manifestReady;
        this.timelineScenes = timelineScenes != null ? List.copyOf(timelineScenes) : List.of();
        this.totalDurationSeconds = totalDurationSeconds;
        this.sceneCount = sceneCount;
        this.readinessStatus = readinessStatus;
        this.readinessIssues = readinessIssues != null ? List.copyOf(readinessIssues) : List.of();
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
        this.summary = summary;
    }

    public UUID getProjectId() { return projectId; }
    public TurkuVideoWorkflowStage getCurrentStage() { return currentStage; }
    public boolean isCompositionReady() { return compositionReady; }
    public boolean isManifestReady() { return manifestReady; }
    public List<TurkuCompositionPreviewScene> getTimelineScenes() { return timelineScenes; }
    public double getTotalDurationSeconds() { return totalDurationSeconds; }
    public int getSceneCount() { return sceneCount; }
    public String getReadinessStatus() { return readinessStatus; }
    public List<String> getReadinessIssues() { return readinessIssues; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getReviewFlags() { return reviewFlags; }
    public String getSummary() { return summary; }
}
