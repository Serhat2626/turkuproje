package com.anadoluatlasi.backend.platformpackage.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * FP-004: Platform paket üretim endpoint yanıtı.
 * Internal AI contract field'ları expose edilmez.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformPackageGenerateResponse {
    private boolean success;
    private UUID contentProjectId;
    private UUID sourceStoryboardDraftId;
    private List<UUID> generatedPackageIds;
    private Integer packageCount;
    private String message;
    private String errorCode;
}
