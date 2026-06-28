package com.anadoluatlasi.backend.content.entity;

/**
 * İçeriğin hedef kitlesini belirten enum.
 *
 * AI'ın ürettiği içeriğin dil, ton ve detay seviyesini etkiler.
 */
public enum TargetAudience {

    /** Genel kullanıcı */
    GENERAL_USER,

    /** Çocuk */
    CHILD,

    /** Turist */
    TOURIST,

    /** Öğrenci */
    STUDENT,

    /** Araştırmacı */
    RESEARCHER,

    /** Mobil uygulama kullanıcısı */
    MOBILE_APP_USER
}
