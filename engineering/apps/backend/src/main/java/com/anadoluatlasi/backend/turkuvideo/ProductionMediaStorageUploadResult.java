package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * PD-002: Upload result — metadata-only, private-only.
 * No URL, no path, no raw payload.
 */
public final class ProductionMediaStorageUploadResult {
    private final ProductionMediaStorageUploadStatus status;
    private final List<ProductionMediaStorageUploadIssueCode> issueCodes;
    private final ProductionMediaStorageObjectRef privateObjectRef;
    private final boolean privateOnly;

    public ProductionMediaStorageUploadResult(
            ProductionMediaStorageUploadStatus status,
            List<ProductionMediaStorageUploadIssueCode> issueCodes,
            ProductionMediaStorageObjectRef privateObjectRef) {
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.privateObjectRef = privateObjectRef;
        this.privateOnly = true;
    }

    public static ProductionMediaStorageUploadResult rejected(ProductionMediaStorageUploadIssueCode... codes) {
        return new ProductionMediaStorageUploadResult(
                ProductionMediaStorageUploadStatus.REJECTED_UNSAFE, List.of(codes), null);
    }

    public static ProductionMediaStorageUploadResult failed(ProductionMediaStorageUploadIssueCode... codes) {
        return new ProductionMediaStorageUploadResult(
                ProductionMediaStorageUploadStatus.FAILED_SAFE, List.of(codes), null);
    }

    public ProductionMediaStorageUploadStatus getStatus() { return status; }
    public List<ProductionMediaStorageUploadIssueCode> getIssueCodes() { return issueCodes; }
    public ProductionMediaStorageObjectRef getPrivateObjectRef() { return privateObjectRef; }
    public boolean isPrivateOnly() { return privateOnly; }
}
