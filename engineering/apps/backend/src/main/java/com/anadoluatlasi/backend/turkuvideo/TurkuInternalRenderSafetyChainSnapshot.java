package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-025 Task 03: FP-024 safety chain snapshot.
 * Summarizes config/workspace/execution/artifact verification status
 * without exposing internals.
 */
public final class TurkuInternalRenderSafetyChainSnapshot {
    private final boolean configValid;
    private final boolean workspaceSafe;
    private final boolean executionBoundarySafe;
    private final boolean artifactVerified;
    private final String configModeLabel;
    private final String executionModeLabel;
    private final String verificationStatusLabel;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean publicDeliveryAvailable;
    private final List<String> safetyFlags;

    public TurkuInternalRenderSafetyChainSnapshot(boolean configValid, boolean workspaceSafe,
                                                    boolean executionBoundarySafe, boolean artifactVerified,
                                                    String configModeLabel, String executionModeLabel,
                                                    String verificationStatusLabel) {
        this.configValid = configValid;
        this.workspaceSafe = workspaceSafe;
        this.executionBoundarySafe = executionBoundarySafe;
        this.artifactVerified = artifactVerified;
        this.configModeLabel = configModeLabel;
        this.executionModeLabel = executionModeLabel;
        this.verificationStatusLabel = verificationStatusLabel;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.publicDeliveryAvailable = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "HUMAN_REVIEW_REQUIRED", "NO_PROCESS_EXECUTION");
    }

    public boolean isConfigValid() { return configValid; }
    public boolean isWorkspaceSafe() { return workspaceSafe; }
    public boolean isExecutionBoundarySafe() { return executionBoundarySafe; }
    public boolean isArtifactVerified() { return artifactVerified; }
    public String getConfigModeLabel() { return configModeLabel; }
    public String getExecutionModeLabel() { return executionModeLabel; }
    public String getVerificationStatusLabel() { return verificationStatusLabel; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
