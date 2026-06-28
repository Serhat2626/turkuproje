package com.anadoluatlasi.backend.content.entity;

/**
 * İçerik projesinin yaşam döngüsündeki durumunu belirten enum.
 * DRAFT → SCRIPTING → STORYBOARDING → MEDIA_PRODUCTION → REVIEW → READY_TO_PUBLISH → PUBLISHED → ARCHIVED
 */
public enum ContentProjectStatus {
    DRAFT,
    SCRIPTING,
    STORYBOARDING,
    MEDIA_PRODUCTION,
    REVIEW,
    READY_TO_PUBLISH,
    PUBLISHED,
    ARCHIVED
}
