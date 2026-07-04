package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * FP-006: Visual Asset Plan üretim agent'ına gönderilen istek contract'ı.
 *
 * <p>Bu contract dahili AI/application sınırında kullanılır.
 * REST katmanına expose edilmez.</p>
 *
 * <p>Raw prompt, provider payload ve API key/secret asla dahil edilmez.</p>
 */
@Value
@Builder
public class VisualAssetPlanAgentRequest {

    /** Benzersiz istek kimliği */
    UUID requestId;

    /** Kaynak storyboard ID */
    UUID storyboardDraftId;

    /** İçerik projesi ID */
    UUID contentProjectId;

    /** Hikâye/proje başlığı */
    String title;

    /** Hikâye özeti */
    String summary;

    /** Hedef platform (YouTube, IG, etc.) */
    String targetPlatform;

    /** Varsayılan aspect ratio */
    String aspectRatio;

    /** Tahmini video süresi (saniye) */
    Integer estimatedDurationSeconds;

    /** Görsel stil rehberi */
    String visualStyleGuide;

    /** Anlatım stili */
    String narrationStyle;

    /** Sahne listesi */
    List<VisualAssetSceneInput> scenes;

    /** Thumbnail üretim context'i */
    VisualAssetThumbnailInput thumbnailContext;

    /** Dil politikası */
    String languagePolicy;

    /** Kültürel bağlam politikası */
    String culturalContextPolicy;

    /** İçerik güvenliği politikası */
    String safetyPolicy;

    /** İsteği yapan admin */
    String requestedBy;
}
