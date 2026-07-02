package com.anadoluatlasi.backend.scenario.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioDraftEditRequest {
    private String title;
    private String summary;
    private String targetAudience;
    private String culturalObjective;
    private String tone;
    private Integer estimatedDurationSeconds;
    private String setting;
    private String characters;
    private String reviewNotes;
    private List<ScenarioBeatEditRequest> beats;
}
