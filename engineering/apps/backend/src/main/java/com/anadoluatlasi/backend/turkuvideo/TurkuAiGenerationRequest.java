package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * FP-020 Task 02: Safe AI generation request.
 *
 * <p>No API keys, secrets, credentials, authorization, or raw provider URLs.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuAiGenerationRequest {

    private UUID projectId;
    private String generationStage;
    private TurkuNarrativeContext context;
    private String promptTemplateId;
    private Map<String, String> safeParams;
}
