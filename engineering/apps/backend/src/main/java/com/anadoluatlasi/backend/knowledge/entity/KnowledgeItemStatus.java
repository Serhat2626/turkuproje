package com.anadoluatlasi.backend.knowledge.entity;

/**
 * Bilgi bankası öğelerinin yaşam döngüsü durumlarını tanımlar.
 *
 * Geçiş sırası: DRAFT → READY → ARCHIVED
 */
public enum KnowledgeItemStatus {

    /** Taslak olarak oluşturuldu, düzenleme aşamasında */
    DRAFT,

    /** İçerik üretimine hazır */
    READY,

    /** Arşivlenmiş, artık aktif değil */
    ARCHIVED
}
