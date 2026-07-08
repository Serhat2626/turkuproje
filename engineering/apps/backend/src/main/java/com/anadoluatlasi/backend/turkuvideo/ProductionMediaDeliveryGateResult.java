package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * PD-006: Delivery gate result — metadata-only. No URL/path/binary.
 */
public final class ProductionMediaDeliveryGateResult {
    private final ProductionMediaDeliveryGateStatus status;
    private final List<ProductionMediaDeliveryGateIssueCode> issueCodes;
    private final String opaqueDeliveryGateRef;
    private final String opaqueStorageObjectRef;
    private final String opaqueArtifactRef;
    private final String opaqueReviewDecisionRef;
    private final String mediaFormat;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final boolean controlledPublicDeliveryPrepEligible;

    public ProductionMediaDeliveryGateResult(
            ProductionMediaDeliveryGateStatus status,
            List<ProductionMediaDeliveryGateIssueCode> issueCodes,
            String opaqueDeliveryGateRef, String opaqueStorageObjectRef,
            String opaqueArtifactRef, String opaqueReviewDecisionRef,
            String mediaFormat, int durationSeconds, int fps,
            int width, int height, long sizeBytes,
            boolean controlledPublicDeliveryPrepEligible) {
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.opaqueDeliveryGateRef = opaqueDeliveryGateRef;
        this.opaqueStorageObjectRef = opaqueStorageObjectRef;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.opaqueReviewDecisionRef = opaqueReviewDecisionRef;
        this.mediaFormat = mediaFormat;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.controlledPublicDeliveryPrepEligible = controlledPublicDeliveryPrepEligible;
    }

    public static ProductionMediaDeliveryGateResult rejected(ProductionMediaDeliveryGateIssueCode... codes) {
        return new ProductionMediaDeliveryGateResult(
                ProductionMediaDeliveryGateStatus.REJECTED_UNSAFE, List.of(codes),
                null, null, null, null, null, 0, 0, 0, 0, 0, false);
    }

    public static ProductionMediaDeliveryGateResult failed(ProductionMediaDeliveryGateIssueCode... codes) {
        return new ProductionMediaDeliveryGateResult(
                ProductionMediaDeliveryGateStatus.FAILED_SAFE, List.of(codes),
                null, null, null, null, null, 0, 0, 0, 0, 0, false);
    }

    public ProductionMediaDeliveryGateStatus getStatus() { return status; }
    public List<ProductionMediaDeliveryGateIssueCode> getIssueCodes() { return issueCodes; }
    public String getOpaqueDeliveryGateRef() { return opaqueDeliveryGateRef; }
    public String getOpaqueStorageObjectRef() { return opaqueStorageObjectRef; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public String getOpaqueReviewDecisionRef() { return opaqueReviewDecisionRef; }
    public String getMediaFormat() { return mediaFormat; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public boolean isControlledPublicDeliveryPrepEligible() { return controlledPublicDeliveryPrepEligible; }
}
