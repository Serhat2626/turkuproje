package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-020 Task 04: Request DTO for provider-backed image batch run.
 *
 * <p>Safe input only. No secrets, tokens, raw prompts, or provider payloads.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuImageBatchRunRequest {

    /** Optional style override hint (max 200 chars). */
    private String styleOverrideHint;

    /** Optional aspect ratio hint (e.g. "16:9"). */
    private String aspectRatioHint;
}
