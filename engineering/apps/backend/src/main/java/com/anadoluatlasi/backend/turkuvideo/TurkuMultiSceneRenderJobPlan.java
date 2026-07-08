package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-038: Multi-scene render job plan. Metadata-only.
 */
public final class TurkuMultiSceneRenderJobPlan {
    private final String opaqueJobRef;
    private final TurkuMultiSceneRenderJobStatus status;
    private final List<TurkuMultiSceneRenderSceneRef> scenes;
    private final List<TurkuMultiSceneRenderJobIssueCode> issueCodes;
    private final int sceneCount;
    private final int totalExpectedDurationSeconds;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxFps;
    private final int requiredArtifactCount;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;

    public TurkuMultiSceneRenderJobPlan(String opaqueJobRef,
            TurkuMultiSceneRenderJobStatus status,
            List<TurkuMultiSceneRenderSceneRef> scenes,
            List<TurkuMultiSceneRenderJobIssueCode> issueCodes) {
        this.opaqueJobRef = opaqueJobRef;
        this.status = status;
        this.scenes = scenes != null ? List.copyOf(scenes) : List.of();
        this.issueCodes = issueCodes != null ? List.copyOf(issueCodes) : List.of();
        this.sceneCount = this.scenes.size();
        this.totalExpectedDurationSeconds = this.scenes.stream()
                .mapToInt(TurkuMultiSceneRenderSceneRef::getExpectedDurationSeconds).sum();
        this.maxWidth = this.scenes.stream().mapToInt(TurkuMultiSceneRenderSceneRef::getExpectedWidth).max().orElse(0);
        this.maxHeight = this.scenes.stream().mapToInt(TurkuMultiSceneRenderSceneRef::getExpectedHeight).max().orElse(0);
        this.maxFps = this.scenes.stream().mapToInt(TurkuMultiSceneRenderSceneRef::getExpectedFps).max().orElse(0);
        this.requiredArtifactCount = (int) this.scenes.stream()
                .filter(TurkuMultiSceneRenderSceneRef::isHasPrimaryVideoExpectation).count();
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
    }

    public String getOpaqueJobRef() { return opaqueJobRef; }
    public TurkuMultiSceneRenderJobStatus getStatus() { return status; }
    public List<TurkuMultiSceneRenderSceneRef> getScenes() { return scenes; }
    public List<TurkuMultiSceneRenderJobIssueCode> getIssueCodes() { return issueCodes; }
    public int getSceneCount() { return sceneCount; }
    public int getTotalExpectedDurationSeconds() { return totalExpectedDurationSeconds; }
    public int getMaxWidth() { return maxWidth; }
    public int getMaxHeight() { return maxHeight; }
    public int getMaxFps() { return maxFps; }
    public int getRequiredArtifactCount() { return requiredArtifactCount; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }

    @Override
    public String toString() {
        return "MultiSceneRenderJobPlan{ref=" + opaqueJobRef
                + ", status=" + status
                + ", scenes=" + sceneCount
                + ", totalDuration=" + totalExpectedDurationSeconds + "s"
                + ", issues=" + issueCodes + "}";
    }
}
