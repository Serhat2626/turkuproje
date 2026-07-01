package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-002: AI Scenario Agent'ın normalize edilmiş yanıt contract'ı.
 * Provider raw response değildir — AI Platform tarafından normalize edilmiştir.
 *
 * <p>success=true ise senaryo alanları dolu, success=false ise hata alanları dolu.</p>
 *
 * <p>Raw provider payload, raw prompt ve API key/secret asla dahil edilmez.</p>
 */
@Value
@Builder
public class ScenarioAgentResponse {

    /** İşlem başarılı mı */
    boolean success;

    /** Senaryo başlığı */
    String scenarioTitle;

    /** Kısa özet */
    String summary;

    /** Hedef kitle */
    String targetAudience;

    /** Kültürel/öğretici amaç */
    String culturalObjective;

    /** Anlatım tonu */
    String tone;

    /** Tahmini video süresi (saniye) */
    Integer estimatedDurationSeconds;

    /** 6 beat sonucu */
    List<ScenarioBeatResult> beats;

    /** Karakterler */
    String characters;

    /** Mekan/lokasyon */
    String setting;

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
