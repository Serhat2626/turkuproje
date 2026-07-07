package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-024 Task 04: FFmpeg execution safety envelope.
 */
public final class TurkuFfmpegExecutionSafetyEnvelope {
    private final TurkuFfmpegExecutionMode executionMode;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> safetyFlags;
    private final List<String> validationNotes;

    public TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode executionMode,
                                                List<String> validationNotes) {
        this.executionMode = executionMode;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.safetyFlags = List.of(
                "NO_PUBLISH", "NO_EXPORT", "NO_DOWNLOAD",
                "NO_PUBLIC_DELIVERY", "INTERNAL_ONLY",
                "HUMAN_REVIEW_REQUIRED", "NO_AUTO_APPROVAL",
                "NO_PROCESS_EXECUTION");
        this.validationNotes = validationNotes != null ? List.copyOf(validationNotes) : List.of();
    }

    public TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode executionMode) {
        this(executionMode, List.of());
    }

    public TurkuFfmpegExecutionMode getExecutionMode() { return executionMode; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getSafetyFlags() { return safetyFlags; }
    public List<String> getValidationNotes() { return validationNotes; }
}
