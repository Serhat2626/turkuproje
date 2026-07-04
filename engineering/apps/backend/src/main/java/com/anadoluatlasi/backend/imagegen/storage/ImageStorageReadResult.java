package com.anadoluatlasi.backend.imagegen.storage;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.Builder;
import lombok.Data;

/**
 * FP-008: Storage read result — internal model.
 *
 * <p>Preview proxy için storage'dan okunan image byte'larını taşır.
 * DTO değildir. Public/signed URL döndürülmez.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Public URL alanı yoktur.</li>
 *   <li>Signed URL alanı yoktur.</li>
 *   <li>CDN URL alanı yoktur.</li>
 *   <li>storageObjectKey bu result'ta yer almaz — input'tur.</li>
 *   <li>Base64 alanı yoktur.</li>
 *   <li>Raw storage provider response yoktur.</li>
 *   <li>imageBytes preview proxy tarafından stream edilir, DTO'ya çıkmaz.</li>
 * </ul>
 */
@Data
@Builder
public class ImageStorageReadResult {

    /** Read başarılı mı */
    private final boolean success;

    /** Image bytes — preview proxy stream için, DTO değil */
    private final byte[] imageBytes;

    /** Content MIME type */
    private final String mimeType;

    /** File size */
    private final long fileSizeBytes;

    /** Normalized error code — null if success */
    private final ImageStorageErrorCode errorCode;

    /** Safe normalized error message — null if success */
    private final String errorMessage;

    /** End-to-end trace identifier */
    private final String correlationId;
}
