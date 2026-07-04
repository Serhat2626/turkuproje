package com.anadoluatlasi.backend.imagegen.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Image generation request safe response DTO.
 *
 * <p>Yalnızca güvenli metadata döner. Raw provider request/response,
 * internal full prompt, API key/secret döndürülmez.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageGenerationRequestResponse {
    private UUID id;
    private UUID contentProjectId;
    private UUID visualAssetPlanId;
    private UUID visualAssetItemId;
    private String itemType;
    private String status;
    private String providerType;
    private String modelName;
    private String safePromptSummary;
    private String errorCode;
    private String errorMessage;
    private UUID correlationId;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
