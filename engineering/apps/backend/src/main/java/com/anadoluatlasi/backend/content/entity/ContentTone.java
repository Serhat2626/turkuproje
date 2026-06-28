package com.anadoluatlasi.backend.content.entity;

/**
 * İçeriğin yazım tonunu belirten enum.
 *
 * AI prompt'unda kullanılarak üretilen metnin stilini yönlendirir.
 */
public enum ContentTone {

    /** Bilgilendirici, nesnel ton */
    INFORMATIVE,

    /** Sade ve anlaşılır ton */
    SIMPLE,

    /** Hikâye anlatıcı ton */
    STORYTELLING,

    /** Akademik olmayan, gündelik ton */
    NON_ACADEMIC,

    /** Çocuklara uygun, eğlenceli ton */
    CHILD_FRIENDLY,

    /** Turistik, keşfettirici ton */
    TOURISTIC,

    /** Kısa ve net ton */
    SHORT_AND_CLEAR
}
