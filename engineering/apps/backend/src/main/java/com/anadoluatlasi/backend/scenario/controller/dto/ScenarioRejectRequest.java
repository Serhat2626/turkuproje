package com.anadoluatlasi.backend.scenario.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioRejectRequest {
    @NotBlank(message = "Red sebebi zorunludur")
    private String reason;
}
