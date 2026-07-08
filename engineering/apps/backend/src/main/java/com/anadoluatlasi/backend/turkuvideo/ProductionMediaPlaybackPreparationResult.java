package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * PD-003: Playback preparation result — metadata-only.
 * No URL, no stream, no player, no binary.
 */
public final class ProductionMediaPlaybackPreparationResult {
    private final String opaquePlaybackPreparationRef;
    private final String opaqueStorageObjectRef;
    private final String opaqueArtifactRef;
    private final String mediaFormat;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final ProductionMediaPlaybackPreparationStatus status;
    private final List<ProductionMediaPlaybackPreparationIssueCode> issueCodes;
    private final boolean internalPreviewEligible;

    public ProductionMediaPlaybackPreparationResult(
            String opaquePlaybackPreparationRef, String opaqueStorageObjectRef,
            String opaqueArtifactRef, String mediaFormat,
            int durationSeconds, int fps, int width, int height, long sizeBytes,
            ProductionMediaPlaybackPreparationStatus status,
            List<ProductionMediaPlaybackPreparationIssueCode> issueCodes,
            boolean internalPreviewEligible) {
        this.opaquePlaybackPreparationRef = opaquePlaybackPreparationRef;
        this.opaqueStorageObjectRef = opaqueStorageObjectRef;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.mediaFormat = mediaFormat;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.internalPreviewEligible = internalPreviewEligible;
    }

    public static ProductionMediaPlaybackPreparationResult rejected(
            ProductionMediaPlaybackPreparationIssueCode... codes) {
        return new ProductionMediaPlaybackPreparationResult(
                null, null, null, null, 0, 0, 0, 0, 0,
                ProductionMediaPlaybackPreparationStatus.REJECTED_UNSAFE,
                List.of(codes), false);
    }

    public static ProductionMediaPlaybackPreparationResult failed(
            ProductionMediaPlaybackPreparationIssueCode... codes) {
        return new ProductionMediaPlaybackPreparationResult(
                null, null, null, null, 0, 0, 0, 0, 0,
                ProductionMediaPlaybackPreparationStatus.FAILED_SAFE,
                List.of(codes), false);
    }

    public String getOpaquePlaybackPreparationRef() { return opaquePlaybackPreparationRef; }
    public String getOpaqueStorageObjectRef() { return opaqueStorageObjectRef; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public String getMediaFormat() { return mediaFormat; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public ProductionMediaPlaybackPreparationStatus getStatus() { return status; }
    public List<ProductionMediaPlaybackPreparationIssueCode> getIssueCodes() { return issueCodes; }
    public boolean isInternalPreviewEligible() { return internalPreviewEligible; }
}
