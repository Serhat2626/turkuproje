package com.anadoluatlasi.backend.ai.entity;

/**
 * Prompt şablonlarının kategori türlerini tanımlar.
 * Her kategori, içerik üretim pipeline'ındaki farklı bir adıma karşılık gelir.
 */
public enum PromptCategory {

    /** Araştırma promptu */
    RESEARCH,

    /** Senaryo üretim promptu */
    SCRIPT,

    /** Storyboard oluşturma promptu */
    STORYBOARD,

    /** Görsel üretim promptu */
    IMAGE,

    /** Video üretim promptu */
    VIDEO,

    /** SEO metin üretim promptu */
    SEO,

    /** Küçük resim (thumbnail) üretim promptu */
    THUMBNAIL,

    /** Çeviri promptu */
    TRANSLATION
}
