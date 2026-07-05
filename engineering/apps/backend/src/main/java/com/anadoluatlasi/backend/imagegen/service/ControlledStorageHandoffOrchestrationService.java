package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.ImageGeneratedAssetRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-009 Task 09: Controlled storage handoff orchestration.
 *
 * <p>Wires the full pipeline: retrieval → validation → private storage → status update.
 * Entry point for the explicit storage handoff endpoint.</p>
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>Load GeneratedAsset + MediaAsset safely</li>
 *   <li>Retrieve provider output via retrieval+validation service</li>
 *   <li>If retrieval/validation fails → safe failure status</li>
 *   <li>If validation succeeds → delegate to storage handoff service</li>
 *   <li>Return safe result — no URL/key/secret exposure</li>
 * </ol>
 *
 * <p>Safety rules:</p>
 * <ul>
 *   <li>Provider URL is transient method parameter only — never persisted/logged/returned</li>
 *   <li>Storage key remains internal-only</li>
 *   <li>No automatic approval — stored ≠ approved</li>
 *   <li>No READY_FOR_PUBLISH, COMPLETED, publish/export</li>
 *   <li>Human review gate preserved</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ControlledStorageHandoffOrchestrationService {

    private final ImageGeneratedAssetRepository generatedAssetRepository;
    private final ImageMediaAssetRepository mediaAssetRepository;
    private final ProviderOutputRetrievalValidationService retrievalValidationService;
    private final ImageStorageHandoffService storageHandoffService;

    /**
     * Execute controlled storage handoff for a generated asset.
     *
     * <p>This is the full FP-009 pipeline: retrieval → validation → storage.</p>
     *
     * <p>The provider output URL is passed as a transient parameter.
     * It is NEVER persisted, logged, returned, or stored in any model.</p>
     *
     * @param generatedAssetId   the generated asset to process
     * @param providerOutputUrl  transient provider URL — NEVER persisted/logged
     * @param correlationId      trace identifier
     * @return safe handoff result
     */
    @Transactional
    public ImageStorageHandoffResult executeControlledHandoff(
            UUID generatedAssetId,
            String providerOutputUrl,
            String correlationId) {

        log.info("[CONTROLLED_HANDOFF] Starting controlled handoff. generatedAssetId={}, correlationId={}, hasProviderUrl={}",
                generatedAssetId, correlationId,
                providerOutputUrl != null && !providerOutputUrl.isBlank());

        // 1. Load GeneratedAsset
        ImageGeneratedAsset generatedAsset = generatedAssetRepository.findById(generatedAssetId)
                .orElse(null);
        if (generatedAsset == null) {
            log.warn("[CONTROLLED_HANDOFF] Generated asset not found. generatedAssetId={}, correlationId={}",
                    generatedAssetId, correlationId);
            return safeFailure(null, generatedAssetId, correlationId,
                    ImageStorageErrorCode.UNKNOWN_STORAGE_HANDOFF_FAILURE,
                    "İlgili generated asset bulunamadı");
        }

        // 2. Load or create MediaAsset
        UUID mediaAssetId = generatedAsset.getImageMediaAssetId();
        if (mediaAssetId == null) {
            // Create a new media asset for this generated asset
            ImageMediaAsset newMediaAsset = ImageMediaAsset.builder()
                    .assetKind(ImageMediaAssetKind.GENERATED_IMAGE)
                    .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_PENDING)
                    .previewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE)
                    .build();
            newMediaAsset = mediaAssetRepository.save(newMediaAsset);
            mediaAssetId = newMediaAsset.getId();

            generatedAsset.setImageMediaAssetId(mediaAssetId);
            generatedAssetRepository.save(generatedAsset);

            log.info("[CONTROLLED_HANDOFF] Created new media asset. mediaAssetId={}, generatedAssetId={}, correlationId={}",
                    mediaAssetId, generatedAssetId, correlationId);
        }

        // 3. Provider URL validation
        if (providerOutputUrl == null || providerOutputUrl.isBlank()) {
            log.warn("[CONTROLLED_HANDOFF] Provider output URL is missing. generatedAssetId={}, correlationId={}",
                    generatedAssetId, correlationId);
            return safeFailure(mediaAssetId, generatedAssetId, correlationId,
                    ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE,
                    ImageStorageErrorNormalizer.safeMessage(
                            ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE));
        }

        // 4. Execute retrieval → validation
        String providerAlias = "OPENAI"; // Safe metadata only
        RetrievalValidationResult rvResult = retrievalValidationService.execute(
                providerOutputUrl, mediaAssetId, generatedAssetId,
                providerAlias, correlationId);

        if (!rvResult.isSuccess()) {
            log.warn("[CONTROLLED_HANDOFF] Retrieval/validation failed. phase={}, errorCode={}, correlationId={}",
                    rvResult.getFailurePhase(), rvResult.getErrorCode(), correlationId);

            ImageStorageErrorCode errorCode = rvResult.getErrorCode() != null
                    ? rvResult.getErrorCode()
                    : ImageStorageErrorCode.UNKNOWN_STORAGE_HANDOFF_FAILURE;
            String safeMessage = rvResult.getErrorMessage() != null
                    ? rvResult.getErrorMessage()
                    : ImageStorageErrorNormalizer.safeMessage(errorCode);

            return safeFailure(mediaAssetId, generatedAssetId, correlationId,
                    errorCode, safeMessage);
        }

        // 5. Delegate to existing storage handoff service with validated bytes
        log.info("[CONTROLLED_HANDOFF] Retrieval+validation success. Delegating to storage handoff. correlationId={}",
                correlationId);

        ImageStorageHandoffRequest handoffRequest = ImageStorageHandoffRequest.builder()
                .mediaAssetId(mediaAssetId)
                .generatedAssetId(generatedAssetId)
                .imageBytes(rvResult.getValidatedImageBytes())
                .contentTypeHint(rvResult.getDetectedMimeType())
                .correlationId(correlationId)
                .requestedBy("system:controlled-handoff")
                .requestedAt(LocalDateTime.now())
                .build();

        ImageStorageHandoffResult handoffResult = storageHandoffService.executeHandoff(handoffRequest);

        log.info("[CONTROLLED_HANDOFF] Handoff completed. success={}, storageStatus={}, previewStatus={}, correlationId={}",
                handoffResult.isSuccess(),
                handoffResult.getStorageHandoffStatus(),
                handoffResult.getPreviewStatus(),
                correlationId);

        return handoffResult;
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private ImageStorageHandoffResult safeFailure(UUID mediaAssetId,
                                                    UUID generatedAssetId,
                                                    String correlationId,
                                                    ImageStorageErrorCode errorCode,
                                                    String safeMessage) {
        return ImageStorageHandoffResult.builder()
                .success(false)
                .mediaAssetId(mediaAssetId)
                .generatedAssetId(generatedAssetId)
                .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_FAILED)
                .previewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .correlationId(correlationId)
                .build();
    }
}
