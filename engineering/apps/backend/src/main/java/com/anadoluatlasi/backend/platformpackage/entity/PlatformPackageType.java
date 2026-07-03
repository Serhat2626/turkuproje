package com.anadoluatlasi.backend.platformpackage.entity;

/**
 * FP-004: Platform paketi tipini belirten enum.
 *
 * <p>Desteklenen ilk platform tipleri:</p>
 * <ul>
 *   <li>{@code YOUTUBE_LONG_FORM} — YouTube ana video formatı (16:9)</li>
 *   <li>{@code YOUTUBE_SHORTS} — YouTube kısa form dikey video (9:16, ≤60s)</li>
 *   <li>{@code INSTAGRAM_REELS} — Instagram Reels dikey video (9:16, ≤90s)</li>
 *   <li>{@code FACEBOOK_REELS} — Facebook Reels dikey video (9:16, ≤90s)</li>
 * </ul>
 */
public enum PlatformPackageType {

    /** YouTube ana video formatı (16:9, 5-15 dk) */
    YOUTUBE_LONG_FORM,

    /** YouTube Shorts dikey video (9:16, ≤60 saniye) */
    YOUTUBE_SHORTS,

    /** Instagram Reels dikey video (9:16, ≤90 saniye) */
    INSTAGRAM_REELS,

    /** Facebook Reels dikey video (9:16, ≤90 saniye) */
    FACEBOOK_REELS
}
