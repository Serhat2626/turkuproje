package com.anadoluatlasi.backend.media.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Medya varlığı kayıt isteği DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterMediaAssetRequest {

    @NotBlank(message = "Varlık türü boş olamaz")
    private String assetType;

    @NotBlank(message = "Depolama sağlayıcısı boş olamaz")
    private String storageProvider;

    @NotBlank(message = "Bucket boş olamaz")
    @Size(max = 255, message = "Bucket adı en fazla 255 karakter olabilir")
    private String bucket;

    @NotBlank(message = "Nesne anahtarı boş olamaz")
    private String objectKey;

    @Size(max = 500, message = "Dosya adı en fazla 500 karakter olabilir")
    private String fileName;

    private String mimeType;

    @PositiveOrZero(message = "Dosya boyutu negatif olamaz")
    private Long sizeBytes;

    @PositiveOrZero(message = "Süre negatif olamaz")
    private Integer durationSeconds;
}
