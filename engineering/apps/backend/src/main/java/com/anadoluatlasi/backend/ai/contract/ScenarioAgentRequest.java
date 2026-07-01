package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-002: AI Scenario Agent'a gönderilen istek contract'ı.
 * Product context'ini AI Platform boundary'sine taşır.
 * Provider bağımsızdır — hiçbir AI SDK'ya bağımlı değildir.
 *
 * <p>API key, raw prompt veya provider-specific alan içermez.</p>
 */
@Value
@Builder
public class ScenarioAgentRequest {

    /** AI isteği için benzersiz takip referansı */
    UUID requestId;

    /** Content Project referansı */
    UUID contentProjectId;

    /** Kaynak approved draft referansı */
    UUID sourceDraftId;

    /** Source draft başlığı (prompt context) */
    String sourceDraftTitle;

    /** Source draft gövde metni (ana içerik) */
    String sourceDraftBody;

    /** Source draft özeti */
    String sourceDraftSummary;

    /** İlgili bilgi öğesi başlığı */
    String knowledgeItemTitle;

    /** Bilgi öğesi açıklaması */
    String knowledgeItemDescription;

    /** Şehir/bölge bilgisi */
    String knowledgeItemCity;

    /** Hedef kitle */
    String targetAudience;

    /** Anlatım tonu */
    String tone;

    /** Hedef video süresi (saniye) */
    Integer desiredDurationSeconds;

    /** İçerik dili */
    String language;

    /** Kültürel hassasiyet notları */
    String culturalContext;

    /** Talep eden admin ID */
    String requestedByAdminId;

    /** Üretim nedeni */
    String generationReason;

    /** Regeneration'da mevcut version */
    Integer existingScenarioVersion;

    /** Admin'in regeneration yönergesi */
    String regenerationInstruction;
}
