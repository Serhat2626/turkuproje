package com.anadoluatlasi.backend.storyboard.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-003: Storyboard scene düzenleme istek DTO'su.
 * FP-002 ScenarioBeatEditRequest pattern'ı ile tutarlıdır.
 *
 * <p>sceneNumber hangi scene'in düzenleneceğini belirtir.
 * Client status, provider fields gönderemez.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardSceneEditRequest {
    private Integer sceneNumber;
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
