package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-020 Task 02: Safe image generation result.
 *
 * <p>No base64, binary, public URL, signed URL, CDN URL, or storage keys.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuImageGenerationResult {

    private UUID projectId;
    private int sceneNo;
    private String safeInternalRef;
    private boolean success;
    private String safeErrorCode;
    private TurkuProviderTraceReference traceRef;
    private boolean needsHumanReview;
}
