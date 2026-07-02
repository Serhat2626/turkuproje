package com.anadoluatlasi.backend.scenario.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-002: Senaryo taslağı özet yanıt DTO'su (liste için).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioDraftSummaryResponse {
    private UUID id;
    private Integer version;
    private String status;
    private String title;
    private String generatedBy;
    private String providerType;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime editedAt;
}
