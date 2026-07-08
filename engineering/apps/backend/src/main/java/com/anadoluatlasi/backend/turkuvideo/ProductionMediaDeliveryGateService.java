package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * PD-006: Delivery gate service. Validates all prerequisites.
 * No public delivery. No URL generation. No auto-approval.
 */
public class ProductionMediaDeliveryGateService {

    private final ProductionMediaDeliveryGatePolicy policy = new ProductionMediaDeliveryGatePolicy();

    public ProductionMediaDeliveryGateResult evaluate(ProductionMediaDeliveryGateRequest request) {
        if (request == null) {
            return ProductionMediaDeliveryGateResult.rejected(
                    ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_MISSING);
        }

        // Human decision check
        if (request.getOpaqueReviewDecisionRef() == null) {
            return ProductionMediaDeliveryGateResult.rejected(
                    ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_MISSING);
        }
        if (!policy.isHumanDecisionApproved(request.getReviewDecisionStatus())) {
            return mapDecisionRejection(request.getReviewDecisionStatus());
        }

        // Storage ref check
        if (request.getOpaqueStorageObjectRef() == null) {
            return ProductionMediaDeliveryGateResult.rejected(
                    ProductionMediaDeliveryGateIssueCode.STORAGE_REF_MISSING);
        }
        if (!policy.isStorageRefVerified(request.getStorageObjectStatus())) {
            return mapStorageRejection(request.getStorageObjectStatus());
        }

        // Playback metadata check
        if (request.getOpaquePlaybackPreparationRef() == null) {
            return ProductionMediaDeliveryGateResult.rejected(
                    ProductionMediaDeliveryGateIssueCode.PLAYBACK_METADATA_MISSING);
        }

        // Format/size check
        if (!policy.isFormatAllowed(request.getMediaFormat())) {
            return ProductionMediaDeliveryGateResult.rejected(
                    ProductionMediaDeliveryGateIssueCode.FORMAT_NOT_SUPPORTED);
        }
        if (!policy.isSizeAllowed(request.getSizeBytes())) {
            return ProductionMediaDeliveryGateResult.rejected(
                    ProductionMediaDeliveryGateIssueCode.SIZE_EXCEEDS_LIMIT);
        }

        return new ProductionMediaDeliveryGateResult(
                ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                List.of(),
                "gate:" + UUID.randomUUID().toString().substring(0, 8),
                request.getOpaqueStorageObjectRef(),
                request.getOpaqueArtifactRef(),
                request.getOpaqueReviewDecisionRef(),
                request.getMediaFormat().toUpperCase(),
                request.getDurationSeconds(), request.getFps(),
                request.getWidth(), request.getHeight(), request.getSizeBytes(),
                true);
    }

    private ProductionMediaDeliveryGateResult mapDecisionRejection(ProductionMediaReviewDecisionStatus status) {
        if (status == ProductionMediaReviewDecisionStatus.REJECTED_FOR_INTERNAL_REVISION)
            return ProductionMediaDeliveryGateResult.rejected(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_REJECTED);
        if (status == ProductionMediaReviewDecisionStatus.INTERNAL_FIX_REQUESTED)
            return ProductionMediaDeliveryGateResult.rejected(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_FIX_REQUESTED);
        if (status == ProductionMediaReviewDecisionStatus.REJECTED_UNSAFE)
            return ProductionMediaDeliveryGateResult.rejected(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_UNSAFE);
        return ProductionMediaDeliveryGateResult.rejected(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_NOT_APPROVED);
    }

    private ProductionMediaDeliveryGateResult mapStorageRejection(ProductionMediaStorageObjectStatus status) {
        if (status == ProductionMediaStorageObjectStatus.FAILED_SAFE)
            return ProductionMediaDeliveryGateResult.rejected(ProductionMediaDeliveryGateIssueCode.STORAGE_REF_FAILED);
        return ProductionMediaDeliveryGateResult.rejected(ProductionMediaDeliveryGateIssueCode.STORAGE_REF_NOT_VERIFIED);
    }
}
