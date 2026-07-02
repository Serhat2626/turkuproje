package com.anadoluatlasi.backend.scenario.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioBeatEditRequest {
    private String type;
    private String label;
    private String body;
    private Integer orderIndex;
    private Integer estimatedDurationSeconds;
}
