package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-006: Sahne bazlı görsel plan çıktısı.
 *
 * <p>AI'ın her storyboard sahnesi için ürettiği image-generation-ready prompt planı.</p>
 *
 * <p>{@code englishImagePrompt} ve {@code turkishVisualContext} alanları ürün çıktısıdır —
 * admin tarafından görüntülenir, düzenlenir ve onaylanır.</p>
 *
 * <p>Image URL, dosya referansı veya MediaAsset/GeneratedAsset ID'si içermez.</p>
 */
@Value
@Builder
public class VisualAssetSceneOutput {

    // ── Source Reference ─────────────────────────────────────────

    /** Kaynak storyboard sahne ID (input'tan korunur) */
    UUID sourceStoryboardSceneId;

    /** Sahne numarası (input'tan korunur) */
    Integer sceneNumber;

    /** Beat tipi (input'tan korunur) */
    String sourceBeatType;

    /** Sahne başlığı */
    String sceneTitle;

    // ── Image Prompt (product output) ────────────────────────────

    /** İngilizce image-generation-ready prompt — ürün çıktısı */
    String englishImagePrompt;

    /** Türkçe görsel bağlam açıklaması — ürün çıktısı */
    String turkishVisualContext;

    /** Negatif prompt (ne olmaması gerektiği) */
    String negativePrompt;

    // ── Style & Composition ──────────────────────────────────────

    /** Görsel stil */
    String styleDirective;

    /** Aspect ratio */
    String aspectRatio;

    /** Çerçeveleme, perspektif, odak noktası */
    String compositionNotes;

    /** Kamera açısı/çerçeveleme */
    String cameraFraming;

    /** Aydınlatma yönlendirmesi */
    String lightingNotes;

    // ── Mood & Color ─────────────────────────────────────────────

    /** Renk paleti */
    String colorPalette;

    /** Atmosfer/duygu */
    String mood;

    // ── Content ──────────────────────────────────────────────────

    /** Mekan/ortam tanımı */
    String location;

    /** Karakter/özne tanımı ve görsel notları */
    String characters;

    /** Kültürel/tarihsel doğruluk detayları */
    String culturalDetails;

    // ── Overlay & Tool Notes ─────────────────────────────────────

    /** Ekran metni/overlay yönlendirmesi */
    String textOverlayGuidance;

    /** Image gen tool notları */
    String imageGenerationToolNotes;

    /** İçerik güvenliği notları */
    String safetyNotes;
}
