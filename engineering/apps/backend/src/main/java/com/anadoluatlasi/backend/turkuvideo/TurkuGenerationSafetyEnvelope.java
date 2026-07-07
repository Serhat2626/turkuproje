package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-020 Task 02: Generation safety envelope.
 *
 * <p>Wraps every provider call with safe audit metadata.
 * humanReviewRequired defaults true, autoApproved defaults false,
 * lyricsIncluded defaults false.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuGenerationSafetyEnvelope {

    private UUID envelopeId;
    private UUID projectId;
    private String generationStage;
    private TurkuProviderMode providerMode;
    private TurkuProviderTraceReference traceRef;
    private boolean outputSanitized;
    private boolean promptInjectionChecked;
    private boolean lyricsIncluded;
    private boolean copyrightClearanceVerified;
    private boolean humanReviewRequired;
    private boolean autoApproved;
    private List<String> sanitizationActions;
    private List<String> reviewFlags;
    private String createdAt;

    /**
     * Creates a default safety envelope with safe invariants.
     */
    public static TurkuGenerationSafetyEnvelope createDefault(
            UUID projectId, String generationStage, TurkuProviderMode mode) {
        return TurkuGenerationSafetyEnvelope.builder()
                .envelopeId(UUID.randomUUID())
                .projectId(projectId)
                .generationStage(generationStage)
                .providerMode(mode)
                .outputSanitized(false)
                .promptInjectionChecked(false)
                .lyricsIncluded(false)
                .copyrightClearanceVerified(false)
                .humanReviewRequired(true)
                .autoApproved(false)
                .sanitizationActions(new ArrayList<>())
                .reviewFlags(new ArrayList<>())
                .createdAt(Instant.now().toString())
                .build();
    }
}
