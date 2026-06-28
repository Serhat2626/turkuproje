package com.anadoluatlasi.backend.ai.entity;

/**
 * Prompt şablonlarının yaşam döngüsü durumlarını tanımlar.
 */
public enum PromptTemplateStatus {

    /** Taslak aşamasında, henüz kullanıma hazır değil */
    DRAFT,

    /** Aktif ve kullanılabilir durumda */
    ACTIVE,

    /** Arşivlenmiş, artık kullanılmıyor */
    ARCHIVED
}
