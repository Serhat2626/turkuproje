package com.anadoluatlasi.backend.platformpackage.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-004: Scene usage REST yanıt DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageSceneUsageResponse {
    private UUID id;
    private int sceneNumber;
    private UUID sourceStoryboardSceneId;
    private String usageType;
    private boolean included;
    private String adaptationNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
