package com.anadoluatlasi.backend.content.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: İçerik projesi özet yanıt DTO'su.
 * Liste endpoint'inin yanıtı — detay alanları içermez.
 */
@Value
@Builder
public class ContentProjectSummaryResponse {

    UUID id;
    UUID knowledgeItemId;
    String knowledgeItemTitle;
    String contentPurpose;
    String status;
    String language;
    LocalDateTime createdAt;
}
