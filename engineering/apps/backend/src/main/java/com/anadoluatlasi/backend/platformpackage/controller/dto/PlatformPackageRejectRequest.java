package com.anadoluatlasi.backend.platformpackage.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-004: Platform paketi red istek DTO'su.
 * FP-003 StoryboardRejectRequest pattern'ı ile tutarlıdır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageRejectRequest {
    @NotBlank(message = "Red sebebi zorunludur")
    private String reason;
}
