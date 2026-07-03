package com.anadoluatlasi.backend.platformpackage.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-004: Scene usage düzenleme istek DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageSceneUsageEditRequest {
    private int sceneNumber;
    private String usageType;
    private Boolean included;
    private String adaptationNote;
}
