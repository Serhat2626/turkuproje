package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RP-002: Storyboard draft result — safe metadata + scene list.
 */
public final class TurkuStoryboardDraftResult {
    private final TurkuStoryboardDraftStatus status;
    private final List<TurkuStoryboardDraftIssueCode> issueCodes;
    private final String opaqueDraftRef;
    private final String opaqueRequestRef;
    private final String safeTurkuTitleLabel;
    private final String copyrightSafeIntentSummary;
    private final String orchestrationMode;
    private final List<TurkuStoryboardDraftScene> scenes;
    private final int totalSceneCount;
    private final int totalDurationMs;
    private final String targetFormat;
    private final int targetWidth;
    private final int targetHeight;
    private final int targetFps;

    public TurkuStoryboardDraftResult(
            TurkuStoryboardDraftStatus status,
            List<TurkuStoryboardDraftIssueCode> issueCodes,
            String opaqueDraftRef, String opaqueRequestRef,
            String safeTurkuTitleLabel, String copyrightSafeIntentSummary,
            String orchestrationMode, List<TurkuStoryboardDraftScene> scenes,
            int totalSceneCount, int totalDurationMs,
            String targetFormat, int targetWidth, int targetHeight, int targetFps) {
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.opaqueDraftRef = opaqueDraftRef;
        this.opaqueRequestRef = opaqueRequestRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.copyrightSafeIntentSummary = copyrightSafeIntentSummary;
        this.orchestrationMode = orchestrationMode;
        this.scenes = scenes == null ? List.of() : List.copyOf(scenes);
        this.totalSceneCount = totalSceneCount;
        this.totalDurationMs = totalDurationMs;
        this.targetFormat = targetFormat;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.targetFps = targetFps;
    }

    public static TurkuStoryboardDraftResult rejected(TurkuStoryboardDraftIssueCode... codes) {
        return new TurkuStoryboardDraftResult(
                TurkuStoryboardDraftStatus.REJECTED_UNSAFE, List.of(codes),
                null, null, null, null, null, List.of(), 0, 0, null, 0, 0, 0);
    }

    public static TurkuStoryboardDraftResult failed(TurkuStoryboardDraftIssueCode... codes) {
        return new TurkuStoryboardDraftResult(
                TurkuStoryboardDraftStatus.FAILED_SAFE, List.of(codes),
                null, null, null, null, null, List.of(), 0, 0, null, 0, 0, 0);
    }

    public TurkuStoryboardDraftStatus getStatus() { return status; }
    public List<TurkuStoryboardDraftIssueCode> getIssueCodes() { return issueCodes; }
    public String getOpaqueDraftRef() { return opaqueDraftRef; }
    public String getOpaqueRequestRef() { return opaqueRequestRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getCopyrightSafeIntentSummary() { return copyrightSafeIntentSummary; }
    public String getOrchestrationMode() { return orchestrationMode; }
    public List<TurkuStoryboardDraftScene> getScenes() { return scenes; }
    public int getTotalSceneCount() { return totalSceneCount; }
    public int getTotalDurationMs() { return totalDurationMs; }
    public String getTargetFormat() { return targetFormat; }
    public int getTargetWidth() { return targetWidth; }
    public int getTargetHeight() { return targetHeight; }
    public int getTargetFps() { return targetFps; }
}
