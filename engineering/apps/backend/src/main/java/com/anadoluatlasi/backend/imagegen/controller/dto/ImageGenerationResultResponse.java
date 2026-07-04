package com.anadoluatlasi.backend.imagegen.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Image generation result safe response DTO.
 *
 * <p>Normalized provider result metadata. providerRequestId admin-facing
 * olarak döndürülmez. Raw provider response/payload döndürülmez.
 * Signed URL döndürülmez. Full prompt döndürülmez.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageGenerationResultResponse {
    private UUID id;
    private UUID imageGenerationRequestId;
    private UUID visualAssetPlanId;
    private UUID visualAssetItemId;
    private String providerType;
    private String modelName;
    // providerRequestId intentionally excluded — internal-only
    private Long generationDurationMs;
    private Integer outputCount;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Long fileSizeBytes;
    private String checksumSha256;
    private String errorCode;
    private String errorMessage;
    private UUID correlationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
