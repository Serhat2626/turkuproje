package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * FP-003: AI Storyboard Agent'a gönderilen istek contract'ı.
 * Approved ScenarioDraft context'ini AI Platform boundary'sine taşır.
 * Provider bağımsızdır — hiçbir AI SDK'ya bağımlı değildir.
 *
 * <p>API key, raw prompt veya provider-specific alan içermez.</p>
 */
@Value
@Builder
public class StoryboardAgentRequest {

    /** AI isteği için benzersiz takip referansı */
    UUID requestId;

    /** Content Project referansı */
    UUID contentProjectId;

    /** Kaynak approved ContentDraft referansı */
    UUID sourceDraftId;

    /** Kaynak approved ScenarioDraft referansı */
    UUID sourceScenarioId;

    /** Kaynak senaryo versiyonu */
    Integer sourceScenarioVersion;

    /** Senaryo başlığı (prompt context) */
    String scenarioTitle;

    /** Senaryo özeti */
    String scenarioSummary;

    /** Senaryo beat listesi (6 adet) */
    List<StoryboardSourceBeatInput> scenarioBeats;

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
    Integer estimatedDurationSeconds;

    /** İstenen scene sayısı (null = beat sayısına göre) */
    Integer desiredSceneCount;

    /** Hedef platform (YouTube, Instagram Reels, vb.) */
    String targetPlatform;

    /** Görüntü oranı (16:9, 9:16, 1:1) */
    String aspectRatio;

    /** İçerik dili */
    String language;

    /** Kültürel hassasiyet notları */
    String culturalContext;

    /** Talep eden admin ID */
    String requestedByAdminId;

    /** Üretim nedeni */
    String generationReason;

    /** Regeneration'da mevcut version */
    Integer existingStoryboardVersion;

    /** Admin'in regeneration yönergesi */
    String regenerationInstruction;
}
