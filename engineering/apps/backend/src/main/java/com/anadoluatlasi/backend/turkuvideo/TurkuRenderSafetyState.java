package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-021 Task 03: Render safety state — safety context for render operations.
 */
public final class TurkuRenderSafetyState {

    private final UUID projectId;
    private final boolean renderAllowed;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;
    private final boolean copyrightClearanceVerified;
    private final boolean lyricsIncluded;
    private final boolean audioSourceSafe;
    private final String safeFailureCode;
    private final List<String> safeWarnings;
    private final List<String> reviewFlags;

    public TurkuRenderSafetyState(UUID projectId, boolean renderAllowed,
                                    String safeFailureCode,
                                    List<String> safeWarnings,
                                    List<String> reviewFlags) {
        this.projectId = projectId;
        this.renderAllowed = renderAllowed;
        this.humanReviewRequired = true;
        this.autoApproved = false;
        this.copyrightClearanceVerified = false;
        this.lyricsIncluded = false;
        this.audioSourceSafe = true;
        this.safeFailureCode = safeFailureCode;
        this.safeWarnings = safeWarnings != null ? List.copyOf(safeWarnings) : List.of();
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getProjectId() { return projectId; }
    public boolean isRenderAllowed() { return renderAllowed; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isCopyrightClearanceVerified() { return copyrightClearanceVerified; }
    public boolean isLyricsIncluded() { return lyricsIncluded; }
    public boolean isAudioSourceSafe() { return audioSourceSafe; }
    public String getSafeFailureCode() { return safeFailureCode; }
    public List<String> getSafeWarnings() { return safeWarnings; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
