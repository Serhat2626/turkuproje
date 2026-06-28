package com.anadoluatlasi.backend.application.content;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: Content Project operasyonlarının sonuç nesnesi.
 * Entity döndürmez, yalnızca sonuç bilgisi taşır.
 */
@Value
@Builder
public class ContentProjectResult {

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
