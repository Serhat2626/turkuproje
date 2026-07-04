package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-006: Storyboard sahnesi için görsel planlama input'u.
 *
 * <p>Kaynak StoryboardScene'den map'lenen, AI agent'a gönderilen sahne verisi.</p>
 */
@Value
@Builder
public class VisualAssetSceneInput {

    /** Kaynak storyboard sahne ID */
    UUID storyboardSceneId;

    /** Sahne numarası (1-based) */
    Integer sceneNumber;

    /** Beat tipi (HOOK, INTRODUCTION, etc.) */
    String sourceBeatType;

    /** Beat sırası */
    Integer sourceBeatOrder;

    /** Sahne başlığı */
    String sceneTitle;

    /** Sahnenin görsel tanımı — birincil AI input */
    String visualDescription;

    /** Seslendirme metni */
    String narrationText;

    /** Ekran metni */
    String onScreenText;

    /** Kamera yönlendirmesi */
    String cameraDirection;

    /** Geçiş notu */
    String transitionNote;

    /** Atmosfer/duygu */
    String mood;

    /** Sahne mekanı */
    String location;

    /** Karakterler/özneler */
    String characters;

    /** Sahne süresi (saniye) */
    Integer estimatedDurationSeconds;

    /** Medya üretim notları */
    String mediaGenerationNotes;
}
