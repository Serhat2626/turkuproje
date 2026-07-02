package com.anadoluatlasi.backend.storyboard.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-003: Storyboard red istek DTO'su.
 * FP-002 ScenarioRejectRequest pattern'ı ile tutarlıdır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardRejectRequest {
    @NotBlank(message = "Red sebebi zorunludur")
    private String reason;
}
