package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * PD-007: Controlled public playback result — opaque handle only.
 * No direct URL/path/bucket/key exposure.
 */
public final class ProductionMediaPublicPlaybackResult {
    private final ProductionMediaPublicPlaybackStatus status;
    private final List<ProductionMediaPublicPlaybackIssueCode> issueCodes;
    private final String opaquePublicPlaybackRef;
    private final String opaqueDeliveryGateRef;
    private final String opaqueArtifactRef;
    private final String mediaFormat;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final boolean controlledPlaybackEligible;

    public ProductionMediaPublicPlaybackResult(
            ProductionMediaPublicPlaybackStatus status,
            List<ProductionMediaPublicPlaybackIssueCode> issueCodes,
            String opaquePublicPlaybackRef, String opaqueDeliveryGateRef,
            String opaqueArtifactRef, String mediaFormat,
            int durationSeconds, int fps, int width, int height,
            long sizeBytes, boolean controlledPlaybackEligible) {
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.opaquePublicPlaybackRef = opaquePublicPlaybackRef;
        this.opaqueDeliveryGateRef = opaqueDeliveryGateRef;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.mediaFormat = mediaFormat;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.controlledPlaybackEligible = controlledPlaybackEligible;
    }

    public static ProductionMediaPublicPlaybackResult rejected(ProductionMediaPublicPlaybackIssueCode... codes) {
        return new ProductionMediaPublicPlaybackResult(
                ProductionMediaPublicPlaybackStatus.REJECTED_UNSAFE, List.of(codes),
                null, null, null, null, 0, 0, 0, 0, 0, false);
    }

    public static ProductionMediaPublicPlaybackResult failed(ProductionMediaPublicPlaybackIssueCode... codes) {
        return new ProductionMediaPublicPlaybackResult(
                ProductionMediaPublicPlaybackStatus.FAILED_SAFE, List.of(codes),
                null, null, null, null, 0, 0, 0, 0, 0, false);
    }

    public ProductionMediaPublicPlaybackStatus getStatus() { return status; }
    public List<ProductionMediaPublicPlaybackIssueCode> getIssueCodes() { return issueCodes; }
    public String getOpaquePublicPlaybackRef() { return opaquePublicPlaybackRef; }
    public String getOpaqueDeliveryGateRef() { return opaqueDeliveryGateRef; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public String getMediaFormat() { return mediaFormat; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public boolean isControlledPlaybackEligible() { return controlledPlaybackEligible; }
}
