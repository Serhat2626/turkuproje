package com.startup.turkuproje.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Araştırma yanıtı
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchResponse {

    private UUID id;
    private UUID projectId;
    private String content;
    private String summary;
    private String region;
    private String historicalPeriod;
    private String emotionalTone;
    private String uncertainties;
    private Boolean approved;
    private String userNotes;
    private Integer version;
    private LocalDateTime createdAt;
}
