package com.anadoluatlasi.backend.knowledge.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: Bilgi öğesi detay yanıt DTO'su.
 * Tekil getDetail ve mutasyon endpoint'lerinin yanıtı.
 */
@Value
@Builder
public class KnowledgeItemResponse {

    UUID id;
    String title;
    String slug;
    String type;
    String summary;
    String description;
    String category;
    String city;
    String district;
    String locationText;
    String notes;
    String status;
    String language;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
