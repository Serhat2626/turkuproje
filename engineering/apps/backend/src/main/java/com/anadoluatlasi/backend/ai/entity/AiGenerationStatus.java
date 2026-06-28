package com.anadoluatlasi.backend.ai.entity;

/**
 * AI üretim işlemlerinin durum bilgisini tanımlar.
 */
public enum AiGenerationStatus {

    /** İstek gönderildi, yanıt bekleniyor */
    REQUESTED,

    /** Üretim başarıyla tamamlandı */
    SUCCESS,

    /** Üretim başarısız oldu */
    FAILED,

    /** Üretim iptal edildi */
    CANCELLED
}
