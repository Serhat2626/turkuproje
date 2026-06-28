package com.anadoluatlasi.backend.content.entity;

/**
 * İçerik üretim amacını belirten enum.
 *
 * Bir Knowledge Item'dan hangi amaçla içerik üretileceğini tanımlar.
 */
public enum ContentPurpose {

    /** Bilgilendirici metin */
    INFORMATIONAL_TEXT,

    /** Çocuklara uygun metin */
    CHILD_FRIENDLY_TEXT,

    /** Turistik rehber metni */
    TOURISTIC_GUIDE_TEXT,

    /** Sosyal medya paylaşımı */
    SOCIAL_MEDIA_POST,

    /** Mobil uygulama kartı */
    MOBILE_APP_CARD,

    /** Sesli rehber senaryosu */
    AUDIO_GUIDE_SCRIPT
}
