package com.anadoluatlasi.backend.application.media.register;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * Yeni bir medya varlığı (MediaAsset) kaydetmek için gerekli giriş verisi.
 * İş kuralı içermez, yalnızca veri taşır.
 */
@Value
@Builder
public class RegisterMediaAssetCommand {

    @NotBlank(message = "Varlık türü boş olamaz")
    String assetType;

    @NotBlank(message = "Depolama sağlayıcısı boş olamaz")
    String storageProvider;

    @NotBlank(message = "Bucket boş olamaz")
    @Size(max = 255, message = "Bucket adı en fazla 255 karakter olabilir")
    String bucket;

    @NotBlank(message = "Nesne anahtarı boş olamaz")
    String objectKey;

    @Size(max = 500, message = "Dosya adı en fazla 500 karakter olabilir")
    String fileName;

    String mimeType;

    Long sizeBytes;

    Integer durationSeconds;

    String metadataJson;
}
