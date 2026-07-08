package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * PD-002: Upload/handoff service — verified artifact → private storage.
 * Uses PD-001 gateway abstraction. CI uses fake gateway only.
 */
public class ProductionMediaStorageUploadService {

    private final ProductionMediaStorageGateway gateway;
    private final ProductionMediaStoragePolicy policy;

    public ProductionMediaStorageUploadService(ProductionMediaStorageGateway gateway) {
        this.gateway = gateway;
        this.policy = new ProductionMediaStoragePolicy();
    }

    public ProductionMediaStorageUploadResult upload(ProductionMediaStorageUploadRequest request) {
        if (request == null) {
            return ProductionMediaStorageUploadResult.rejected(
                    ProductionMediaStorageUploadIssueCode.METADATA_INCOMPLETE);
        }

        // Verify lifecycle status — only VERIFIED artifacts accepted
        if (!isLifecycleVerified(request.getLifecycleStatus())) {
            return mapLifecycleRejection(request.getLifecycleStatus());
        }

        // Validate format/size/checksum via policy
        if (!policy.isFormatAllowed(request.getMediaFormat())) {
            return ProductionMediaStorageUploadResult.rejected(
                    ProductionMediaStorageUploadIssueCode.FORMAT_NOT_ALLOWED);
        }
        if (!policy.isSizeAllowed(request.getSizeBytes())) {
            return ProductionMediaStorageUploadResult.rejected(
                    ProductionMediaStorageUploadIssueCode.SIZE_EXCEEDS_LIMIT);
        }
        if (!request.isChecksumPresent()) {
            return ProductionMediaStorageUploadResult.rejected(
                    ProductionMediaStorageUploadIssueCode.CHECKSUM_MISSING);
        }

        // Handoff to gateway
        try {
            var storageResult = gateway.storePrivate(
                    request.getOpaqueArtifactRef(), request.getMediaFormat(),
                    request.getDurationSeconds(), request.getFps(),
                    request.getWidth(), request.getHeight(),
                    request.getSizeBytes(), request.isChecksumPresent(),
                    request.getOpaqueChecksumRef());

            if (storageResult.getStatus() == ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED) {
                return new ProductionMediaStorageUploadResult(
                        ProductionMediaStorageUploadStatus.PRIVATE_STORAGE_REF_CREATED,
                        List.of(), storageResult.getObjectRef());
            }

            return ProductionMediaStorageUploadResult.rejected(
                    ProductionMediaStorageUploadIssueCode.GATEWAY_REJECTED);
        } catch (Exception e) {
            return ProductionMediaStorageUploadResult.failed(
                    ProductionMediaStorageUploadIssueCode.GATEWAY_FAILED);
        }
    }

    private boolean isLifecycleVerified(TurkuRenderArtifactLifecycleStatus status) {
        return status == TurkuRenderArtifactLifecycleStatus.VERIFIED_METADATA_ONLY;
    }

    private ProductionMediaStorageUploadResult mapLifecycleRejection(TurkuRenderArtifactLifecycleStatus status) {
        if (status == TurkuRenderArtifactLifecycleStatus.REJECTED_UNSAFE)
            return ProductionMediaStorageUploadResult.rejected(
                    ProductionMediaStorageUploadIssueCode.ARTIFACT_LIFECYCLE_REJECTED);
        if (status == TurkuRenderArtifactLifecycleStatus.FAILED_SAFE)
            return ProductionMediaStorageUploadResult.rejected(
                    ProductionMediaStorageUploadIssueCode.ARTIFACT_LIFECYCLE_FAILED);
        return ProductionMediaStorageUploadResult.rejected(
                ProductionMediaStorageUploadIssueCode.ARTIFACT_NOT_VERIFIED);
    }
}
