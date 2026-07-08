package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * PD-001: Result of a production media storage operation.
 * Metadata-only — no raw payload, no URL, no path.
 */
public final class ProductionMediaStorageResult {
    private final ProductionMediaStorageObjectStatus status;
    private final List<ProductionMediaStorageIssueCode> issueCodes;
    private final ProductionMediaStorageObjectRef objectRef;
    private final boolean privateOnly;

    public ProductionMediaStorageResult(
            ProductionMediaStorageObjectStatus status,
            List<ProductionMediaStorageIssueCode> issueCodes,
            ProductionMediaStorageObjectRef objectRef) {
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.objectRef = objectRef;
        this.privateOnly = true;
    }

    public static ProductionMediaStorageResult rejected(ProductionMediaStorageIssueCode... codes) {
        return new ProductionMediaStorageResult(
                ProductionMediaStorageObjectStatus.REJECTED_UNSAFE,
                List.of(codes), null);
    }

    public static ProductionMediaStorageResult failed(ProductionMediaStorageIssueCode... codes) {
        return new ProductionMediaStorageResult(
                ProductionMediaStorageObjectStatus.FAILED_SAFE,
                List.of(codes), null);
    }

    public ProductionMediaStorageObjectStatus getStatus() { return status; }
    public List<ProductionMediaStorageIssueCode> getIssueCodes() { return issueCodes; }
    public ProductionMediaStorageObjectRef getObjectRef() { return objectRef; }
    public boolean isPrivateOnly() { return privateOnly; }
}
