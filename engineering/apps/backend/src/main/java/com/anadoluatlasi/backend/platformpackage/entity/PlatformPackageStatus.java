package com.anadoluatlasi.backend.platformpackage.entity;

/**
 * FP-004: Platform paketinin yaşam döngüsü durumunu belirten enum.
 *
 * <p>FP-002/FP-003 ile tutarlı governance kuralları:</p>
 * <ul>
 *   <li>AI generation yalnızca {@code AI_GENERATED} veya {@code FAILED} üretebilir.</li>
 *   <li>AI hiçbir zaman {@code APPROVED} üretemez.</li>
 *   <li>{@code APPROVED} için explicit admin eylemi gerekir.</li>
 *   <li>{@code REJECTED} için explicit admin eylemi gerekir.</li>
 *   <li>Generic status update davranışı yoktur.</li>
 *   <li>{@code READY_FOR_PUBLISH} statüsü yoktur.</li>
 * </ul>
 */
public enum PlatformPackageStatus {

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
