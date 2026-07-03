package com.anadoluatlasi.backend.ai.contract;

import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * FP-004: Platform paketi üretim agent'ına gönderilecek istek contract'ı.
 *
 * <p>Bu contract dahili AI/application sınırında kullanılır.
 * REST katmanına expose edilmez.</p>
 *
 * <p>Raw prompt, API key, secret veya provider payload taşımaz.</p>
 */
@Value
@Builder
public class PlatformPackageAgentRequest {

    /** Tekil istek tanımlayıcısı */
    UUID requestId;

    /** İçerik projesi ID */
    UUID contentProjectId;

    /** Kaynak StoryboardDraft ID */
    UUID sourceStoryboardDraftId;

    /** Kaynak StoryboardDraft version */
    Integer sourceStoryboardVersion;

    /** Hedef platform tipleri */
    List<PlatformPackageType> targetPlatforms;

    /** Kaynak storyboard bilgisi */
    PlatformPackageSourceStoryboardInput sourceStoryboard;

    /** Kaynak sahneler */
    List<PlatformPackageSourceSceneInput> sourceScenes;

    /** Bilgi/konu context'i */
    PlatformPackageKnowledgeContextInput knowledgeContext;

    /** İçerik dili */
    String language;

    /** Ton rehberi (ör. "DRAMATIC") */
    String toneGuidance;

    /** Hedef kitle (ör. "GENERAL") */
    String targetAudience;

    /** Talep eden admin */
    String requestedByAdminId;

    /** Üretim nedeni */
    String generationReason;
}
