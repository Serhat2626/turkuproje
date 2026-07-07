package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-020 Task 02: Safe image generation request.
 *
 * <p>No raw API keys, provider endpoints, storage buckets, or signed URLs.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuImageGenerationRequest {

    private UUID projectId;
    private int sceneNo;
    private String safePromptSummary;
    private String safeNegativeNotes;
    private String styleHint;
    private String aspectRatioHint;
}
