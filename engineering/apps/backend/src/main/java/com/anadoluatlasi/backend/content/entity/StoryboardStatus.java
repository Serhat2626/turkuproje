package com.anadoluatlasi.backend.content.entity;

/**
 * Storyboard'un durumunu belirten enum.
 * AI tarafından üretilen storyboard'lar insan incelemesinden geçerek onaylanır veya reddedilir.
 */
public enum StoryboardStatus {
    DRAFT,
    AI_GENERATED,
    HUMAN_REVIEWED,
    APPROVED,
    REJECTED
}
