package com.anadoluatlasi.backend.media.entity;

/**
 * Medya üretim işlemlerinin durum bilgisini tanımlar.
 */
public enum GenerationStatus {

    /** Üretim isteği gönderildi */
    REQUESTED,

    /** Üretim devam ediyor */
    GENERATING,

    /** Üretim başarıyla tamamlandı */
    SUCCESS,

    /** Üretim başarısız oldu */
    FAILED,

    /** Üretim iptal edildi */
    CANCELLED
}
