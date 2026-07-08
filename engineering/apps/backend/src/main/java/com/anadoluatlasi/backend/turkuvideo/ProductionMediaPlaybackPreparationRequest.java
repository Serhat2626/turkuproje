package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-003: Playback preparation request — private storage ref metadata only.
 */
public final class ProductionMediaPlaybackPreparationRequest {
    private final String opaqueStorageObjectRef;
    private final String opaqueArtifactRef;
    private final ProductionMediaStorageObjectStatus storageStatus;
    private final String mediaFormat;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;

    public ProductionMediaPlaybackPreparationRequest(
            String opaqueStorageObjectRef, String opaqueArtifactRef,
            ProductionMediaStorageObjectStatus storageStatus, String mediaFormat,
            int durationSeconds, int fps, int width, int height, long sizeBytes) {
        this.opaqueStorageObjectRef = opaqueStorageObjectRef;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.storageStatus = storageStatus;
        this.mediaFormat = mediaFormat;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
    }

    public String getOpaqueStorageObjectRef() { return opaqueStorageObjectRef; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public ProductionMediaStorageObjectStatus getStorageStatus() { return storageStatus; }
    public String getMediaFormat() { return mediaFormat; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
}
