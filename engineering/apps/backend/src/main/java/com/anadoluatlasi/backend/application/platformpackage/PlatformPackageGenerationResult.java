package com.anadoluatlasi.backend.application.platformpackage;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * FP-004: Platform paketi üretim operasyonunun sonuç nesnesi.
 * Başarı ve hata durumlarını tek tip olarak taşır.
 * FP-003 StoryboardGenerationResult pattern'ı ile tutarlıdır.
 */
@Value
@Builder
public class PlatformPackageGenerationResult {

    /** İşlem başarılı mı */
    boolean success;

    /** Content Project ID */
    UUID contentProjectId;

    /** Kaynak StoryboardDraft ID */
    UUID sourceStoryboardDraftId;

    /** Üretilen paket ID'leri */
    List<UUID> generatedPackageIds;

    /** Üretilen paket sayısı */
    Integer packageCount;

    /** Sonuç mesajı */
    String message;

    /** Hata kodu (başarısızsa) */
    String errorCode;
}
