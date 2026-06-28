package com.anadoluatlasi.backend.knowledge.entity;

/**
 * Bilgi bankası öğelerinin yaşam döngüsü durumlarını tanımlar.
 */
public enum KnowledgeItemStatus {

    /** Taslak olarak oluşturuldu */
    DRAFT,

    /** Araştırma aşamasında */
    RESEARCHING,

    /** İnceleme bekliyor */
    REVIEW_REQUIRED,

    /** Doğrulanmış ve kullanıma hazır */
    VERIFIED,

    /** Arşivlenmiş */
    ARCHIVED
}
