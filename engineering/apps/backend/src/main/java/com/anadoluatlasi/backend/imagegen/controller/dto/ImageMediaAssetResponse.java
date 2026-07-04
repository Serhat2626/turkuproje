package com.anadoluatlasi.backend.imagegen.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007/FP-008: Image media asset safe response DTO.
 *
 * <p>Storage key, bucket ve provider internal bilgileri döndürülmez.
 * Yalnızca güvenli dosya metadata döner.</p>
 *
 * <p>FP-008: storageHandoffStatus, previewStatus, storageErrorCode,
 * storageErrorMessage, storedAt safe alanları eklendi.</p>
 *
 * <p>Yasak alanlar: storageObjectKey, storageBucket, storageProvider,
 * publicUrl, signedUrl, cdnUrl, providerUrl, base64.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageMediaAssetResponse {
    private UUID id;
    private String assetKind;
    private String originalFileName;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Long fileSizeBytes;
    private String checksumSha256;
    private String reviewStatus;

    // ── FP-008: Safe Storage/Preview Status ─────────────────────────
    private String storageHandoffStatus;
    private String previewStatus;
    private String storageErrorCode;
    private String storageErrorMessage;
    private LocalDateTime storedAt;

    // storageObjectKey intentionally excluded — internal-only
    // storageBucket intentionally excluded — internal-only
    // storageProvider intentionally excluded — internal-only
    // publicUrl intentionally excluded — forbidden
    // signedUrl intentionally excluded — forbidden
    // cdnUrl intentionally excluded — forbidden
    // providerUrl intentionally excluded — forbidden
    // base64 intentionally excluded — forbidden
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
