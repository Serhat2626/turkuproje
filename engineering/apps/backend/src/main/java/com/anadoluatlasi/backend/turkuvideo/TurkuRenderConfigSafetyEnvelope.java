package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-024 Task 02: Config safety envelope.
 * Immutable safety flags for render config state.
 */
public final class TurkuRenderConfigSafetyEnvelope {
    private final TurkuRenderConfigMode configMode;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> safetyFlags;

    public TurkuRenderConfigSafetyEnvelope(TurkuRenderConfigMode configMode) {
        this.configMode = configMode;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.safetyFlags = List.of(
                "NO_PUBLISH", "NO_EXPORT", "NO_DOWNLOAD",
                "NO_PUBLIC_DELIVERY", "INTERNAL_ONLY",
                "HUMAN_REVIEW_REQUIRED", "NO_AUTO_APPROVAL");
    }

    public TurkuRenderConfigMode getConfigMode() { return configMode; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
