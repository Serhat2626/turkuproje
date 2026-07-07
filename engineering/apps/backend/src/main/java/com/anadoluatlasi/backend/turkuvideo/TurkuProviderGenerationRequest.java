package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * FP-020 Task 03: Request DTO for provider-backed generation endpoints.
 *
 * <p>Safe input only. No secrets, tokens, raw prompts, or provider payloads.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuProviderGenerationRequest {

    /** Optional narrative angle hint (max 200 chars). */
    private String narrativeAngleHint;

    /** Optional additional context (max 500 chars). */
    private String additionalContextHint;

    /** Optional style/mood hint for generation. */
    private String styleHint;

    /** Optional key-value params for safe prompt customization. */
    private Map<String, String> safeParams;
}
