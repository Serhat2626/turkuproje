package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * FP-020 Task 05: Generation safety read model.
 *
 * <p>Safe read-only view of provider mode, generation availability,
 * safety flags, and safe trace/failure references.</p>
 *
 * <p>No raw provider payloads, base64, storage keys, public URLs, or secrets.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuGenerationSafetyResponse {

    private UUID projectId;
    private TurkuProviderMode providerMode;
    private TurkuVideoWorkflowStage currentStage;
    private boolean generationAvailable;
    private String blockedReasonSummary;
    private boolean humanReviewRequired;
    private boolean autoApproved;
    private boolean copyrightReviewRequired;
    private boolean lyricsIncluded;

    // Available generation actions based on current state
    private boolean canGenerateStory;
    private boolean canGenerateScenario;
    private boolean canGenerateStoryboard;
    private boolean canGeneratePlanningBundle;
    private boolean canRunImageBatch;

    // Safe trace/failure info
    private String safeFailureCode;
    private TurkuProviderTraceReference lastTraceRef;

    // Review flags
    private List<String> reviewFlags;
    private List<String> safeWarnings;
}
