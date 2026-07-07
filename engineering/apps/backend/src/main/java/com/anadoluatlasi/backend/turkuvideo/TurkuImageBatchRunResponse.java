package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * FP-020 Task 04: Response DTO for provider-backed image batch run.
 *
 * <p>Safe output only. Uses opaque internal refs.
 * No base64, no URLs, no storage keys, no raw provider payloads.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuImageBatchRunResponse {

    private UUID projectId;
    private UUID batchId;
    private TurkuProviderMode providerMode;
    private TurkuImageBatchStatus batchStatus;
    private TurkuVideoWorkflowStage currentStage;
    private String safeOutputSummary;
    private String safeFailureCode;
    private TurkuProviderTraceReference traceRef;
    private int requestCount;
    private int successCount;
    private int failureCount;
    private boolean humanReviewRequired;
    private boolean autoApproved;
    private boolean copyrightReviewRequired;
    private List<TurkuImageBatchRunSceneStatus> sceneStatuses;
    private List<String> reviewFlags;
    private List<String> safeWarnings;
}
