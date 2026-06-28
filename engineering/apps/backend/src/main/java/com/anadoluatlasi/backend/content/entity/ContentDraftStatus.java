package com.anadoluatlasi.backend.content.entity;

/**
 * İçerik taslağının (Content Draft) durumunu belirten enum.
 *
 * AI veya insan tarafından oluşturulan taslakların review sürecindeki durumu.
 */
public enum ContentDraftStatus {

    /** AI tarafından üretildi, henüz incelenmedi */
    GENERATED,

    /** İnsan editör tarafından düzenlendi */
    EDITED,

    /** Revizyon gerekiyor */
    REVISION_NEEDED,

    /** İnsan tarafından onaylandı */
    APPROVED,

    /** İnsan tarafından reddedildi */
    REJECTED
}
