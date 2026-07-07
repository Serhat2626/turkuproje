package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-024 Task 03: File safety report.
 * Immutable safety flags for a file reference.
 */
public final class TurkuRenderFileSafetyReport {
    private final String fileId;
    private final boolean safe;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> safetyFlags;

    public TurkuRenderFileSafetyReport(String fileId, boolean safe) {
        this.fileId = fileId;
        this.safe = safe;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.safetyFlags = List.of(
                "NO_PUBLISH", "NO_EXPORT", "NO_DOWNLOAD",
                "INTERNAL_ONLY", "HUMAN_REVIEW_REQUIRED");
    }

    public String getFileId() { return fileId; }
    public boolean isSafe() { return safe; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
