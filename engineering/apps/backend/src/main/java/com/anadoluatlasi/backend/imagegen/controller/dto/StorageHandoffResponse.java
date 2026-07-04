package com.anadoluatlasi.backend.imagegen.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-008: Storage handoff safe response DTO.
 *
 * <p>Storage handoff explicit action sonuç DTO'su.
 * storageObjectKey, storageReference, storageBucket, storageProvider,
 * publicUrl, signedUrl, cdnUrl, base64 döndürülmez.</p>
 *
 * <p>Yasak alanlar:</p>
 * <ul>
 *   <li>storageObjectKey, storageReference — internal-only</li>
 *   <li>storageBucket, storageProvider — internal-only</li>
 *   <li>publicUrl, signedUrl, cdnUrl — forbidden</li>
 *   <li>providerUrl — forbidden</li>
 *   <li>base64, binary — forbidden</li>
 *   <li>rawProviderResponse, rawStorageProviderResponse — forbidden</li>
 *   <li>apiKey, secret, internalPrompt — forbidden</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageHandoffResponse {

    private boolean success;
    private UUID mediaAssetId;
    private UUID generatedAssetId;

    private String storageHandoffStatus;
    private String previewStatus;

    private String mimeType;
    private Integer width;
    private Integer height;
    private Long fileSizeBytes;
    private String checksumSha256;
    private LocalDateTime storedAt;

    private String errorCode;
    private String errorMessage;
    private String correlationId;

    // storageObjectKey intentionally excluded — internal-only
    // storageReference intentionally excluded — internal-only
    // storageBucket intentionally excluded — internal-only
    // storageProvider intentionally excluded — internal-only
    // publicUrl intentionally excluded — forbidden
    // signedUrl intentionally excluded — forbidden
    // cdnUrl intentionally excluded — forbidden
    // providerUrl intentionally excluded — forbidden
    // base64 intentionally excluded — forbidden
}
