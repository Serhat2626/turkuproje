package com.anadoluatlasi.backend.application.scenario;

import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import lombok.Builder;
import lombok.Value;

/**
 * FP-002: Senaryo beat düzenleme komut nesnesi.
 * Service katmanı DTO'sudur — REST API'de henüz kullanılmaz.
 */
@Value
@Builder
public class ScenarioBeatEditCommand {

    /** Beat tipi (hangi beat düzenleniyor) */
    ScenarioBeatType type;

    /** Beat etiketi/başlığı */
    String label;

    /** Beat içerik metni */
    String body;

    /** Sıralama numarası */
    Integer orderIndex;

    /** Tahmini süre (saniye) */
    Integer estimatedDurationSeconds;
}
