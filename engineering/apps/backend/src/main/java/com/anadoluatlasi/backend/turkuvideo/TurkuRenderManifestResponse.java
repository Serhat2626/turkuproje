package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-021 Task 03: Safe response DTO for render manifest preparation.
 * No rendered video, no downloadable file, no public/signed/CDN URL.
 */
public final class TurkuRenderManifestResponse {

    private final UUID projectId;
    private final UUID manifestId;
    private final TurkuVideoWorkflowStage currentStage;
    private final int sceneCount;
    private final double totalDurationSeconds;
    private final String targetResolution;
    private final int targetFps;
    private final String targetFormat;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final List<String> reviewFlags;
    private final String summary;

    public TurkuRenderManifestResponse(UUID projectId, UUID manifestId,
                                        TurkuVideoWorkflowStage currentStage,
                                        int sceneCount, double totalDurationSeconds,
                                        String targetResolution, int targetFps, String targetFormat,
                                        boolean humanReviewRequired, boolean copyrightReviewRequired,
                                        List<String> reviewFlags, String summary) {
        this.projectId = projectId;
        this.manifestId = manifestId;
        this.currentStage = currentStage;
        this.sceneCount = sceneCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.targetResolution = targetResolution;
        this.targetFps = targetFps;
        this.targetFormat = targetFormat;
        this.humanReviewRequired = humanReviewRequired;
        this.copyrightReviewRequired = copyrightReviewRequired;
        this.autoApproved = false;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
        this.summary = summary;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getManifestId() { return manifestId; }
    public TurkuVideoWorkflowStage getCurrentStage() { return currentStage; }
    public int getSceneCount() { return sceneCount; }
    public double getTotalDurationSeconds() { return totalDurationSeconds; }
    public String getTargetResolution() { return targetResolution; }
    public int getTargetFps() { return targetFps; }
    public String getTargetFormat() { return targetFormat; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getReviewFlags() { return reviewFlags; }
    public String getSummary() { return summary; }
}
