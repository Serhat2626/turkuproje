package com.anadoluatlasi.backend.visualasset.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-006: Visual Asset Plan red istek DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualAssetPlanRejectRequest {
    @NotBlank(message = "Red sebebi zorunludur")
    private String reason;
}
