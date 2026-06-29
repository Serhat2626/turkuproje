package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-001: AI Content Agent'a gönderilen istek contract'ı.
 * Product context'ini AI Platform boundary'sine taşır.
 * Provider bağımsızdır — hiçbir AI SDK'ya bağımlı değildir.
 */
@Value
@Builder
public class ContentAgentRequest {

    /** AI isteği için benzersiz takip referansı */
    UUID requestId;

    /** Content Project referansı */
    UUID projectId;

    /** İçerik üretilecek kültürel bilgi */
    ContentAgentKnowledgeItemPayload knowledgeItem;

    /** İçerik üretim amacı (ContentPurpose enum adı) */
    String contentPurpose;

    /** Hedef kitle (TargetAudience enum adı) */
    String targetAudience;

    /** Anlatım tonu (ContentTone enum adı) */
    String tone;

    /** Üretilecek içerik dili */
    String language;

    /** Beklenen çıktı formatına dair ipucu */
    String formatHint;

    /** Yaklaşık uzunluk beklentisi */
    String lengthHint;

    /** Revizyon sürecinde insan notu */
    HumanFeedbackPayload humanFeedback;

    /** Revizyon için önceki draft */
    PreviousDraftPayload previousDraft;

    /** Retry bilgisi */
    ContentAgentRetryMetadata retryMetadata;

    /** Kültürel hassasiyet ve güvenlik notları */
    String safetyInstructions;
}
