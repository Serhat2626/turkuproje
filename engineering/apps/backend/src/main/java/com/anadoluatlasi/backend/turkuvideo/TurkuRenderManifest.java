package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-021 Task 03: Render manifest — complete render instruction set.
 *
 * <p>Planning artifact only. No actual video rendering.
 * No downloadable file, no public/signed/CDN URL, no storage key,
 * no base64/binary, no raw provider payload, no lyrics.</p>
 */
public final class TurkuRenderManifest {

    private final UUID manifestId;
    private final UUID projectId;
    private final UUID compositionId;
    private final List<TurkuRenderManifestScene> scenes;
    private final String targetResolution;
    private final int targetFps;
    private final String targetFormat;
    private final double totalDurationSeconds;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final List<String> reviewFlags;

    public TurkuRenderManifest(UUID manifestId, UUID projectId, UUID compositionId,
                                List<TurkuRenderManifestScene> scenes,
                                String targetResolution, int targetFps, String targetFormat,
                                double totalDurationSeconds, List<String> reviewFlags) {
        this.manifestId = manifestId;
        this.projectId = projectId;
        this.compositionId = compositionId;
        this.scenes = scenes != null ? List.copyOf(scenes) : List.of();
        this.targetResolution = targetResolution;
        this.targetFps = targetFps;
        this.targetFormat = targetFormat;
        this.totalDurationSeconds = totalDurationSeconds;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getManifestId() { return manifestId; }
    public UUID getProjectId() { return projectId; }
    public UUID getCompositionId() { return compositionId; }
    public List<TurkuRenderManifestScene> getScenes() { return scenes; }
    public String getTargetResolution() { return targetResolution; }
    public int getTargetFps() { return targetFps; }
    public String getTargetFormat() { return targetFormat; }
    public double getTotalDurationSeconds() { return totalDurationSeconds; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
