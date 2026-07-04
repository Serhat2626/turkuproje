package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-007: Image generation normalized error kodları.
 *
 * <p>Provider hatalarını normalize eden güvenli error code'ları.
 * Raw provider error dump admin-facing yüzeylerde gösterilmez;
 * bu enum ile güvenli hata kategorileri temsil edilir.</p>
 */
public enum ImageGenerationErrorCode {

    /** Provider erişilemez durumda */
    PROVIDER_UNAVAILABLE,

    /** Provider yanıt süresi aşıldı */
    PROVIDER_TIMEOUT,

    /** Provider request'i reddetti (format/kota vb.) */
    PROVIDER_REJECTED_REQUEST,

    /** Provider safety policy'si engelledi */
    PROVIDER_SAFETY_BLOCKED,

    /** Geçersiz generation input (eksik prompt, geçersiz boyut vb.) */
    INVALID_GENERATION_INPUT,

    /** Üretilen görsel storage'a aktarılamadı */
    STORAGE_HANDOFF_FAILED,

    /** Sınıflandırılamayan hata */
    UNKNOWN_GENERATION_FAILURE
}
