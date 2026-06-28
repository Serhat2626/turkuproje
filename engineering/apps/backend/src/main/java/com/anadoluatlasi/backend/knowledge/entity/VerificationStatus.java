package com.anadoluatlasi.backend.knowledge.entity;

/**
 * Olguların (fact) doğrulama durumlarını tanımlar.
 */
public enum VerificationStatus {

    /** Henüz doğrulanmamış */
    UNVERIFIED,

    /** Kısmen doğrulanmış */
    PARTIALLY_VERIFIED,

    /** Tam doğrulanmış */
    VERIFIED,

    /** Tartışmalı / İhtilaflı */
    DISPUTED
}
