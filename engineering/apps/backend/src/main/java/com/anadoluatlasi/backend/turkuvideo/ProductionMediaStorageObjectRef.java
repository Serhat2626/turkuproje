package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * PD-001: Opaque reference to a private storage object.
 * No bucket/key/path/URL fields.
 */
public final class ProductionMediaStorageObjectRef {
    private final UUID refId;
    private final String opaqueStorageObjectRef;
    private final String opaqueArtifactRef;
    private final ProductionMediaStorageProviderType providerType;
    private final ProductionMediaStorageMode storageMode;
    private final ProductionMediaStorageObjectStatus objectStatus;
    private final String mediaFormat;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final boolean checksumPresent;
    private final String opaqueChecksumRef;

    public ProductionMediaStorageObjectRef(
            UUID refId, String opaqueStorageObjectRef, String opaqueArtifactRef,
            ProductionMediaStorageProviderType providerType, ProductionMediaStorageMode storageMode,
            ProductionMediaStorageObjectStatus objectStatus, String mediaFormat,
            int durationSeconds, int fps, int width, int height, long sizeBytes,
            boolean checksumPresent, String opaqueChecksumRef) {
        this.refId = refId;
        this.opaqueStorageObjectRef = opaqueStorageObjectRef;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.providerType = providerType;
        this.storageMode = storageMode;
        this.objectStatus = objectStatus;
        this.mediaFormat = mediaFormat;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.checksumPresent = checksumPresent;
        this.opaqueChecksumRef = opaqueChecksumRef;
    }

    public UUID getRefId() { return refId; }
    public String getOpaqueStorageObjectRef() { return opaqueStorageObjectRef; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public ProductionMediaStorageProviderType getProviderType() { return providerType; }
    public ProductionMediaStorageMode getStorageMode() { return storageMode; }
    public ProductionMediaStorageObjectStatus getObjectStatus() { return objectStatus; }
    public String getMediaFormat() { return mediaFormat; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public boolean isChecksumPresent() { return checksumPresent; }
    public String getOpaqueChecksumRef() { return opaqueChecksumRef; }
}
