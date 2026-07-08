package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * PD-005: Review decision service. Human/internal only. No auto-approval.
 */
public class ProductionMediaReviewDecisionService {

    public ProductionMediaReviewDecisionResult recordDecision(ProductionMediaReviewDecisionRequest request) {
        if (request == null) {
            return ProductionMediaReviewDecisionResult.rejected(
                    ProductionMediaReviewDecisionIssueCode.PREVIEW_METADATA_MISSING);
        }

        if (request.getOpaquePlaybackPreparationRef() == null || request.getOpaqueStorageObjectRef() == null) {
            return ProductionMediaReviewDecisionResult.rejected(
                    ProductionMediaReviewDecisionIssueCode.STORAGE_REF_MISSING);
        }

        if (!request.isInternalPreviewEligible()) {
            return ProductionMediaReviewDecisionResult.rejected(
                    ProductionMediaReviewDecisionIssueCode.PREVIEW_NOT_ELIGIBLE);
        }

        if (request.getDecisionType() == null) {
            return ProductionMediaReviewDecisionResult.rejected(
                    ProductionMediaReviewDecisionIssueCode.DECISION_TYPE_INVALID);
        }

        var status = mapDecisionToStatus(request.getDecisionType());
        return new ProductionMediaReviewDecisionResult(
                status, request.getDecisionType(), List.of(),
                "decision:" + UUID.randomUUID().toString().substring(0, 8));
    }

    private ProductionMediaReviewDecisionStatus mapDecisionToStatus(ProductionMediaReviewDecisionType type) {
        return switch (type) {
            case APPROVE_FOR_INTERNAL_DELIVERY_PREP -> ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP;
            case REJECT_FOR_INTERNAL_REVISION -> ProductionMediaReviewDecisionStatus.REJECTED_FOR_INTERNAL_REVISION;
            case REQUEST_INTERNAL_FIX -> ProductionMediaReviewDecisionStatus.INTERNAL_FIX_REQUESTED;
            case MARK_UNSAFE_INTERNAL_ONLY -> ProductionMediaReviewDecisionStatus.REJECTED_UNSAFE;
        };
    }
}
