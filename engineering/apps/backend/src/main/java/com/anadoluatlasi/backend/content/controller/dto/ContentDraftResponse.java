package com.anadoluatlasi.backend.content.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: İçerik taslağı yanıt DTO'su.
 * AI contract veya entity döndürmez.
 */
@Value
@Builder
public class ContentDraftResponse {

    UUID id;
    UUID contentProjectId;
    String title;
    String body;
    String summary;
    String status;
    String generatedBy;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
