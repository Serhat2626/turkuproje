package com.anadoluatlasi.backend.storyboard.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-003: Storyboard üretim endpoint yanıtı.
 * Internal AI contract field'ları (StoryboardAgentRequest/Response) içermez.
 * FP-002 ScenarioGenerateResponse pattern'ı ile tutarlıdır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryboardGenerateResponse {
    private boolean success;
    private UUID contentProjectId;
    private UUID storyboardDraftId;
    private Integer version;
    private String status;
    private String storyboardTitle;
    private Integer sceneCount;
    private String message;
    private String errorCode;
}
