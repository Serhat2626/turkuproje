package com.anadoluatlasi.backend.application.visualasset;

import lombok.Builder;
import lombok.Value;

/**
 * FP-006: SceneVisualPlan düzenleme komut nesnesi.
 * Service katmanı DTO'sudur — REST API'de henüz kullanılmaz.
 *
 * <p>Raw prompt, provider payload, API key, secret, image URL,
 * MediaAsset, GeneratedAsset alanları içermez.</p>
 */
@Value
@Builder
public class UpdateSceneVisualPlanCommand {

    /** İngilizce görsel üretim prompt'u (ürün çıktısı) */
    String englishImagePrompt;

    /** Türkçe görsel bağlam açıklaması (ürün çıktısı) */
    String turkishVisualContext;

    /** Negatif prompt */
    String negativePrompt;

    /** Stil yönergesi */
    String styleDirective;

    /** Aspect ratio */
    String aspectRatio;

    /** Kompozisyon notları */
    String compositionNotes;

    /** Kamera çerçeveleme */
    String cameraFraming;

    /** Aydınlatma notları */
    String lightingNotes;

    /** Renk paleti */
    String colorPalette;

    /** Sahne ruh hali */
    String mood;

    /** Mekan */
    String location;

    /** Karakterler */
    String characters;

    /** Kültürel detaylar */
    String culturalDetails;

    /** Metin overlay rehberi */
    String textOverlayGuidance;

    /** Görsel üretim aracı notları */
    String imageGenerationToolNotes;

    /** Güvenlik notları */
    String safetyNotes;
}
