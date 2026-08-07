package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public final class TurkuRenderArtifact {
    private final UUID artifactId;
    private final String artifactRef;
    private final String format;
    private final double durationSeconds;
    private final String resolution;
    private final String sizeLabel;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;

    @JsonCreator
    public TurkuRenderArtifact(@JsonProperty("artifactId") UUID artifactId,
                                @JsonProperty("artifactRef") String artifactRef,
                                @JsonProperty("format") String format,
                                @JsonProperty("durationSeconds") double durationSeconds,
                                @JsonProperty("resolution") String resolution,
                                @JsonProperty("sizeLabel") String sizeLabel,
                                @JsonProperty("humanReviewRequired") boolean humanReviewRequired,
                                @JsonProperty("autoApproved") boolean autoApproved) {
        this.artifactId = artifactId;
        this.artifactRef = artifactRef;
        this.format = format;
        this.durationSeconds = durationSeconds;
        this.resolution = resolution;
        this.sizeLabel = sizeLabel;
        this.humanReviewRequired = humanReviewRequired;
        this.autoApproved = autoApproved;
    }

    public TurkuRenderArtifact(UUID artifactId, String artifactRef, String format,
                                double durationSeconds, String resolution, String sizeLabel) {
        this.artifactId = artifactId;
        this.artifactRef = artifactRef;
        this.format = format;
        this.durationSeconds = durationSeconds;
        this.resolution = resolution;
        this.sizeLabel = sizeLabel;
        this.humanReviewRequired = true;
        this.autoApproved = false;
    }

    public UUID getArtifactId() { return artifactId; }
    public String getArtifactRef() { return artifactRef; }
    public String getFormat() { return format; }
    public double getDurationSeconds() { return durationSeconds; }
    public String getResolution() { return resolution; }
    public String getSizeLabel() { return sizeLabel; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
}
