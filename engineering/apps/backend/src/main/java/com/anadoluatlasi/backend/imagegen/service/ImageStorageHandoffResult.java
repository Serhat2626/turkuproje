package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImagePreviewStatus;
import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import com.anadoluatlasi.backend.imagegen.entity.ImageStorageHandoffStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-008: Storage handoff safe result model.
 *
 * <p>Handoff sonucunu taşır. storageObjectKey, provider URL,
 * signed/public URL, base64 bu modelde yer almaz.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>storageObjectKey yoktur — internal adapter result'ta kalır.</li>
 *   <li>Provider URL yoktur.</li>
 *   <li>Public/signed/CDN URL yoktur.</li>
 *   <li>Base64 yoktur.</li>
 *   <li>Raw storage/provider response yoktur.</li>
 *   <li>Secret/API key yoktur.</li>
 * </ul>
 */
@Data
@Builder
public class ImageStorageHandoffResult {

    /** Handoff başarılı mı */
    private final boolean success;

    /** Media asset identifier */
    private final UUID mediaAssetId;

    /** Generated asset identifier — lineage */
    private final UUID generatedAssetId;

    /** Final storage handoff status */
    private final ImageStorageHandoffStatus storageHandoffStatus;

    /** Final preview status */
    private final ImagePreviewStatus previewStatus;

    /** Validated/stored MIME type */
    private final String mimeType;

    /** Validated/stored width */
    private final int width;

    /** Validated/stored height */
    private final int height;

    /** Validated/stored file size */
    private final long fileSizeBytes;

    /** Validated/stored checksum */
    private final String checksumSha256;

    /** Storage completion timestamp — null if failed */
    private final LocalDateTime storedAt;

    /** Normalized error code — null if success */
    private final ImageStorageErrorCode errorCode;

    /** Safe normalized error message — null if success */
    private final String errorMessage;

    /** End-to-end trace identifier */
    private final String correlationId;
}
