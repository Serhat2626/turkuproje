package com.anadoluatlasi.backend.media.entity;

/**
 * Medya varlığının hangi yöntemle üretildiğini tanımlar.
 */
public enum GeneratorType {

    /** AI tabanlı görsel üretimi */
    IMAGE_AI,

    /** AI tabanlı video üretimi */
    VIDEO_AI,

    /** Text-to-Speech seslendirme */
    TTS,

    /** Kullanıcı tarafından manuel yükleme */
    MANUAL_UPLOAD
}
