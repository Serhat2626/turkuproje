package com.anadoluatlasi.backend.application.knowledge.create;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bilgi öğesi oluşturma işleminin sonucu.
 * Entity döndürmez, yalnızca sonuç bilgisi taşır.
 */
@Value
@Builder
public class CreateKnowledgeItemResult {

    UUID id;
    String title;
    String slug;
    String type;
    String status;
    String language;
    LocalDateTime createdAt;
}
