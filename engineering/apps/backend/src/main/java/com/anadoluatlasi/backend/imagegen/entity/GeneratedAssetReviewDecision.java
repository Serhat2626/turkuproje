package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-007: Generated asset review kararı.
 *
 * <p>Admin tarafından verilen explicit human review kararını temsil eder.
 * Otomatik karar verilmez; her review bir admin aksiyonudur.</p>
 */
public enum GeneratedAssetReviewDecision {

    /** Admin görseli onayladı */
    APPROVED,

    /** Admin görseli reddetti — rejection reason zorunludur */
    REJECTED
}
