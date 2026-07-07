package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-023 Task 04: Render artifact service.
 * Creates safe artifact metadata from render execution results.
 * No filesystem access. No storage provider calls. No real deletion.
 */
public class TurkuRenderArtifactService {

    private static final List<String> FORBIDDEN_REF_PATTERNS = List.of(
            "http://", "https://", "ftp://", "file://",
            "s3://", "gs://", "az://",
            "C:\\", "D:\\", "/usr", "/tmp", "/var", "/home",
            "publicUrl", "signedUrl", "cdnUrl", "storageKey",
            "base64", "data:image", "data:video", "data:audio",
            "Bearer ", "Authorization:", "token=", "apiKey=",
            "password=", "secret=",
            ";", "&&", "||", "$(", "`",
            "rm ", "del ", "format ");

    private final TurkuRenderAdapterSanitizer sanitizer;

    public TurkuRenderArtifactService() {
        this.sanitizer = new TurkuRenderAdapterSanitizer();
    }

    public TurkuInternalRenderArtifact createFromExecutionResult(
            TurkuRenderExecutionResult result,
            TurkuRenderAdapterMode rendererMode) {
        if (result == null || result.getArtifact() == null) {
            return null;
        }

        TurkuRenderArtifact rawArtifact = result.getArtifact();
        String ref = rawArtifact.getArtifactRef();

        // Validate ref is safe
        if (!isValidOpaqueRef(ref)) {
            return null;
        }

        TurkuRenderArtifactReference reference = new TurkuRenderArtifactReference(
                rawArtifact.getArtifactId(),
                ref, rendererMode,
                rawArtifact.getFormat(),
                rawArtifact.getDurationSeconds(),
                rawArtifact.getResolution(),
                rawArtifact.getSizeLabel());

        TurkuInternalRenderArtifact artifact = new TurkuInternalRenderArtifact(reference, rendererMode);
        artifact.markValidated();
        return artifact;
    }

    public boolean isValidOpaqueRef(String ref) {
        if (ref == null || ref.isBlank()) return false;
        for (String pattern : FORBIDDEN_REF_PATTERNS) {
            if (ref.contains(pattern)) return false;
        }
        return true;
    }

    public TurkuRenderArtifactCleanupPlan createCleanupPlan(TurkuInternalRenderArtifact artifact) {
        if (artifact == null) {
            return null;
        }
        boolean eligible = artifact.getStatus() == TurkuRenderArtifactStatus.VALIDATED
                || artifact.getStatus() == TurkuRenderArtifactStatus.CLEANUP_ELIGIBLE;
        String reason = eligible
                ? "Artifact eligible for cleanup after retention period"
                : "Artifact not eligible: status=" + artifact.getStatus();

        return new TurkuRenderArtifactCleanupPlan(
                artifact.getReference().getArtifactId(),
                artifact.getReference().getOpaqueRef(),
                artifact.getRetentionPolicy(),
                eligible, reason);
    }

    public TurkuRenderArtifactSafetyReport getSafetyReport(TurkuInternalRenderArtifact artifact) {
        if (artifact == null) return null;
        return artifact.getSafetyReport();
    }
}
