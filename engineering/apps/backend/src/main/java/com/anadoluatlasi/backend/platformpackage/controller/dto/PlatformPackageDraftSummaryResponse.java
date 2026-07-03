package com.anadoluatlasi.backend.platformpackage.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-004: Platform paket taslağı özet yanıt DTO'su (liste görünümü).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageDraftSummaryResponse {
    private UUID id;
    private UUID contentProjectId;
    private UUID sourceStoryboardDraftId;
    private String packageType;
    private String status;
    private Integer version;
    private String title;
    private String captionOrDescription;
    private String generatedBy;
    private String providerType;
    private Integer targetDurationSeconds;
    private String aspectRatio;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
}
