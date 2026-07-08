package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-005: Review decision request — safe metadata only.
 */
public final class ProductionMediaReviewDecisionRequest {
    private final String opaqueReviewPackageRef;
    private final String opaquePlaybackPreparationRef;
    private final String opaqueStorageObjectRef;
    private final String opaqueArtifactRef;
    private final ProductionMediaReviewDecisionType decisionType;
    private final String mediaFormat;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final boolean internalPreviewEligible;

    public ProductionMediaReviewDecisionRequest(
            String opaqueReviewPackageRef, String opaquePlaybackPreparationRef,
            String opaqueStorageObjectRef, String opaqueArtifactRef,
            ProductionMediaReviewDecisionType decisionType,
            String mediaFormat, int durationSeconds, int fps,
            int width, int height, long sizeBytes, boolean internalPreviewEligible) {
        this.opaqueReviewPackageRef = opaqueReviewPackageRef;
        this.opaquePlaybackPreparationRef = opaquePlaybackPreparationRef;
        this.opaqueStorageObjectRef = opaqueStorageObjectRef;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.decisionType = decisionType;
        this.mediaFormat = mediaFormat;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.internalPreviewEligible = internalPreviewEligible;
    }

    public String getOpaqueReviewPackageRef() { return opaqueReviewPackageRef; }
    public String getOpaquePlaybackPreparationRef() { return opaquePlaybackPreparationRef; }
    public String getOpaqueStorageObjectRef() { return opaqueStorageObjectRef; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public ProductionMediaReviewDecisionType getDecisionType() { return decisionType; }
    public String getMediaFormat() { return mediaFormat; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public boolean isInternalPreviewEligible() { return internalPreviewEligible; }
}
