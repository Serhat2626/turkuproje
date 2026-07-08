package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-002: Safe upload request — verified artifact metadata only.
 * No paths, no URLs, no binary, no secrets.
 */
public final class ProductionMediaStorageUploadRequest {
    private final String opaqueArtifactRef;
    private final String mediaFormat;
    private final TurkuRenderArtifactLifecycleStatus lifecycleStatus;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final boolean checksumPresent;
    private final String opaqueChecksumRef;

    public ProductionMediaStorageUploadRequest(
            String opaqueArtifactRef, String mediaFormat,
            TurkuRenderArtifactLifecycleStatus lifecycleStatus,
            int durationSeconds, int fps, int width, int height,
            long sizeBytes, boolean checksumPresent, String opaqueChecksumRef) {
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.mediaFormat = mediaFormat;
        this.lifecycleStatus = lifecycleStatus;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.checksumPresent = checksumPresent;
        this.opaqueChecksumRef = opaqueChecksumRef;
    }

    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public String getMediaFormat() { return mediaFormat; }
    public TurkuRenderArtifactLifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public boolean isChecksumPresent() { return checksumPresent; }
    public String getOpaqueChecksumRef() { return opaqueChecksumRef; }
}
