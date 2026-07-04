package com.anadoluatlasi.backend.imagegen.storage;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FP-008: Storage write result — internal model.
 *
 * <p>Storage adapter write sonucunu taşır. storageObjectKey internal-only'dir
 * ve admin/user-facing DTO'ya çıkmaz.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>storageObjectKey internal-only — admin DTO'ya çıkmaz.</li>
 *   <li>Public URL alanı yoktur.</li>
 *   <li>Signed URL alanı yoktur.</li>
 *   <li>CDN URL alanı yoktur.</li>
 *   <li>Base64 alanı yoktur.</li>
 *   <li>Raw storage provider response alanı yoktur.</li>
 *   <li>Secret/config alanı yoktur.</li>
 * </ul>
 */
@Data
@Builder
public class ImageStorageWriteResult {

    /** Write başarılı mı */
    private final boolean success;

    /** Storage provider alias (e.g. "mock", "s3") — internal */
    private final String storageProviderAlias;

    /** Storage object key — internal-only, admin DTO'ya çıkmaz */
    private final String storageObjectKey;

    /** Stored MIME type */
    private final String mimeType;

    /** Stored width */
    private final int width;

    /** Stored height */
    private final int height;

    /** Stored file size */
    private final long fileSizeBytes;

    /** Stored checksum */
    private final String checksumSha256;

    /** Storage completion timestamp */
    private final LocalDateTime storedAt;

    /** Normalized error code — null if success */
    private final ImageStorageErrorCode errorCode;

    /** Safe normalized error message — null if success */
    private final String errorMessage;

    /** End-to-end trace identifier */
    private final String correlationId;
}
