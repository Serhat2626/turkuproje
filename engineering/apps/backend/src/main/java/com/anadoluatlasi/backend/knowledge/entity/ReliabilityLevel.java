package com.anadoluatlasi.backend.knowledge.entity;

/**
 * Kaynak güvenilirlik seviyelerini tanımlar.
 * Araştırma kaynaklarının ne kadar güvenilir olduğunu belirtir.
 */
public enum ReliabilityLevel {

    /** Düşük güvenilirlik */
    LOW,

    /** Orta güvenilirlik */
    MEDIUM,

    /** Yüksek güvenilirlik */
    HIGH,

    /** Birincil / Özgün kaynak */
    PRIMARY
}
