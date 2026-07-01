package com.anadoluatlasi.backend.application.scenario;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-002: Senaryo review operasyonlarının sonuç nesnesi.
 * Edit, approve, reject işlemlerinin ortak dönüş tipi.
 * FP-001 pattern'ı ile tutarlıdır.
 */
@Value
@Builder
public class ScenarioReviewResult {

    /** İşlem başarılı mı */
    boolean success;

    /** ScenarioDraft ID */
    UUID scenarioDraftId;

    /** Content Project ID */
    UUID contentProjectId;

    /** Güncel status */
    String status;

    /** Senaryo başlığı */
    String scenarioTitle;

    /** Sonuç mesajı */
    String message;
}
