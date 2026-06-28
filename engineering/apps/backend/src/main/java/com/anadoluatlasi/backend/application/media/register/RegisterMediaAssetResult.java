package com.anadoluatlasi.backend.application.media.register;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Medya varlığı kayıt işleminin sonucu.
 * Entity döndürmez, yalnızca sonuç bilgisi taşır.
 */
@Value
@Builder
public class RegisterMediaAssetResult {

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
