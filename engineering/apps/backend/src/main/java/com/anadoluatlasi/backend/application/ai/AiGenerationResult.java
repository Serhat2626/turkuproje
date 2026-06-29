package com.anadoluatlasi.backend.application.ai;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-001: AI üretim operasyonlarının sonuç nesnesi.
 * Başarı ve hata durumlarını tek tip olarak taşır.
 */
@Value
@Builder
public class AiGenerationResult {

    /** İşlem başarılı mı */
    boolean success;

    /** İçerik projesi ID */
    UUID contentProjectId;

    /** Oluşturulan draft ID (başarılıysa) */
    UUID contentDraftId;

    /** Proje güncel statüsü */
    String projectStatus;

    /** Draft başlığı (başarılıysa) */
    String draftTitle;

    /** Sonuç mesajı */
    String message;

    /** Hata tipi (başarısızsa) */
    String errorType;

    /** Retry yapılabilir mi (başarısızsa) */
    Boolean retryable;

    /** Güncel retry sayısı */
    Integer retryCount;

    /** Maksimum retry sayısı */
    Integer maxRetryCount;
}
