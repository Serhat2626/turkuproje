package com.anadoluatlasi.backend.knowledge.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bilgi öğesi yanıt DTO'su.
 * Entity değildir, UseCase Result'tan üretilir.
 */
@Value
@Builder
public class KnowledgeItemResponse {

    UUID id;
    String title;
    String slug;
    String type;
    String status;
    String language;
    LocalDateTime createdAt;
}
