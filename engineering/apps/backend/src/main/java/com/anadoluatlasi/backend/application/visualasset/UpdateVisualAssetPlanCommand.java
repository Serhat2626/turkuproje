package com.anadoluatlasi.backend.application.visualasset;

import lombok.Builder;
import lombok.Value;

/**
 * FP-006: VisualAssetPlan düzenleme komut nesnesi.
 * Service katmanı DTO'sudur — REST API'de henüz kullanılmaz.
 *
 * <p>Raw prompt, provider payload, API key, secret, image URL,
 * MediaAsset, GeneratedAsset alanları içermez.</p>
 */
@Value
@Builder
public class UpdateVisualAssetPlanCommand {

    /** Plan başlığı */
    String title;

    /** Kısa özet */
    String summary;

    /** Genel görsel stil */
    String overallVisualStyle;

    /** Aspect ratio stratejisi */
    String aspectRatioStrategy;

    /** Kültürel doğruluk rehberi */
    String culturalAuthenticityGuidance;

    /** Karakter sürekliliği rehberi */
    String characterContinuityGuidance;

    /** Mekan sürekliliği rehberi */
    String locationContinuityGuidance;

    /** Renk paleti rehberi */
    String colorPaletteGuidance;

    /** Admin review notları */
    String reviewNotes;
}
