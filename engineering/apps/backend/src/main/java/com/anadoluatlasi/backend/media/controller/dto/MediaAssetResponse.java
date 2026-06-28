package com.anadoluatlasi.backend.media.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Medya varlığı yanıt DTO'su.
 * Entity değildir, UseCase Result'tan üretilir.
 */
@Value
@Builder
public class MediaAssetResponse {

    UUID id;
    String assetType;
    String storageProvider;
    String bucket;
    String objectKey;
    String fileName;
    String mimeType;
    Long sizeBytes;
    LocalDateTime createdAt;
}
