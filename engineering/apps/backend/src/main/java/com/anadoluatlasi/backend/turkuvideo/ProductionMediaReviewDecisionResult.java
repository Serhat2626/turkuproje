package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * PD-005: Review decision result — metadata-only.
 */
public final class ProductionMediaReviewDecisionResult {
    private final ProductionMediaReviewDecisionStatus status;
    private final ProductionMediaReviewDecisionType decisionType;
    private final List<ProductionMediaReviewDecisionIssueCode> issueCodes;
    private final String opaqueDecisionRef;
    private final boolean internalOnly;

    public ProductionMediaReviewDecisionResult(
            ProductionMediaReviewDecisionStatus status,
            ProductionMediaReviewDecisionType decisionType,
            List<ProductionMediaReviewDecisionIssueCode> issueCodes,
            String opaqueDecisionRef) {
        this.status = status;
        this.decisionType = decisionType;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.opaqueDecisionRef = opaqueDecisionRef;
        this.internalOnly = true;
    }

    public static ProductionMediaReviewDecisionResult rejected(ProductionMediaReviewDecisionIssueCode... codes) {
        return new ProductionMediaReviewDecisionResult(
                ProductionMediaReviewDecisionStatus.REJECTED_UNSAFE, null, List.of(codes), null);
    }

    public static ProductionMediaReviewDecisionResult failed(ProductionMediaReviewDecisionIssueCode... codes) {
        return new ProductionMediaReviewDecisionResult(
                ProductionMediaReviewDecisionStatus.FAILED_SAFE, null, List.of(codes), null);
    }

    public ProductionMediaReviewDecisionStatus getStatus() { return status; }
    public ProductionMediaReviewDecisionType getDecisionType() { return decisionType; }
    public List<ProductionMediaReviewDecisionIssueCode> getIssueCodes() { return issueCodes; }
    public String getOpaqueDecisionRef() { return opaqueDecisionRef; }
    public boolean isInternalOnly() { return internalOnly; }
}
