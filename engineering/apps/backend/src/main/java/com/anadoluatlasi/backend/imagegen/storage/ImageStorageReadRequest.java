package com.anadoluatlasi.backend.imagegen.storage;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * FP-008: Storage read request — internal model.
 *
 * <p>Preview proxy için storage'dan image okumak için kullanılır.
 * storageObjectKey internal-only'dir — admin/user tarafından verilmez.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>storageObjectKey admin/user DTO'ya çıkmaz.</li>
 *   <li>Public/signed/CDN URL alanı yoktur.</li>
 * </ul>
 */
@Data
@Builder
public class ImageStorageReadRequest {

    /** Internal storage object key */
    private final String storageObjectKey;

    /** Media asset identifier — audit/trace */
    private final UUID mediaAssetId;

    /** Generated asset identifier — audit/trace */
    private final UUID generatedAssetId;

    /** End-to-end trace identifier */
    private final String correlationId;
}
