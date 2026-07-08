package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-006: Image generation execution candidate result.
 */
public final class TurkuImageGenerationExecutionResult {
    private final TurkuImageGenerationStatus status;
    private final List<TurkuImageGenerationIssueCode> issues;
    private final String opaqueGenerationRef;
    private final String opaquePromptPackageRef;
    private final TurkuImageGenerationExecutionMode executionMode;
    private final String providerDisabledReason;
    private final boolean manualGatePassed;
    private final List<TurkuFakeImageResultMetadata> fakeResults;
    private final int totalScenes;
    private final int fakeResultCount;
    private final String validationSummary;

    public TurkuImageGenerationExecutionResult(
            TurkuImageGenerationStatus status, List<TurkuImageGenerationIssueCode> issues,
            String opaqueGenerationRef, String opaquePromptPackageRef,
            TurkuImageGenerationExecutionMode executionMode, String providerDisabledReason,
            boolean manualGatePassed, List<TurkuFakeImageResultMetadata> fakeResults,
            int totalScenes, int fakeResultCount, String validationSummary) {
        this.status = status;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.opaqueGenerationRef = opaqueGenerationRef;
        this.opaquePromptPackageRef = opaquePromptPackageRef;
        this.executionMode = executionMode;
        this.providerDisabledReason = providerDisabledReason;
        this.manualGatePassed = manualGatePassed;
        this.fakeResults = fakeResults == null ? List.of() : List.copyOf(fakeResults);
        this.totalScenes = totalScenes;
        this.fakeResultCount = fakeResultCount;
        this.validationSummary = validationSummary;
    }

    public TurkuImageGenerationStatus getStatus() { return status; }
    public List<TurkuImageGenerationIssueCode> getIssues() { return issues; }
    public String getOpaqueGenerationRef() { return opaqueGenerationRef; }
    public String getOpaquePromptPackageRef() { return opaquePromptPackageRef; }
    public TurkuImageGenerationExecutionMode getExecutionMode() { return executionMode; }
    public String getProviderDisabledReason() { return providerDisabledReason; }
    public boolean isManualGatePassed() { return manualGatePassed; }
    public List<TurkuFakeImageResultMetadata> getFakeResults() { return fakeResults; }
    public int getTotalScenes() { return totalScenes; }
    public int getFakeResultCount() { return fakeResultCount; }
    public String getValidationSummary() { return validationSummary; }
}
