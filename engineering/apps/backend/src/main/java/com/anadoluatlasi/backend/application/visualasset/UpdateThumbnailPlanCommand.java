package com.anadoluatlasi.backend.application.visualasset;

import lombok.Builder;
import lombok.Value;

/**
 * FP-006: ThumbnailPlan düzenleme komut nesnesi.
 * Service katmanı DTO'sudur — REST API'de henüz kullanılmaz.
 *
 * <p>Raw prompt, provider payload, API key, secret, image URL,
 * MediaAsset, GeneratedAsset alanları içermez.</p>
 */
@Value
@Builder
public class UpdateThumbnailPlanCommand {

    /** Thumbnail prompt'u (ürün çıktısı) */
    String thumbnailPrompt;

    /** Negatif prompt */
    String negativePrompt;

    /** Metin overlay rehberi */
    String textOverlayGuidance;

    /** Kompozisyon notları */
    String compositionNotes;

    /** Duygusal kanca */
    String emotionalHook;

    /** Aspect ratio */
    String aspectRatio;

    /** Stil yönergesi */
    String styleDirective;

    /** Renk paleti */
    String colorPalette;
}
