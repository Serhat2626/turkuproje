package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-024 Task 05: Artifact metadata integration service.
 * Connects execution result → verification → artifact metadata.
 * Metadata-only. No filesystem.
 */
public class TurkuRenderArtifactMetadataIntegrationService {

    private final TurkuRenderArtifactVerifier verifier;
    private final TurkuRenderArtifactService artifactService;

    public TurkuRenderArtifactMetadataIntegrationService(
            TurkuRenderArtifactVerifier verifier,
            TurkuRenderArtifactService artifactService) {
        this.verifier = verifier;
        this.artifactService = artifactService;
    }

    public TurkuRenderArtifactMetadataIntegrationService() {
        this(new TurkuRenderArtifactVerifier(), new TurkuRenderArtifactService());
    }

    /**
     * Integrate execution result into artifact metadata with verification.
     */
    public TurkuRenderArtifactVerificationReport integrateFromExecution(
            TurkuFfmpegExecutionResult executionResult,
            TurkuRenderExecutionPlan plan) {

        // Step 1: Verify
        var report = verifier.verifyFromExecution(executionResult, plan);

        // Step 2: If verified, create artifact metadata via existing service
        if (report.getStatus() == TurkuRenderArtifactVerificationStatus.VERIFIED
                && executionResult != null && executionResult.isSuccess()) {
            var artifact = new TurkuRenderArtifact(
                    java.util.UUID.randomUUID(),
                    executionResult.getOpaqueArtifactRef(),
                    plan != null ? plan.getTargetFormat() : "mp4",
                    plan != null ? plan.getTotalDurationSeconds() : 0,
                    plan != null ? plan.getTargetResolution() : "1920x1080",
                    "internal");
            var executionSafeResult = new TurkuRenderExecutionResult(
                    true, executionResult.getDurationMs(),
                    null, null, artifact,
                    "verification-integrated:" + java.util.UUID.randomUUID());
            artifactService.createFromExecutionResult(
                    executionSafeResult, TurkuRenderAdapterMode.STUB);
        }

        return report;
    }

    /**
     * Verify-only without creating artifact metadata.
     */
    public TurkuRenderArtifactVerificationReport verifyOnly(
            String opaqueRef, String rendererMode,
            String format, double duration, int sizeMb,
            String resolution, int fps) {
        return verifier.verify(opaqueRef, rendererMode, format, duration, sizeMb, resolution, fps);
    }
}
