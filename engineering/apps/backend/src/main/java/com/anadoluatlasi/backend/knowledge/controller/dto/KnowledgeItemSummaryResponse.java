package com.anadoluatlasi.backend.knowledge.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: Bilgi öğesi özet yanıt DTO'su.
 * Liste endpoint'inin yanıtı — detay alanları içermez.
 */
@Value
@Builder
public class KnowledgeItemSummaryResponse {

    UUID id;
    String title;
    String slug;
    String type;
    String category;
    String city;
    String status;
    String language;
    LocalDateTime createdAt;
}
