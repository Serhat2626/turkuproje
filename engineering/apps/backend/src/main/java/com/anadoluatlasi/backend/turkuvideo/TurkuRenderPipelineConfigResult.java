package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-003: Render pipeline configuration result — audit-safe metadata.
 */
public final class TurkuRenderPipelineConfigResult {
    private final TurkuRenderConfigStatus status;
    private final List<TurkuRenderConfigIssueCode> issues;
    private final String opaqueRenderConfigRef;
    private final TurkuRenderEnvironmentBoundary environmentBoundary;
    private final List<TurkuFfmpegCapabilityMetadata> ffmpegCapabilities;
    private final TurkuOutputFormatPolicy outputFormatPolicy;
    private final List<String> safetyPrerequisiteLabels;
    private final boolean renderExecutionEnabled;
    private final String validationSummary;

    public TurkuRenderPipelineConfigResult(
            TurkuRenderConfigStatus status, List<TurkuRenderConfigIssueCode> issues,
            String opaqueRenderConfigRef, TurkuRenderEnvironmentBoundary environmentBoundary,
            List<TurkuFfmpegCapabilityMetadata> ffmpegCapabilities, TurkuOutputFormatPolicy outputFormatPolicy,
            List<String> safetyPrerequisiteLabels, boolean renderExecutionEnabled, String validationSummary) {
        this.status = status;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.opaqueRenderConfigRef = opaqueRenderConfigRef;
        this.environmentBoundary = environmentBoundary;
        this.ffmpegCapabilities = ffmpegCapabilities == null ? List.of() : List.copyOf(ffmpegCapabilities);
        this.outputFormatPolicy = outputFormatPolicy;
        this.safetyPrerequisiteLabels = safetyPrerequisiteLabels == null ? List.of() : List.copyOf(safetyPrerequisiteLabels);
        this.renderExecutionEnabled = renderExecutionEnabled;
        this.validationSummary = validationSummary;
    }

    public TurkuRenderConfigStatus getStatus() { return status; }
    public List<TurkuRenderConfigIssueCode> getIssues() { return issues; }
    public String getOpaqueRenderConfigRef() { return opaqueRenderConfigRef; }
    public TurkuRenderEnvironmentBoundary getEnvironmentBoundary() { return environmentBoundary; }
    public List<TurkuFfmpegCapabilityMetadata> getFfmpegCapabilities() { return ffmpegCapabilities; }
    public TurkuOutputFormatPolicy getOutputFormatPolicy() { return outputFormatPolicy; }
    public List<String> getSafetyPrerequisiteLabels() { return safetyPrerequisiteLabels; }
    public boolean isRenderExecutionEnabled() { return renderExecutionEnabled; }
    public String getValidationSummary() { return validationSummary; }
}
