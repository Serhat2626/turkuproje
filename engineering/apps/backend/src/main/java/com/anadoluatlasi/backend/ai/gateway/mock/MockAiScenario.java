package com.anadoluatlasi.backend.ai.gateway.mock;

/**
 * FP-001: Mock AI gateway'in hangi senaryoyu simüle edeceğini belirleyen enum.
 * Test ve geliştirme ortamında deterministic davranış sağlar.
 */
public enum MockAiScenario {

    /** Başarılı AI yanıtı — deterministic içerik üretir */
    SUCCESS,

    /** AI provider timeout hatası — retryable */
    TIMEOUT,

    /** Boş AI yanıtı — retryable */
    EMPTY_RESPONSE,

    /** Çok kısa AI yanıtı (100 karakterden az) — retryable */
    TOO_SHORT_RESPONSE,

    /** Geçersiz format AI yanıtı — retryable */
    INVALID_FORMAT,

    /** Güvenli olmayan içerik AI yanıtı — non-retryable */
    UNSAFE_CONTENT
}
