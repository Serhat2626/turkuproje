package com.anadoluatlasi.backend.storyboard.entity;

/**
 * FP-003: Storyboard taslağının yaşam döngüsü durumunu belirten enum.
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>AI generation yalnızca AI_GENERATED veya FAILED üretebilir.</li>
 *   <li>AI hiçbir zaman APPROVED üretemez.</li>
 *   <li>APPROVED için explicit admin eylemi gerekir.</li>
 *   <li>REJECTED için explicit admin eylemi gerekir.</li>
 *   <li>Düzenleme ayrı bir status gerektirmez (metadata ile izlenir).</li>
 *   <li>Generic status update davranışı yoktur.</li>
 * </ul>
 */
public enum StoryboardDraftStatus {

    /** AI tarafından üretildi, henüz incelenmedi */
    AI_GENERATED,

    /** Admin tarafından düzenlendi, tekrar inceleme gerekiyor */
    NEEDS_REVIEW,

    /** Admin tarafından onaylandı (explicit action) */
    APPROVED,

    /** Admin tarafından reddedildi (explicit action) */
    REJECTED,

    /** AI üretimi başarısız */
    FAILED
}
