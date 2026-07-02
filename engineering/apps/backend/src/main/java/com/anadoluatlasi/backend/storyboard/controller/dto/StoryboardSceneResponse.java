package com.anadoluatlasi.backend.storyboard.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-003: Storyboard scene REST yanıt DTO'su.
 * FP-002 ScenarioBeatResponse pattern'ı ile tutarlıdır.
 * Tüm safe scene alanlarını içerir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryboardSceneResponse {
    private UUID id;
    private Integer sceneNumber;
    private String sourceBeatType;
    private Integer sourceBeatOrder;
    private String sceneTitle;
    private String visualDescription;
    private String narrationText;
    private String onScreenText;
    private String cameraDirection;
    private String transitionNote;
    private String mood;
    private String location;
    private String characters;
    private Integer estimatedDurationSeconds;
}
