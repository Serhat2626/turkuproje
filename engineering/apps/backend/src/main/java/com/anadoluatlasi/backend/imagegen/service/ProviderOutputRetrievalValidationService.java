package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates provider output retrieval followed by media validation.
 *
 * FP-009 Task 06 — Retrieval → Validation-first flow.
 *
 * This service proves the validation-before-storage contract:
 * 1. Retrieve provider output via {@link ProviderOutputRetrievalClient}.
 * 2. If retrieval fails → return safe failure (validation NOT called).
 * 3. If retrieval succeeds → validate via {@link ImageMediaValidationService}.
 * 4. If validation fails → return safe failure (storage NOT called).
 * 5. If validation succeeds → return validated result for future storage.
 *
 * Storage is NOT called in this task — only retrieval + validation.
 *
 * Safety: provider URL is a transient method parameter only — never
 * persisted, logged, returned, or stored in any model.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderOutputRetrievalValidationService {

    private final ProviderOutputRetrievalClient retrievalClient;
    private final ImageMediaValidationService validationService;

    /**
     * Execute retrieval → validation flow.
     *
     * @param providerOutputUrl transient provider URL — never persisted/logged/returned
     * @param mediaAssetId      target media asset
     * @param generatedAssetId  target generated asset
     * @param providerAlias     provider name (safe metadata)
     * @param correlationId     trace identifier
     * @return safe orchestration result
     */
    public RetrievalValidationResult execute(String providerOutputUrl,
                                              UUID mediaAssetId,
                                              UUID generatedAssetId,
                                              String providerAlias,
                                              String correlationId) {

        log.info("[RETRIEVAL_VALIDATION] Starting retrieval+validation. correlationId={}, mediaAssetId={}, providerAlias={}",
                correlationId, mediaAssetId, providerAlias);

        // Step 1: Retrieve provider output
        ProviderOutputRetrievalRequest retrievalRequest = ProviderOutputRetrievalRequest.builder()
                .mediaAssetId(mediaAssetId)
                .generatedAssetId(generatedAssetId)
                .providerAlias(providerAlias)
                .correlationId(correlationId)
                .build();

        ProviderOutputRetrievalResult retrievalResult = retrievalClient.retrieve(
                providerOutputUrl, retrievalRequest);

        // Step 2: Handle retrieval failure — validation is NOT called
        if (!retrievalResult.isSuccess()) {
            log.warn("[RETRIEVAL_VALIDATION] Retrieval failed. correlationId={}, errorCode={}",
                    correlationId, retrievalResult.getErrorCode());

            return RetrievalValidationResult.builder()
                    .success(false)
                    .failurePhase(RetrievalValidationResult.FailurePhase.RETRIEVAL)
                    .errorCode(retrievalResult.getErrorCode())
                    .errorMessage(retrievalResult.getErrorMessage())
                    .correlationId(correlationId)
                    .retryable(retrievalResult.isRetryable())
                    .completedAt(LocalDateTime.now())
                    .build();
        }

        // Step 3: Validate retrieved bytes
        byte[] imageBytes = retrievalResult.getImageBytes();

        log.info("[RETRIEVAL_VALIDATION] Retrieval success, starting validation. correlationId={}, contentLength={}",
                correlationId, retrievalResult.getContentLength());

        ImageMediaValidationResult validationResult = validationService.validate(
                imageBytes, correlationId);

        // Step 4: Handle validation failure — storage is NOT called
        if (!validationResult.isValid()) {
            log.warn("[RETRIEVAL_VALIDATION] Validation failed. correlationId={}, errorCode={}",
                    correlationId, validationResult.getErrorCode());

            return RetrievalValidationResult.builder()
                    .success(false)
                    .failurePhase(RetrievalValidationResult.FailurePhase.VALIDATION)
                    .errorCode(validationResult.getErrorCode())
                    .errorMessage(validationResult.getErrorMessage())
                    .correlationId(correlationId)
                    .retryable(false)
                    .completedAt(LocalDateTime.now())
                    .build();
        }

        // Step 5: Return validated result (storage deferred to future task)
        log.info("[RETRIEVAL_VALIDATION] Validation success. correlationId={}, mimeType={}, size={}",
                correlationId, validationResult.getDetectedMimeType(), validationResult.getFileSizeBytes());

        return RetrievalValidationResult.builder()
                .success(true)
                .validatedImageBytes(imageBytes)
                .detectedMimeType(validationResult.getDetectedMimeType())
                .fileSizeBytes(validationResult.getFileSizeBytes())
                .width(validationResult.getWidth())
                .height(validationResult.getHeight())
                .checksumSha256(validationResult.getChecksumSha256())
                .correlationId(correlationId)
                .retryable(false)
                .completedAt(LocalDateTime.now())
                .build();
    }
}
