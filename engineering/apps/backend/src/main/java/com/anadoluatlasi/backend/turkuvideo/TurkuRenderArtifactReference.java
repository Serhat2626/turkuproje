package com.anadoluatlasi.backend.turkuvideo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * FP-023 Task 04: Render artifact reference.
 * Opaque internal ref only. No file path, no URL, no storage key.
 */
public final class TurkuRenderArtifactReference {
    private final UUID artifactId;
    private final String opaqueRef;
    private final TurkuRenderAdapterMode rendererMode;
    private final String format;
    private final double durationSeconds;
    private final String resolution;
    private final String sizeLabel;
    private final Instant createdAt;

    public TurkuRenderArtifactReference(UUID artifactId, String opaqueRef,
                                         TurkuRenderAdapterMode rendererMode,
                                         String format, double durationSeconds,
                                         String resolution, String sizeLabel) {
        this.artifactId = artifactId;
        this.opaqueRef = opaqueRef;
        this.rendererMode = rendererMode;
        this.format = format;
        this.durationSeconds = durationSeconds;
        this.resolution = resolution;
        this.sizeLabel = sizeLabel;
        this.createdAt = Instant.now();
    }

    public UUID getArtifactId() { return artifactId; }
    public String getOpaqueRef() { return opaqueRef; }
    public TurkuRenderAdapterMode getRendererMode() { return rendererMode; }
    public String getFormat() { return format; }
    public double getDurationSeconds() { return durationSeconds; }
    public String getResolution() { return resolution; }
    public String getSizeLabel() { return sizeLabel; }
    public Instant getCreatedAt() { return createdAt; }
}
