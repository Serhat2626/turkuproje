package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-007: Render package candidate result — metadata only.
 */
public final class TurkuRenderPackageResult {
    private final TurkuRenderPackageStatus status;
    private final String opaqueRenderPackageRef;
    private final String opaqueVerificationRef;
    private final String renderInputManifestSummary;
    private final List<String> renderPrerequisiteLabels;
    private final boolean localManualRenderEnabled;
    private final String localManualRenderNote;
    private final List<String> blockedReasons;
    private final String validationSummary;

    public TurkuRenderPackageResult(
            TurkuRenderPackageStatus status, String opaqueRenderPackageRef,
            String opaqueVerificationRef, String renderInputManifestSummary,
            List<String> renderPrerequisiteLabels, boolean localManualRenderEnabled,
            String localManualRenderNote, List<String> blockedReasons, String validationSummary) {
        this.status = status;
        this.opaqueRenderPackageRef = opaqueRenderPackageRef;
        this.opaqueVerificationRef = opaqueVerificationRef;
        this.renderInputManifestSummary = renderInputManifestSummary;
        this.renderPrerequisiteLabels = renderPrerequisiteLabels == null ? List.of() : List.copyOf(renderPrerequisiteLabels);
        this.localManualRenderEnabled = localManualRenderEnabled;
        this.localManualRenderNote = localManualRenderNote;
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.validationSummary = validationSummary;
    }

    public TurkuRenderPackageStatus getStatus() { return status; }
    public String getOpaqueRenderPackageRef() { return opaqueRenderPackageRef; }
    public String getOpaqueVerificationRef() { return opaqueVerificationRef; }
    public String getRenderInputManifestSummary() { return renderInputManifestSummary; }
    public List<String> getRenderPrerequisiteLabels() { return renderPrerequisiteLabels; }
    public boolean isLocalManualRenderEnabled() { return localManualRenderEnabled; }
    public String getLocalManualRenderNote() { return localManualRenderNote; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public String getValidationSummary() { return validationSummary; }
}
