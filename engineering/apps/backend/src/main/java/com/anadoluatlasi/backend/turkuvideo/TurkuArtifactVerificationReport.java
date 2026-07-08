package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-007: Image artifact verification report.
 */
public final class TurkuArtifactVerificationReport {
    private final TurkuArtifactVerificationStatus status;
    private final String opaqueVerificationRef;
    private final String opaqueGenerationRef;
    private final List<TurkuSceneArtifactReadiness> sceneReadiness;
    private final int totalScenes;
    private final int verifiedScenes;
    private final String validationSummary;

    public TurkuArtifactVerificationReport(
            TurkuArtifactVerificationStatus status, String opaqueVerificationRef,
            String opaqueGenerationRef, List<TurkuSceneArtifactReadiness> sceneReadiness,
            int totalScenes, int verifiedScenes, String validationSummary) {
        this.status = status;
        this.opaqueVerificationRef = opaqueVerificationRef;
        this.opaqueGenerationRef = opaqueGenerationRef;
        this.sceneReadiness = sceneReadiness == null ? List.of() : List.copyOf(sceneReadiness);
        this.totalScenes = totalScenes;
        this.verifiedScenes = verifiedScenes;
        this.validationSummary = validationSummary;
    }

    public TurkuArtifactVerificationStatus getStatus() { return status; }
    public String getOpaqueVerificationRef() { return opaqueVerificationRef; }
    public String getOpaqueGenerationRef() { return opaqueGenerationRef; }
    public List<TurkuSceneArtifactReadiness> getSceneReadiness() { return sceneReadiness; }
    public int getTotalScenes() { return totalScenes; }
    public int getVerifiedScenes() { return verifiedScenes; }
    public String getValidationSummary() { return validationSummary; }
}
