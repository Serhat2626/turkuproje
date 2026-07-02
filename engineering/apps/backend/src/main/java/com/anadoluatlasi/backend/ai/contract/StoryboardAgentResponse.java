package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-003: AI Storyboard Agent'ın normalize edilmiş yanıt contract'ı.
 * Provider raw response değildir — AI Platform tarafından normalize edilmiştir.
 *
 * <p>success=true ise storyboard alanları dolu, success=false ise hata alanları dolu.</p>
 *
 * <p>Raw provider payload, raw prompt ve API key/secret asla dahil edilmez.</p>
 */
@Value
@Builder
public class StoryboardAgentResponse {

    /** İşlem başarılı mı */
    boolean success;

    /** Storyboard başlığı */
    String storyboardTitle;

    /** Kısa özet */
    String summary;

    /** Hedef platform */
    String targetPlatform;

    /** Görüntü oranı */
    String aspectRatio;

    /** Toplam tahmini süre (saniye) */
    Integer estimatedDurationSeconds;

    /** Sıralı scene listesi */
    List<StoryboardSceneOutput> scenes;

    /** Genel görsel stil kılavuzu */
    String visualStyleGuide;

    /** Narration stil notu */
    String narrationStyle;

    /** AI uyarıları */
    List<String> warnings;

    /** AI kalite değerlendirmesi */
    String qualityNotes;

    /** Safe provider metadata: provider tipi */
    String providerType;

    /** Safe provider metadata: model adı */
    String modelName;

    /** Hata kodu (başarısızsa) */
    String errorCode;

    /** Sanitized hata mesajı (başarısızsa) */
    String errorMessage;

    /** Retry yapılabilir mi */
    Boolean retryable;

    /** Üretim zamanı */
    LocalDateTime generatedAt;
}
