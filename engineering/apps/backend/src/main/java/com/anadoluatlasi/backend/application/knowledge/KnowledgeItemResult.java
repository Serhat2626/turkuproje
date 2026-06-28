package com.anadoluatlasi.backend.application.knowledge;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: Bilgi öğesi operasyonlarının sonuç nesnesi.
 * Entity döndürmez, yalnızca sonuç bilgisi taşır.
 */
@Value
@Builder
public class KnowledgeItemResult {

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
