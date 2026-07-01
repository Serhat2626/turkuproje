package com.anadoluatlasi.backend.application.scenario;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-002: Senaryo üretim operasyonlarının sonuç nesnesi.
 * Başarı ve hata durumlarını tek tip olarak taşır.
 * FP-001 AiGenerationResult pattern'ı ile tutarlıdır.
 */
@Value
@Builder
public class ScenarioGenerationResult {

    /** İşlem başarılı mı */
    boolean success;

    /** Content Project ID */
    UUID contentProjectId;

    /** Oluşturulan ScenarioDraft ID */
    UUID scenarioDraftId;

    /** Senaryo version numarası */
    Integer version;

    /** Senaryo güncel status'ü */
    String status;

    /** Senaryo başlığı (başarılıysa) */
    String scenarioTitle;

    /** Sonuç mesajı */
    String message;

    /** Hata kodu (başarısızsa) */
    String errorCode;
}
