package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-020 Task 02: Provider gateway contract.
 *
 * <p>Implementations provide real or stub AI/image generation.
 * All calls must be wrapped in a safety envelope.</p>
 */
public interface TurkuGenerationProviderGateway {

    /**
     * Generates AI content (story, scenario, storyboard) through the provider.
     */
    TurkuAiGenerationResult generateAi(
            TurkuAiGenerationRequest request,
            TurkuGenerationSafetyEnvelope envelope);

    /**
     * Generates an image through the provider.
     */
    TurkuImageGenerationResult generateImage(
            TurkuImageGenerationRequest request,
            TurkuGenerationSafetyEnvelope envelope);
}
