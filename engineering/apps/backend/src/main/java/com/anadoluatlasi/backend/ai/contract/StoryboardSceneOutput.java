package com.anadoluatlasi.backend.ai.contract;

import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import lombok.Builder;
import lombok.Value;

/**
 * FP-003: AI tarafından üretilen tek bir storyboard scene'inin sonuç contract'ı.
 * Provider-agnostic'tir — tüm provider'lar bu yapıya normalize eder.
 *
 * <p>{@code mediaGenerationNotes} yalnızca gelecek media workflow handoff metadata'sıdır.
 * FP-003 gerçek image/video/audio üretmez.</p>
 */
@Value
@Builder
public class StoryboardSceneOutput {

    /** Sahne sıra numarası (1-based) */
    int sceneNumber;

    /** Kaynak beat tipi */
    ScenarioBeatType sourceBeatType;

    /** Kaynak beat sıra numarası */
    int sourceBeatOrder;

    /** Sahne başlığı */
    String sceneTitle;

    /** Sahne görsel açıklaması — ne gösterilecek */
    String visualDescription;

    /** Voiceover / narration metni */
    String narrationText;

    /** Ekranda gösterilecek metin */
    String onScreenText;

    /** Kamera hareketi / açısı */
    String cameraDirection;

    /** Sahne geçiş notu */
    String transitionNote;

    /** Sahne atmosferi / duygu */
    String mood;

    /** Sahne mekanı */
    String location;

    /** Sahnedeki karakterler/özneler */
    String characters;

    /** Sahne tahmini süresi (saniye) */
    int estimatedDurationSeconds;

    /** Gelecek media üretimi için notlar — yalnızca handoff metadata */
    String mediaGenerationNotes;
}
