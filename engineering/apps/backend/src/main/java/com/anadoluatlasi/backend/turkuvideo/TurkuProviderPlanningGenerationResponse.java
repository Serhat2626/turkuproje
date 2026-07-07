package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * FP-020 Task 03: Response DTO for provider-backed single-stage generation.
 *
 * <p>Safe output only. No raw provider payload, base64, URLs, storage, secrets.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuProviderPlanningGenerationResponse {

    private UUID projectId;
    private TurkuVideoWorkflowStage currentStage;
    private TurkuProviderMode providerMode;
    private String generationStage;
    private String safeOutputSummary;
    private TurkuProviderTraceReference traceRef;
    private String safeFailureCode;
    private TurkuSourceConfidence outputConfidence;
    private boolean humanReviewRequired;
    private boolean autoApproved;
    private boolean copyrightReviewRequired;
    private List<String> reviewFlags;
    private List<String> safeWarnings;
}
