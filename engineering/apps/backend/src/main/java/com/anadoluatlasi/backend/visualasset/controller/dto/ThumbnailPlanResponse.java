package com.anadoluatlasi.backend.visualasset.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-006: Thumbnail Plan REST yanıt DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThumbnailPlanResponse {
    private UUID id;
    private UUID sourceStoryboardSceneId;
    private Integer sourceSceneNumber;
    private String thumbnailPrompt;
    private String negativePrompt;
    private String textOverlayGuidance;
    private String compositionNotes;
    private String emotionalHook;
    private String aspectRatio;
    private String styleDirective;
    private String colorPalette;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
