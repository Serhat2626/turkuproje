package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * FP-020 Task 02: Safe AI generation result.
 *
 * <p>No raw provider response, tokens, base64, storage keys, URLs, lyrics, or binary data.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuAiGenerationResult {

    private UUID projectId;
    private String generationStage;
    private TurkuProviderTraceReference traceRef;
    private String safeOutputSummary;
    private TurkuSourceConfidence outputConfidence;
    private boolean needsHumanReview;
    private boolean copyrightReviewRequired;
    private List<String> reviewFlags;
    private List<String> safeWarnings;
}
