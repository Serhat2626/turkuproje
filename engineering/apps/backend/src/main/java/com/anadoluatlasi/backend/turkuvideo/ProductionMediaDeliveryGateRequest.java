package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-006: Delivery gate request — safe metadata only.
 */
public final class ProductionMediaDeliveryGateRequest {
    private final String opaqueReviewDecisionRef;
    private final ProductionMediaReviewDecisionStatus reviewDecisionStatus;
    private final String opaqueStorageObjectRef;
    private final ProductionMediaStorageObjectStatus storageObjectStatus;
    private final String opaquePlaybackPreparationRef;
    private final String opaqueArtifactRef;
    private final String mediaFormat;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final boolean internalPreviewEligible;

    public ProductionMediaDeliveryGateRequest(
            String opaqueReviewDecisionRef,
            ProductionMediaReviewDecisionStatus reviewDecisionStatus,
            String opaqueStorageObjectRef,
            ProductionMediaStorageObjectStatus storageObjectStatus,
            String opaquePlaybackPreparationRef,
            String opaqueArtifactRef, String mediaFormat,
            int durationSeconds, int fps, int width, int height,
            long sizeBytes, boolean internalPreviewEligible) {
        this.opaqueReviewDecisionRef = opaqueReviewDecisionRef;
        this.reviewDecisionStatus = reviewDecisionStatus;
        this.opaqueStorageObjectRef = opaqueStorageObjectRef;
        this.storageObjectStatus = storageObjectStatus;
        this.opaquePlaybackPreparationRef = opaquePlaybackPreparationRef;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.mediaFormat = mediaFormat;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.internalPreviewEligible = internalPreviewEligible;
    }

    public String getOpaqueReviewDecisionRef() { return opaqueReviewDecisionRef; }
    public ProductionMediaReviewDecisionStatus getReviewDecisionStatus() { return reviewDecisionStatus; }
    public String getOpaqueStorageObjectRef() { return opaqueStorageObjectRef; }
    public ProductionMediaStorageObjectStatus getStorageObjectStatus() { return storageObjectStatus; }
    public String getOpaquePlaybackPreparationRef() { return opaquePlaybackPreparationRef; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public String getMediaFormat() { return mediaFormat; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public boolean isInternalPreviewEligible() { return internalPreviewEligible; }
}
