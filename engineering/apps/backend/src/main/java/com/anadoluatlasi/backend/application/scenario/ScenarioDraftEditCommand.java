package com.anadoluatlasi.backend.application.scenario;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * FP-002: Senaryo taslağı düzenleme komut nesnesi.
 * Service katmanı DTO'sudur — REST API'de henüz kullanılmaz.
 */
@Value
@Builder
public class ScenarioDraftEditCommand {

    /** Senaryo başlığı */
    String title;

    /** Kısa özet */
    String summary;

    /** Hedef kitle */
    String targetAudience;

    /** Kültürel/öğretici amaç */
    String culturalObjective;

    /** Anlatım tonu */
    String tone;

    /** Tahmini video süresi (saniye) */
    Integer estimatedDurationSeconds;

    /** Mekan/lokasyon */
    String setting;

    /** Karakterler */
    String characters;

    /** Admin review notları */
    String reviewNotes;

    /** Beat düzenlemeleri (opsiyonel — null ise beat'lere dokunulmaz) */
    List<ScenarioBeatEditCommand> beats;
}
