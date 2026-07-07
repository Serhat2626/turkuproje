package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-023 Task 02: Safety envelope for render adapter.
 * All safety flags are immutable safe defaults.
 */
public final class TurkuRenderAdapterSafetyEnvelope {
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final TurkuRenderAdapterMode adapterMode;
    private final List<String> safetyFlags;
    private final List<String> reviewFlags;

    public TurkuRenderAdapterSafetyEnvelope(TurkuRenderAdapterMode adapterMode) {
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.adapterMode = adapterMode;
        this.safetyFlags = List.of(
                "NO_REAL_RENDER", "NO_BINARY_OUTPUT",
                "NO_PUBLIC_DELIVERY", "NO_EXPORT",
                "NO_SHELL_EXECUTION", "NO_USER_ARGS");
        this.reviewFlags = List.of(
                "HUMAN_REVIEW_REQUIRED", "COPYRIGHT_REVIEW_REQUIRED",
                "NOT_PUBLISH_READY", "NOT_EXPORT_READY", "INTERNAL_ONLY");
    }

    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public TurkuRenderAdapterMode getAdapterMode() { return adapterMode; }
    public List<String> getSafetyFlags() { return safetyFlags; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
