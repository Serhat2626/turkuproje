package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RP-001: Storyboard request result — safe metadata only.
 */
public final class TurkuNameStoryboardRequestResult {
    private final TurkuNameStoryboardRequestStatus status;
    private final List<TurkuNameStoryboardRequestIssueCode> issueCodes;
    private final String opaqueStoryboardRequestRef;
    private final String safeTurkuTitleLabel;
    private final String inspirationMode;
    private final int targetSceneCount;
    private final int targetDurationMs;
    private final String targetFormat;
    private final int targetWidth;
    private final int targetHeight;
    private final int targetFps;
    private final String copyrightSafeIntentSummary;

    public TurkuNameStoryboardRequestResult(
            TurkuNameStoryboardRequestStatus status,
            List<TurkuNameStoryboardRequestIssueCode> issueCodes,
            String opaqueStoryboardRequestRef, String safeTurkuTitleLabel,
            String inspirationMode, int targetSceneCount, int targetDurationMs,
            String targetFormat, int targetWidth, int targetHeight, int targetFps,
            String copyrightSafeIntentSummary) {
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.opaqueStoryboardRequestRef = opaqueStoryboardRequestRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.inspirationMode = inspirationMode;
        this.targetSceneCount = targetSceneCount;
        this.targetDurationMs = targetDurationMs;
        this.targetFormat = targetFormat;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.targetFps = targetFps;
        this.copyrightSafeIntentSummary = copyrightSafeIntentSummary;
    }

    public static TurkuNameStoryboardRequestResult rejected(TurkuNameStoryboardRequestIssueCode... codes) {
        return new TurkuNameStoryboardRequestResult(
                TurkuNameStoryboardRequestStatus.REJECTED_UNSAFE, List.of(codes),
                null, null, null, 0, 0, null, 0, 0, 0, null);
    }

    public static TurkuNameStoryboardRequestResult failed(TurkuNameStoryboardRequestIssueCode... codes) {
        return new TurkuNameStoryboardRequestResult(
                TurkuNameStoryboardRequestStatus.FAILED_SAFE, List.of(codes),
                null, null, null, 0, 0, null, 0, 0, 0, null);
    }

    public TurkuNameStoryboardRequestStatus getStatus() { return status; }
    public List<TurkuNameStoryboardRequestIssueCode> getIssueCodes() { return issueCodes; }
    public String getOpaqueStoryboardRequestRef() { return opaqueStoryboardRequestRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getInspirationMode() { return inspirationMode; }
    public int getTargetSceneCount() { return targetSceneCount; }
    public int getTargetDurationMs() { return targetDurationMs; }
    public String getTargetFormat() { return targetFormat; }
    public int getTargetWidth() { return targetWidth; }
    public int getTargetHeight() { return targetHeight; }
    public int getTargetFps() { return targetFps; }
    public String getCopyrightSafeIntentSummary() { return copyrightSafeIntentSummary; }
}
