package com.anadoluatlasi.backend.ai.entity;

/**
 * AI üretim işlemlerinin durum bilgisini tanımlar.
 *
 * Akış: REQUESTED → PROCESSING → COMPLETED / FAILED / CANCELLED
 */
public enum AiGenerationStatus {

    /** İstek gönderildi, işlem bekleniyor */
    REQUESTED,

    /** AI üretim işlemi devam ediyor */
    PROCESSING,

    /** Üretim başarıyla tamamlandı */
    COMPLETED,

    /** Üretim başarısız oldu */
    FAILED,

    /** Üretim iptal edildi */
    CANCELLED
}
