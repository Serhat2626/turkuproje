package com.anadoluatlasi.backend.application.visualasset;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-006: Visual Asset Plan üretim sonucu.
 *
 * <p>Service sonucunu admin controller'a taşıyan immutable DTO.
 * Raw prompt, provider payload veya API key içermez.</p>
 */
@Value
@Builder
public class VisualAssetPlanGenerationResult {

    /** İşlem başarılı mı */
    boolean success;

    /** İçerik projesi ID */
    UUID contentProjectId;

    /** Kaynak storyboard ID */
    UUID storyboardDraftId;

    /** Üretilen visual asset plan ID (başarılıysa) */
    UUID visualAssetPlanId;

    /** Versiyon numarası */
    Integer version;

    /** Durum (AI_GENERATED, FAILED, etc.) */
    String status;

    /** Plan başlığı */
    String planTitle;

    /** Sahne plan sayısı */
    Integer scenePlanCount;

    /** Thumbnail plan mevcut mu */
    Boolean hasThumbnailPlan;

    /** Sonuç mesajı */
    String message;

    /** Hata kodu (başarısızsa) */
    String errorCode;
}
