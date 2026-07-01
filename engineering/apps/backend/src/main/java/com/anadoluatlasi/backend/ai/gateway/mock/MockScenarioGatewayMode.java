package com.anadoluatlasi.backend.ai.gateway.mock;

/**
 * FP-002: Mock Scenario Gateway test modları.
 * MockAiScenarioAgentGateway'in davranışını kontrol eder.
 */
public enum MockScenarioGatewayMode {

    /** Deterministic başarılı 6-beat senaryo */
    SUCCESS,

    /** Provider timeout simülasyonu (retryable) */
    TIMEOUT,

    /** Boş yanıt simülasyonu (retryable) */
    EMPTY_RESPONSE,

    /** Geçersiz format simülasyonu (retryable) */
    INVALID_FORMAT,

    /** Güvenli olmayan içerik simülasyonu (non-retryable) */
    UNSAFE_CONTENT
}
