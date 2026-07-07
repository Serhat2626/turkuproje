package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-023 Task 04: Internal render artifact.
 * Combines reference, safety, retention into single safe model.
 * No file path, no URL, no storage key, no binary.
 */
public final class TurkuInternalRenderArtifact {
    private final TurkuRenderArtifactReference reference;
    private final TurkuRenderArtifactSafetyReport safetyReport;
    private final TurkuRenderArtifactRetentionPolicy retentionPolicy;
    private TurkuRenderArtifactStatus status;

    public TurkuInternalRenderArtifact(TurkuRenderArtifactReference reference,
                                        TurkuRenderAdapterMode rendererMode) {
        this.reference = reference;
        this.safetyReport = new TurkuRenderArtifactSafetyReport(
                reference.getArtifactId(),
                TurkuRenderArtifactStatus.CREATED, rendererMode);
        this.retentionPolicy = new TurkuRenderArtifactRetentionPolicy();
        this.status = TurkuRenderArtifactStatus.CREATED;
    }

    public TurkuRenderArtifactReference getReference() { return reference; }
    public TurkuRenderArtifactSafetyReport getSafetyReport() { return safetyReport; }
    public TurkuRenderArtifactRetentionPolicy getRetentionPolicy() { return retentionPolicy; }
    public TurkuRenderArtifactStatus getStatus() { return status; }

    public void markValidated() { this.status = TurkuRenderArtifactStatus.VALIDATED; }
    public void markCleanupEligible() { this.status = TurkuRenderArtifactStatus.CLEANUP_ELIGIBLE; }
    public void markRejected() { this.status = TurkuRenderArtifactStatus.REJECTED; }
}
