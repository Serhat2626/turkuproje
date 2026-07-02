package com.anadoluatlasi.backend.application.storyboard;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * FP-003: Storyboard taslağı düzenleme komut nesnesi.
 * Service katmanı DTO'sudur — REST API'de henüz kullanılmaz.
 *
 * <p>Media generation, platform publishing, FP-004 alanları içermez.</p>
 */
@Value
@Builder
public class StoryboardDraftEditCommand {

    /** Storyboard başlığı */
    String title;

    /** Kısa özet */
    String summary;

    /** Hedef platform (YouTube, Instagram Reels, vb.) */
    String targetPlatform;

    /** Görüntü oranı (16:9, 9:16, 1:1) */
    String aspectRatio;

    /** Toplam tahmini süre (saniye) */
    Integer estimatedDurationSeconds;

    /** Genel görsel stil kılavuzu */
    String visualStyleGuide;

    /** Narration stil notu */
    String narrationStyle;

    /** Admin review notları */
    String reviewNotes;

    /** Scene düzelenmeleri (opsiyonel — null ise scene'lere dokunulmaz) */
    List<StoryboardSceneEditCommand> scenes;
}
