package com.anadoluatlasi.backend.content.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: AI üretim isteği özet yanıt DTO'su.
 * Raw AI payload veya provider detayı içermez.
 */
@Value
@Builder
public class AiGenerationRequestSummaryResponse {

    UUID id;
    UUID contentProjectId;
    boolean success;
    UUID contentDraftId;
    String projectStatus;
    String draftTitle;
    String message;
    String errorType;
    Boolean retryable;
    Integer retryCount;
    Integer maxRetryCount;
}
