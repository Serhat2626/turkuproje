package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-025 Task 02: Safe job lifecycle summary DTO.
 * Opaque refs only. No forbidden exposure fields.
 */
public final class TurkuInternalRenderJobSummary {
    private final String jobRef;
    private final TurkuInternalRenderJobStatus status;
    private final int progressPercent;
    private final String currentStageLabel;
    private final String safeTraceRef;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> safetyFlags;

    public TurkuInternalRenderJobSummary(String jobRef, TurkuInternalRenderJobStatus status,
                                          int progressPercent, String currentStageLabel,
                                          String safeTraceRef) {
        this.jobRef = jobRef;
        this.status = status;
        this.progressPercent = TurkuInternalRenderJobProgress.clampProgress(progressPercent);
        this.currentStageLabel = currentStageLabel;
        this.safeTraceRef = safeTraceRef;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "HUMAN_REVIEW_REQUIRED");
    }

    public String getJobRef() { return jobRef; }
    public TurkuInternalRenderJobStatus getStatus() { return status; }
    public int getProgressPercent() { return progressPercent; }
    public String getCurrentStageLabel() { return currentStageLabel; }
    public String getSafeTraceRef() { return safeTraceRef; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
