package com.anadoluatlasi.backend.imagegen.provider;

import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * FP-007: Mock image generation provider.
 *
 * <p>External call yapmaz, gerçek image üretmez.
 * Deterministic mock success/failure üretir.</p>
 *
 * <p>Controlled failure: safePromptSummary "MOCK_FAIL" içeriyorsa
 * FAILED result döner.</p>
 *
 * <p>Bu mock bile raw prompt, public URL, signed URL,
 * image binary, base64, storage reference üretmez.</p>
 */
@Slf4j
public class MockImageGenerationProviderGateway implements ImageGenerationProviderGateway {

    private static final String PROVIDER_ALIAS = "MOCK";
    private static final String MODEL_NAME = "mock-image-gen-v1";
    private static final String MOCK_FAIL_TRIGGER = "MOCK_FAIL";

    @Override
    public ImageProviderGenerationResult generate(ImageProviderGenerationRequest request) {
        log.info("Mock image generation başlatıldı: requestId={}, correlationId={}",
                request.getRequestId(), request.getCorrelationId());

        long startMs = System.currentTimeMillis();

        // Controlled failure trigger
        if (request.getSafePromptSummary() != null
                && request.getSafePromptSummary().contains(MOCK_FAIL_TRIGGER)) {
            log.info("Mock controlled failure tetiklendi: requestId={}", request.getRequestId());
            long durationMs = System.currentTimeMillis() - startMs;
            return ImageProviderGenerationResult.builder()
                    .success(false)
                    .providerAlias(PROVIDER_ALIAS)
                    .modelName(MODEL_NAME)
                    .generationDurationMs(durationMs)
                    .errorCode(ImageGenerationErrorCode.PROVIDER_REJECTED_REQUEST)
                    .errorMessage("Mock controlled failure — generation request reddedildi")
                    .correlationId(request.getCorrelationId())
                    .completedAt(LocalDateTime.now())
                    .build();
        }

        // Mock success — deterministic metadata
        long durationMs = System.currentTimeMillis() - startMs;

        ImageProviderGenerationResult result = ImageProviderGenerationResult.builder()
                .success(true)
                .providerAlias(PROVIDER_ALIAS)
                .modelName(MODEL_NAME)
                .outputCount(1)
                .generationDurationMs(durationMs)
                .mimeType("image/png")
                .width(1024)
                .height(1024)
                .fileSizeBytes(204800L)
                .correlationId(request.getCorrelationId())
                .completedAt(LocalDateTime.now())
                .qualitySignalsSummary("Mock generation — quality signals not evaluated")
                .safetyFlagsSummary("Mock generation — no safety concerns")
                .build();

        log.info("Mock image generation tamamlandı: requestId={}, success={}, durationMs={}",
                request.getRequestId(), result.isSuccess(), result.getGenerationDurationMs());

        return result;
    }

    @Override
    public String getProviderAlias() {
        return PROVIDER_ALIAS;
    }
}
