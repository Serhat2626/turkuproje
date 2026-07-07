package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-020 Task 02: Opaque provider audit trace reference.
 *
 * <p>No secrets, API keys, raw endpoints, or credentials.
 * traceId is an opaque UUID-based identifier for audit only.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuProviderTraceReference {

    private String traceId;
    private String generationStage;
    private String providerAlias;
    private String timestamp;
}
