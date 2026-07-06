package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 02: Source confidence level for türkü cultural/historical claims.
 */
public enum TurkuSourceConfidence {
    /** No source evaluation yet */
    UNKNOWN,
    /** Low confidence — needs human review */
    LOW,
    /** Medium confidence — some corroboration */
    MEDIUM,
    /** High confidence — well-documented */
    HIGH
}
