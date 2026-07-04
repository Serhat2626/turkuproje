package com.anadoluatlasi.backend.visualasset.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-006: Visual Asset Plan üretim endpoint yanıtı.
 * Internal AI contract field'ları içermez.
 * FP-003 StoryboardGenerateResponse pattern'ı ile tutarlıdır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisualAssetPlanGenerateResponse {
    private boolean success;
    private UUID contentProjectId;
    private UUID storyboardDraftId;
    private UUID visualAssetPlanId;
    private Integer version;
    private String status;
    private String planTitle;
    private Integer scenePlanCount;
    private Boolean hasThumbnailPlan;
    private String message;
    private String errorCode;
}
