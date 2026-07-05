package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageGeneratedAsset;
import com.anadoluatlasi.backend.imagegen.provider.ProviderOutputReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * FP-010 Task 04: Immediate storage handoff bridge.
 *
 * <p>Connects generation output (ProviderOutputReference) to
 * controlled storage handoff orchestration when enabled.</p>
 *
 * <p>This service is called after successful generation + asset creation.
 * It extracts the transient provider URL and delegates to the existing
 * {@link ControlledStorageHandoffOrchestrationService}.</p>
 *
 * <p>Safety rules:</p>
 * <ul>
 *   <li>Provider URL is transient — used once then discarded</li>
 *   <li>Provider URL is NEVER persisted, logged, or exposed</li>
 *   <li>Storage success does NOT mean approved</li>
 *   <li>No READY_FOR_PUBLISH, COMPLETED, publish/export</li>
 *   <li>Human review gate preserved</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImmediateStorageHandoffService {

    private final ImmediateStorageHandoffProperties config;
    private final ControlledStorageHandoffOrchestrationService controlledHandoffService;

    /**
     * Attempt immediate storage handoff after generation.
     *
     * <p>If disabled or if no provider URL is available, this is a safe no-op.
     * Handoff failure is logged but does NOT fail the generation — the asset
     * remains in GENERATED_PENDING_REVIEW and can be retried via admin endpoint.</p>
     *
     * @param generatedAsset  the created generated asset
     * @param outputReference transient provider output reference (URL + safe result)
     */
    public void attemptImmediateHandoff(ImageGeneratedAsset generatedAsset,
                                         ProviderOutputReference outputReference) {
        // 1. Config check — disabled = safe no-op
        if (!config.isEnabled()) {
            log.info("[IMMEDIATE_HANDOFF] Disabled by config. generatedAssetId={}", generatedAsset.getId());
            return;
        }

        // 2. Output reference check
        if (outputReference == null) {
            log.info("[IMMEDIATE_HANDOFF] No output reference. generatedAssetId={}", generatedAsset.getId());
            return;
        }

        // 3. Transient URL extraction — NEVER persist/log the actual URL
        String transientUrl = outputReference.providerOutputUrl();
        if (transientUrl == null || transientUrl.isBlank()) {
            log.info("[IMMEDIATE_HANDOFF] No provider URL available. generatedAssetId={}", generatedAsset.getId());
            return;
        }

        // 4. Correlation ID from generation result
        String correlationId = (outputReference.generationResult() != null
                && outputReference.generationResult().getCorrelationId() != null)
                ? outputReference.generationResult().getCorrelationId().toString()
                : "immediate-" + generatedAsset.getId().toString();

        // Safety: log only safe context, NEVER the URL
        log.info("[IMMEDIATE_HANDOFF] Starting immediate handoff. generatedAssetId={}, correlationId={}",
                generatedAsset.getId(), correlationId);

        try {
            // 5. Delegate to existing controlled handoff orchestration
            // Provider URL is passed as transient parameter — used once then GC'd
            ImageStorageHandoffResult result = controlledHandoffService.executeControlledHandoff(
                    generatedAsset.getId(),
                    transientUrl,
                    correlationId
            );

            if (result.isSuccess()) {
                log.info("[IMMEDIATE_HANDOFF] Handoff success. generatedAssetId={}, " +
                                "storageStatus={}, previewStatus={}, correlationId={}",
                        generatedAsset.getId(),
                        result.getStorageHandoffStatus(),
                        result.getPreviewStatus(),
                        correlationId);
            } else {
                // Handoff failure is non-fatal — generation succeeded, storage can be retried
                log.warn("[IMMEDIATE_HANDOFF] Handoff failed (non-fatal). generatedAssetId={}, " +
                                "errorCode={}, correlationId={}",
                        generatedAsset.getId(),
                        result.getErrorCode(),
                        correlationId);
            }
        } catch (Exception e) {
            // Safety: never log exception message which may contain URL
            log.warn("[IMMEDIATE_HANDOFF] Unexpected failure (non-fatal). generatedAssetId={}, " +
                            "exceptionType={}, correlationId={}",
                    generatedAsset.getId(),
                    e.getClass().getSimpleName(),
                    correlationId);
        }
    }
}
