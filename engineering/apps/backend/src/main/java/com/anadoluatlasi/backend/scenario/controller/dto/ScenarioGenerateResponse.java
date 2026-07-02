package com.anadoluatlasi.backend.scenario.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-002: Senaryo üretim endpoint yanıtı.
 * Internal AI contract field'ları (ScenarioAgentRequest/Response) içermez.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioGenerateResponse {
    private boolean success;
    private UUID contentProjectId;
    private UUID scenarioDraftId;
    private Integer version;
    private String status;
    private String scenarioTitle;
    private String message;
    private String errorCode;
}
