package com.anadoluatlasi.backend.ai.contract;

import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import lombok.Builder;
import lombok.Value;

/**
 * FP-003: Storyboard üretimi için kaynak senaryo beat girdisi.
 * Approved ScenarioDraft'ın beat'lerini AI Platform boundary'sine taşır.
 *
 * <p>Raw prompt, API key veya provider-specific alan içermez.</p>
 */
@Value
@Builder
public class StoryboardSourceBeatInput {

    /** Beat tipi */
    ScenarioBeatType beatType;

    /** Beat başlığı/etiketi */
    String beatTitle;

    /** Beat içerik metni */
    String beatBody;

    /** Sıralama numarası */
    int orderIndex;

    /** Tahmini süre (saniye) */
    Integer estimatedDurationSeconds;
}
