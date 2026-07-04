package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.Builder;
import lombok.Data;

/**
 * FP-008: Preview proxy internal result model.
 *
 * <p>Backend-controlled preview proxy sonucunu taşır.
 * Image bytes controller tarafından stream edilir.
 * storageObjectKey, public/signed URL, base64 döndürülmez.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>storageObjectKey yoktur — internal adapter'da kalır.</li>
 *   <li>Public URL yoktur.</li>
 *   <li>Signed URL yoktur.</li>
 *   <li>CDN URL yoktur.</li>
 *   <li>Provider URL yoktur.</li>
 *   <li>Base64 yoktur.</li>
 *   <li>imageBytes controller stream için kullanılır, DTO olarak dönmez.</li>
 * </ul>
 */
@Data
@Builder
public class ImagePreviewProxyResult {

    /** Preview başarılı mı */
    private final boolean success;

    /** Image bytes — controller stream için, DTO değil */
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
