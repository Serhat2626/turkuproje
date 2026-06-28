package com.anadoluatlasi.backend.content.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * İçerik projesi yanıt DTO'su.
 * Entity değildir, UseCase Result'tan üretilir.
 */
@Value
@Builder
public class ContentProjectResponse {

    UUID id;
    UUID knowledgeItemId;
    String title;
    String slug;
    String contentType;
    String targetPlatform;
    String status;
    String language;
    LocalDateTime createdAt;
}
