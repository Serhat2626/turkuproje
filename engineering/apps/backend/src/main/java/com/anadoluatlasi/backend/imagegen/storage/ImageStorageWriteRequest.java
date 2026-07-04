package com.anadoluatlasi.backend.imagegen.storage;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-008: Storage write request — internal model.
 *
 * <p>Validated image byte'larını storage adapter'a iletmek için
 * kullanılan internal request. DTO değildir, admin/user-facing değildir.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Provider URL alanı yoktur.</li>
 *   <li>Public/signed/CDN URL alanı yoktur.</li>
 *   <li>Storage object key client/admin tarafından verilmez — server-side üretilir.</li>
 *   <li>API key/secret/config alanı yoktur.</li>
 *   <li>Raw provider payload alanı yoktur.</li>
 *   <li>Internal prompt alanı yoktur.</li>
 *   <li>byte[] bu internal modelde gereklidir ancak loglanmamalıdır.</li>
 * </ul>
 */
@Data
@Builder
public class ImageStorageWriteRequest {

    /** Target media asset identifier */
    private final UUID mediaAssetId;

    /** Source generated asset identifier */
    private final UUID generatedAssetId;

    /** End-to-end trace identifier */
    private final String correlationId;

    /** Validated MIME type */
    private final String mimeType;

    /** Validated image width */
    private final int width;

    /** Validated image height */
    private final int height;

    /** Validated file size */
    private final long fileSizeBytes;

    /** SHA-256 checksum */
    private final String checksumSha256;

    /** Validated image bytes — internal only, not logged */
    private final byte[] imageBytes;

    /** Write request timestamp */
    private final LocalDateTime requestedAt;
}
