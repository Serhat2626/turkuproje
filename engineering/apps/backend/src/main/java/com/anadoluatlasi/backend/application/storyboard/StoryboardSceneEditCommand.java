package com.anadoluatlasi.backend.application.storyboard;

import lombok.Builder;
import lombok.Value;

/**
 * FP-003: Storyboard scene düzenleme komut nesnesi.
 * Service katmanı DTO'sudur — REST API'de henüz kullanılmaz.
 *
 * <p>Media generation alanları içermez.
 * {@code mediaGenerationNotes} yalnızca gelecek handoff metadata'dır.</p>
 */
@Value
@Builder
public class StoryboardSceneEditCommand {

    /** Sahne sıra numarası (hangi scene düzenleniyor) */
    Integer sceneNumber;

    /** Sahne başlığı */
    String sceneTitle;

    /** Sahne görsel açıklaması */
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
    Integer estimatedDurationSeconds;

    /** Gelecek media üretimi için notlar — yalnızca handoff metadata */
    String mediaGenerationNotes;
}
