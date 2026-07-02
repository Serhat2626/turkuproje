package com.anadoluatlasi.backend.ai.gateway.mock;

/**
 * FP-003: Mock Storyboard Gateway test modları.
 * MockAiStoryboardAgentGateway'in davranışını kontrol eder.
 */
public enum MockStoryboardGatewayMode {

    /** Deterministic başarılı storyboard scene'leri */
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
