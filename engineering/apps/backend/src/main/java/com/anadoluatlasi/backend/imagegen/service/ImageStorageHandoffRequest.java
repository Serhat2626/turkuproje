package com.anadoluatlasi.backend.imagegen.service;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-008: Storage handoff internal request model.
 *
 * <p>Storage handoff service'e validated image bytes iletmek için
 * kullanılır. DTO değildir — admin/user-facing değildir.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Provider URL alanı yoktur.</li>
 *   <li>Public/signed/CDN URL alanı yoktur.</li>
 *   <li>Storage object key client/admin tarafından verilmez.</li>
 *   <li>API key/secret/config alanı yoktur.</li>
 *   <li>Raw provider payload/response alanı yoktur.</li>
 *   <li>Internal prompt alanı yoktur.</li>
 *   <li>byte[] loglanmamalıdır.</li>
 * </ul>
 */
@Data
@Builder
public class ImageStorageHandoffRequest {

    /** Target media asset identifier */
    private final UUID mediaAssetId;

    /** Source generated asset identifier — lineage */
    private final UUID generatedAssetId;

    /** Image bytes to validate and store */
    private final byte[] imageBytes;

    /** Optional content type hint from provider */
    private final String contentTypeHint;

    /** End-to-end trace identifier */
    private final String correlationId;

    /** Actor who requested the handoff */
    private final String requestedBy;

    /** Handoff request timestamp */
    private final LocalDateTime requestedAt;
}
