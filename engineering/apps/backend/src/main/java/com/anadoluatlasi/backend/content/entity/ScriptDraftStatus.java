package com.anadoluatlasi.backend.content.entity;

/**
 * Senaryo taslağının durumunu belirten enum.
 * AI tarafından üretilen taslaklar insan incelemesinden geçerek onaylanır veya reddedilir.
 */
public enum ScriptDraftStatus {
    DRAFT,
    AI_GENERATED,
    HUMAN_REVIEWED,
    APPROVED,
    REJECTED
}
