package com.anadoluatlasi.backend.content.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: İçerik projesi detay yanıt DTO'su.
 */
@Value
@Builder
public class ContentProjectResponse {

    UUID id;
    UUID knowledgeItemId;
    String knowledgeItemTitle;
    String contentPurpose;
    String targetAudience;
    String tone;
    String language;
    String formatHint;
    String lengthHint;
    String status;
    Integer retryCount;
    String lastErrorType;
    String lastErrorMessage;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
