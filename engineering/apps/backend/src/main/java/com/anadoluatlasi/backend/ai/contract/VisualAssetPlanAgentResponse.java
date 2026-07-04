package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-006: Visual Asset Plan üretim agent'ından dönen yanıt contract'ı.
 *
 * <p>Bu contract dahili AI/application sınırında kullanılır.
 * REST katmanına expose edilmez.</p>
 *
 * <p>Raw provider payload, raw prompt ve API key/secret asla dahil edilmez.</p>
 *
 * <p>success=true ise output alanları dolu, success=false ise hata alanları dolu.</p>
 */
@Value
@Builder
public class VisualAssetPlanAgentResponse {

    /** İşlem başarılı mı */
    boolean success;

    // ── Success fields ────────────────────────────────────────────

    /** Plan başlığı */
    String planTitle;

    /** Bütünsel görsel stil yönlendirmesi */
    String overallVisualStyle;

    /** Aspect ratio stratejisi */
    String aspectRatioStrategy;

    /** Kültürel/tarihsel doğruluk rehberliği */
    String culturalAuthenticityGuidance;

    /** Karakter görsel süreklilik notları */
    String characterContinuityGuidance;

    /** Mekan görsel süreklilik notları */
    String locationContinuityGuidance;

    /** Renk paleti rehberliği */
    String colorPaletteGuidance;

    /** Sahne bazlı görsel planlar */
    List<VisualAssetSceneOutput> scenePlans;

    /** Thumbnail görsel planı */
    VisualAssetThumbnailOutput thumbnailPlan;

    /** AI uyarıları */
    List<String> warnings;

    // ── Provider metadata (safe) ──────────────────────────────────

    /** Safe provider metadata: provider tipi */
    String providerType;

    /** Safe provider metadata: model adı */
    String modelName;

    // ── Error fields ──────────────────────────────────────────────

    /** Hata kodu (başarısızsa) */
    String errorCode;

    /** Sanitized hata mesajı (başarısızsa) */
    String errorMessage;

    /** Retry yapılabilir mi */
    Boolean retryable;

    // ── Timestamp ─────────────────────────────────────────────────

    /** Üretim zamanı */
    LocalDateTime generatedAt;
}
