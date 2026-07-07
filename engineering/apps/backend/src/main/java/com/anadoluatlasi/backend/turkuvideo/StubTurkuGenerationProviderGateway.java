package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * FP-020 Task 03: Stub provider gateway implementation.
 *
 * <p>Returns safe deterministic output through the gateway contract.
 * No external API calls. No raw provider payloads. No secrets.</p>
 */
@Component
public class StubTurkuGenerationProviderGateway implements TurkuGenerationProviderGateway {

    @Override
    public TurkuAiGenerationResult generateAi(
            TurkuAiGenerationRequest request,
            TurkuGenerationSafetyEnvelope envelope) {

        TurkuProviderTraceReference trace = TurkuProviderTraceReference.builder()
                .traceId("stub-trace-" + UUID.randomUUID())
                .generationStage(request.getGenerationStage())
                .providerAlias("deterministic-stub")
                .timestamp(Instant.now().toString())
                .build();

        return TurkuAiGenerationResult.builder()
                .projectId(request.getProjectId())
                .generationStage(request.getGenerationStage())
                .traceRef(trace)
                .safeOutputSummary("Güvenli stub çıktısı — " + request.getGenerationStage()
                        + " aşaması için deterministik içerik üretildi.")
                .outputConfidence(TurkuSourceConfidence.LOW)
                .needsHumanReview(true)
                .copyrightReviewRequired(true)
                .reviewFlags(List.of(
                        "HUMAN_REVIEW_REQUIRED",
                        "COPYRIGHT_REVIEW_REQUIRED",
                        "CULTURAL_CONTEXT_REVIEW_REQUIRED",
                        "LYRICS_NOT_INCLUDED",
                        "STUB_OUTPUT"))
                .safeWarnings(List.of("Deterministik stub çıktısı — gerçek sağlayıcı kullanılmadı"))
                .build();
    }

    @Override
    public TurkuImageGenerationResult generateImage(
            TurkuImageGenerationRequest request,
            TurkuGenerationSafetyEnvelope envelope) {

        TurkuProviderTraceReference trace = TurkuProviderTraceReference.builder()
                .traceId("stub-trace-" + UUID.randomUUID())
                .generationStage("IMAGE_GENERATION")
                .providerAlias("deterministic-stub")
                .timestamp(Instant.now().toString())
                .build();

        return TurkuImageGenerationResult.builder()
                .projectId(request.getProjectId())
                .sceneNo(request.getSceneNo())
                .safeInternalRef("internal-ref:" + UUID.randomUUID())
                .success(true)
                .traceRef(trace)
                .needsHumanReview(true)
                .build();
    }
}
