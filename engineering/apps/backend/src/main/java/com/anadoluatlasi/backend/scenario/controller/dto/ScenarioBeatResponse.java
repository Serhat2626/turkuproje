package com.anadoluatlasi.backend.scenario.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioBeatResponse {
    private String type;
    private String label;
    private String body;
    private Integer orderIndex;
    private Integer estimatedDurationSeconds;
}
