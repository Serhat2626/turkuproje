package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-006: Thumbnail görsel plan çıktısı.
 *
 * <p>AI'ın ürettiği thumbnail image-generation-ready prompt planı.</p>
 *
 * <p>{@code thumbnailPrompt} alanı ürün çıktısıdır.</p>
 *
 * <p>Thumbnail görsel dosyası, image URL veya MediaAsset referansı içermez.</p>
 */
@Value
@Builder
public class VisualAssetThumbnailOutput {

    /** Thumbnail image-generation-ready prompt — ürün çıktısı */
    String thumbnailPrompt;

    /** Negatif prompt */
    String negativePrompt;

    /** Metin yerleşimi rehberliği */
    String textOverlayGuidance;

    /** Kompozisyon notları */
    String compositionNotes;

    /** Duygusal çekicilik notu */
    String emotionalHook;

    /** Aspect ratio */
    String aspectRatio;

    /** Stil yönlendirmesi */
    String styleDirective;

    /** Renk paleti */
    String colorPalette;

    /** Referans sahne numarası (opsiyonel) */
    Integer sourceSceneNumber;

    /** Referans sahne ID (opsiyonel) */
    UUID sourceStoryboardSceneId;
}
