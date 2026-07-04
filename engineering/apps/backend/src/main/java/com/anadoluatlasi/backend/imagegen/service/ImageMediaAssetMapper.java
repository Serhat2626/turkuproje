package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageMediaAssetResponse;
import com.anadoluatlasi.backend.imagegen.entity.ImageMediaAsset;

/**
 * FP-007: ImageMediaAsset entity → safe response DTO dönüşümü.
 *
 * <p>storageObjectKey, storageBucket, storageProvider admin-facing
 * DTO'ya dahil edilmez. publicUrl, signedUrl, cdnUrl döndürülmez.</p>
 */
public final class ImageMediaAssetMapper {

    private ImageMediaAssetMapper() {
    }

    public static ImageMediaAssetResponse toResponse(ImageMediaAsset entity) {
        return ImageMediaAssetResponse.builder()
                .id(entity.getId())
                .assetKind(entity.getAssetKind() != null ? entity.getAssetKind().name() : null)
                .originalFileName(entity.getOriginalFileName())
                .mimeType(entity.getMimeType())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .fileSizeBytes(entity.getFileSizeBytes())
                .checksumSha256(entity.getChecksumSha256())
                // storageObjectKey intentionally excluded — internal-only
                // storageBucket intentionally excluded — internal-only
                // storageProvider intentionally excluded — internal-only
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
