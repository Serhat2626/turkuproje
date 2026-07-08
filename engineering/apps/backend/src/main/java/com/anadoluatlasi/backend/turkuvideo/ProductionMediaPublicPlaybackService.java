package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * PD-007: Controlled public playback service.
 * Creates opaque playback handle after full gate validation.
 * No direct URL/path/bucket exposure.
 */
public class ProductionMediaPublicPlaybackService {

    private final ProductionMediaPublicPlaybackPolicy policy = new ProductionMediaPublicPlaybackPolicy();

    public ProductionMediaPublicPlaybackResult createPlaybackHandle(ProductionMediaPublicPlaybackRequest request) {
        if (request == null) {
            return ProductionMediaPublicPlaybackResult.rejected(
                    ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_MISSING);
        }

        // Gate check
        if (request.getOpaqueDeliveryGateRef() == null) {
            return ProductionMediaPublicPlaybackResult.rejected(
                    ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_MISSING);
        }
        if (!request.isControlledPublicDeliveryPrepEligible()) {
            return ProductionMediaPublicPlaybackResult.rejected(
                    ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_NOT_APPROVED);
        }
        if (!policy.isGateApproved(request.getDeliveryGateStatus())) {
            return mapGateRejection(request.getDeliveryGateStatus());
        }

        // Storage check
        if (request.getOpaqueStorageObjectRef() == null) {
            return ProductionMediaPublicPlaybackResult.rejected(
                    ProductionMediaPublicPlaybackIssueCode.STORAGE_REF_MISSING);
        }
        if (!policy.isStorageVerified(request.getStorageObjectStatus())) {
            return ProductionMediaPublicPlaybackResult.rejected(
                    ProductionMediaPublicPlaybackIssueCode.STORAGE_REF_NOT_VERIFIED);
        }

        // Playback prep check
        if (request.getOpaquePlaybackPreparationRef() == null) {
            return ProductionMediaPublicPlaybackResult.rejected(
                    ProductionMediaPublicPlaybackIssueCode.PLAYBACK_PREP_MISSING);
        }

        // Format/size
        if (!policy.isFormatAllowed(request.getMediaFormat())) {
            return ProductionMediaPublicPlaybackResult.rejected(
                    ProductionMediaPublicPlaybackIssueCode.FORMAT_NOT_SUPPORTED);
        }
        if (!policy.isSizeAllowed(request.getSizeBytes())) {
            return ProductionMediaPublicPlaybackResult.rejected(
                    ProductionMediaPublicPlaybackIssueCode.SIZE_EXCEEDS_LIMIT);
        }

        return new ProductionMediaPublicPlaybackResult(
                ProductionMediaPublicPlaybackStatus.READY_FOR_CONTROLLED_PUBLIC_PLAYBACK,
                List.of(),
                "pub-play:" + UUID.randomUUID().toString().substring(0, 8),
                request.getOpaqueDeliveryGateRef(),
                request.getOpaqueArtifactRef(),
                request.getMediaFormat().toUpperCase(),
                request.getDurationSeconds(), request.getFps(),
                request.getWidth(), request.getHeight(), request.getSizeBytes(),
                true);
    }

    private ProductionMediaPublicPlaybackResult mapGateRejection(ProductionMediaDeliveryGateStatus status) {
        if (status == ProductionMediaDeliveryGateStatus.REJECTED_UNSAFE)
            return ProductionMediaPublicPlaybackResult.rejected(ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_REJECTED);
        if (status == ProductionMediaDeliveryGateStatus.FAILED_SAFE)
            return ProductionMediaPublicPlaybackResult.failed(ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_FAILED);
        return ProductionMediaPublicPlaybackResult.rejected(ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_NOT_APPROVED);
    }
}
