package com.anadoluatlasi.backend.application.content;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: Content Draft operasyonlarının sonuç nesnesi.
 */
@Value
@Builder
public class ContentDraftResult {

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
