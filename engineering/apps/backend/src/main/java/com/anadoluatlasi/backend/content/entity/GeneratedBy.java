package com.anadoluatlasi.backend.content.entity;

/**
 * İçerik taslağını kimin oluşturduğunu belirten enum.
 *
 * Content Draft'ın kaynağını izlemek için kullanılır.
 */
public enum GeneratedBy {

    /** AI content agent tarafından üretildi */
    AI_CONTENT_AGENT,

    /** İnsan editör tarafından oluşturuldu */
    HUMAN_EDITOR,

    /** Sistem tarafından otomatik oluşturuldu */
    SYSTEM
}
