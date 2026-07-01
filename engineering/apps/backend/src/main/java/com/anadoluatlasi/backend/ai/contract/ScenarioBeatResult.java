package com.anadoluatlasi.backend.ai.contract;

import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import lombok.Builder;
import lombok.Value;

/**
 * FP-002: AI tarafından üretilen tek bir senaryo beat'inin sonuç contract'ı.
 * Provider-agnostic'tir — tüm provider'lar bu yapiya normalize eder.
 */
@Value
@Builder
public class ScenarioBeatResult {

    /** Beat tipi */
    ScenarioBeatType type;

    /** Beat başlığı/etiketi */
    String title;

    /** Beat içerik metni */
    String body;

    /** Sıralama numarası */
    int order;

    /** Tahmini süre (saniye) */
    Integer estimatedDurationSeconds;
}
