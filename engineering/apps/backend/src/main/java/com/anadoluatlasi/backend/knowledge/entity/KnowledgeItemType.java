package com.anadoluatlasi.backend.knowledge.entity;

/**
 * Bilgi bankası öğe türlerini tanımlar.
 * Her bilgi öğesi bu türlerden birine ait olmalıdır.
 */
public enum KnowledgeItemType {

    /** Türkü */
    FOLK_SONG,

    /** Sanatçı */
    ARTIST,

    /** Bölge / Yöre */
    REGION,

    /** Gelenek / Görenek */
    TRADITION,

    /** Tarihî olay */
    HISTORICAL_EVENT,

    /** Efsane / Destan */
    LEGEND
}
