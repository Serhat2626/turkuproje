package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * PD-003: Playback preparation service.
 * Metadata-only — no stream, no player, no URL, no bytes.
 */
public class ProductionMediaPlaybackPreparationService {

    private final ProductionMediaPlaybackPreparationPolicy policy = new ProductionMediaPlaybackPreparationPolicy();

    public ProductionMediaPlaybackPreparationResult prepare(ProductionMediaPlaybackPreparationRequest request) {
        if (request == null || request.getOpaqueStorageObjectRef() == null) {
            return ProductionMediaPlaybackPreparationResult.rejected(
                    ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_MISSING);
        }

        if (!policy.isStorageStatusAccepted(request.getStorageStatus())) {
            return mapStorageRejection(request.getStorageStatus());
        }

        if (!policy.isFormatSupported(request.getMediaFormat())) {
            return ProductionMediaPlaybackPreparationResult.rejected(
                    ProductionMediaPlaybackPreparationIssueCode.FORMAT_NOT_SUPPORTED);
        }

        if (!policy.isSizeAllowed(request.getSizeBytes())) {
            return ProductionMediaPlaybackPreparationResult.rejected(
                    ProductionMediaPlaybackPreparationIssueCode.SIZE_EXCEEDS_PLAYBACK_LIMIT);
        }

        if (!policy.isDurationAllowed(request.getDurationSeconds())) {
            return ProductionMediaPlaybackPreparationResult.rejected(
                    ProductionMediaPlaybackPreparationIssueCode.DURATION_EXCEEDS_LIMIT);
        }

        return new ProductionMediaPlaybackPreparationResult(
                "playback-prep:" + UUID.randomUUID().toString().substring(0, 8),
                request.getOpaqueStorageObjectRef(),
                request.getOpaqueArtifactRef(),
                request.getMediaFormat().toUpperCase(),
                request.getDurationSeconds(), request.getFps(),
                request.getWidth(), request.getHeight(), request.getSizeBytes(),
                ProductionMediaPlaybackPreparationStatus.READY_FOR_INTERNAL_PREVIEW_ONLY,
                List.of(), true);
    }

    private ProductionMediaPlaybackPreparationResult mapStorageRejection(ProductionMediaStorageObjectStatus status) {
        if (status == ProductionMediaStorageObjectStatus.REJECTED_UNSAFE)
            return ProductionMediaPlaybackPreparationResult.rejected(
                    ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_REJECTED);
        if (status == ProductionMediaStorageObjectStatus.FAILED_SAFE)
            return ProductionMediaPlaybackPreparationResult.failed(
                    ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_FAILED);
        return ProductionMediaPlaybackPreparationResult.rejected(
                ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_NOT_VERIFIED);
    }
}
