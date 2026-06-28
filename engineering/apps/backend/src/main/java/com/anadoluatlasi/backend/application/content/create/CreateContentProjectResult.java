package com.anadoluatlasi.backend.application.content.create;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * İçerik projesi oluşturma işleminin sonucu.
 * Entity döndürmez, yalnızca sonuç bilgisi taşır.
 */
@Value
@Builder
public class CreateContentProjectResult {

    UUID id;
    String title;
    String slug;
    String contentType;
    String targetPlatform;
    String status;
    String language;
    UUID knowledgeItemId;
    LocalDateTime createdAt;
}
