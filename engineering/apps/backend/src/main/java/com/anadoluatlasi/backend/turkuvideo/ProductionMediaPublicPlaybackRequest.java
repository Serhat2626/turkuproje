package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-007: Controlled public playback request — safe metadata only.
 */
public final class ProductionMediaPublicPlaybackRequest {
    private final String opaqueDeliveryGateRef;
    private final ProductionMediaDeliveryGateStatus deliveryGateStatus;
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
    private final boolean controlledPublicDeliveryPrepEligible;

    public ProductionMediaPublicPlaybackRequest(
            String opaqueDeliveryGateRef,
            ProductionMediaDeliveryGateStatus deliveryGateStatus,
            String opaqueStorageObjectRef,
            ProductionMediaStorageObjectStatus storageObjectStatus,
            String opaquePlaybackPreparationRef,
            String opaqueArtifactRef, String mediaFormat,
            int durationSeconds, int fps, int width, int height,
            long sizeBytes, boolean controlledPublicDeliveryPrepEligible) {
        this.opaqueDeliveryGateRef = opaqueDeliveryGateRef;
        this.deliveryGateStatus = deliveryGateStatus;
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
        this.controlledPublicDeliveryPrepEligible = controlledPublicDeliveryPrepEligible;
    }

    public String getOpaqueDeliveryGateRef() { return opaqueDeliveryGateRef; }
    public ProductionMediaDeliveryGateStatus getDeliveryGateStatus() { return deliveryGateStatus; }
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
    public boolean isControlledPublicDeliveryPrepEligible() { return controlledPublicDeliveryPrepEligible; }
}
