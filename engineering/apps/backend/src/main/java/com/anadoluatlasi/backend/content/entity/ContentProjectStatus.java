package com.anadoluatlasi.backend.content.entity;

/**
 * İçerik projesinin yaşam döngüsündeki durumunu belirten enum.
 *
 * Ana akış:
 * CREATED → AI_REQUESTED → AI_PROCESSING → DRAFT_READY → IN_REVIEW →
 *   → APPROVED → READY_FOR_PUBLISH
 *   → REVISION_REQUESTED → (yeni AI_REQUESTED)
 *   → REJECTED
 *
 * Hata durumları: FAILED, CANCELLED
 */
public enum ContentProjectStatus {

    /** Proje oluşturuldu, henüz AI çağrısı yapılmadı */
    CREATED,

    /** AI içerik üretimi talep edildi */
    AI_REQUESTED,

    /** AI içerik üretimi devam ediyor */
    AI_PROCESSING,

    /** AI draft hazır, insan review bekliyor */
    DRAFT_READY,

    /** İnsan review sürecinde */
    IN_REVIEW,

    /** Revizyon talep edildi, yeni AI üretimi gerekiyor */
    REVISION_REQUESTED,

    /** İnsan tarafından onaylandı */
    APPROVED,

    /** İnsan tarafından reddedildi */
    REJECTED,

    /** Yayına hazır */
    READY_FOR_PUBLISH,

    /** AI üretimi başarısız oldu */
    FAILED,

    /** Proje iptal edildi */
    CANCELLED
}
