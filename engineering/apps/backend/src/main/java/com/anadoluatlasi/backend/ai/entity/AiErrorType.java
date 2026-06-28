package com.anadoluatlasi.backend.ai.entity;

/**
 * AI üretim hata tiplerini tanımlar.
 *
 * Hata tipleri üç kategoride gruplandırılır:
 * - AI provider teknik hataları (timeout, rate limit, auth, vb.)
 * - AI yanıt kalite hataları (boş yanıt, format hatası, dil uyumsuzluğu, vb.)
 * - İş kuralı hataları (proje iptal, bilgi öğesi arşivlenmiş, insan reddi)
 */
public enum AiErrorType {

    // ── AI Provider Teknik Hataları ──────────────────────

    /** AI provider zaman aşımına uğradı */
    AI_PROVIDER_TIMEOUT,

    /** AI provider erişilemez durumda */
    AI_PROVIDER_UNAVAILABLE,

    /** AI provider rate limit aşıldı */
    AI_PROVIDER_RATE_LIMIT,

    /** AI provider kimlik doğrulama hatası */
    AI_PROVIDER_AUTH_ERROR,

    /** AI provider yapılandırma hatası */
    AI_PROVIDER_CONFIG_ERROR,

    /** AI provider iç hatası */
    AI_PROVIDER_INTERNAL_ERROR,

    // ── AI Yanıt Kalite Hataları ────────────────────────

    /** AI yanıtı boş döndü */
    EMPTY_AI_RESPONSE,

    /** AI yanıtı beklenen formatta değil */
    INVALID_AI_RESPONSE_FORMAT,

    /** AI yanıtı çok kısa */
    AI_RESPONSE_TOO_SHORT,

    /** AI yanıtı dil uyumsuzluğu içeriyor */
    AI_RESPONSE_LANGUAGE_MISMATCH,

    /** AI yanıtı güvenli olmayan içerik barındırıyor */
    AI_RESPONSE_UNSAFE_CONTENT,

    /** AI yanıtı talep edilen amaçla uyuşmuyor */
    AI_RESPONSE_PURPOSE_MISMATCH,

    // ── İş Kuralı Hataları ──────────────────────────────

    /** Proje iptal edildi */
    PROJECT_CANCELLED,

    /** Bilgi öğesi arşivlenmiş, üretim yapılamaz */
    KNOWLEDGE_ITEM_ARCHIVED,

    /** İnsan editör tarafından reddedildi */
    HUMAN_REJECTED,

    /** Sınıflandırılamayan AI hatası */
    UNKNOWN_AI_ERROR
}
