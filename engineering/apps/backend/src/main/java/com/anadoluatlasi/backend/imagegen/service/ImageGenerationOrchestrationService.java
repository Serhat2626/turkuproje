package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGenerationResultResponse;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationErrorCode;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequest;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequestStatus;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationResult;
import com.anadoluatlasi.backend.imagegen.provider.ImageGenerationProviderGateway;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationRequest;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationResult;
import com.anadoluatlasi.backend.imagegen.provider.ProviderOutputReference;
import com.anadoluatlasi.backend.imagegen.repository.ImageGenerationRequestRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageGenerationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Image generation orchestration service.
 *
 * <p>REQUESTED ImageGenerationRequest için provider gateway'i çalıştırır.
 * Status akışı: REQUESTED → GENERATING → GENERATED_PENDING_REVIEW veya FAILED</p>
 *
 * <p>Başarılı generation sonrasında handoff service aracılığıyla
 * ImageMediaAsset ve ImageGeneratedAsset oluşturulur.</p>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Storage upload/download yapmaz</li>
 *   <li>Review approve/reject yapmaz</li>
 *   <li>APPROVED/REJECTED/COMPLETED/READY_FOR_PUBLISH status atamaz</li>
 *   <li>Generic status update sunmaz</li>
 *   <li>Raw provider payload/response saklamaz</li>
 *   <li>Internal full prompt saklamaz</li>
 *   <li>Public/signed URL üretmez</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationOrchestrationService {

    private final ImageGenerationRequestRepository requestRepository;
    private final ImageGenerationResultRepository resultRepository;
    private final ImageGenerationProviderGateway providerGateway;
    private final ImageGeneratedAssetHandoffService handoffService;
    private final ImmediateStorageHandoffService immediateStorageHandoffService;

    /**
     * REQUESTED request için provider generation çalıştırır.
     *
     * <p>Akış:</p>
     * <ol>
     *   <li>Request lookup & REQUESTED status validation</li>
     *   <li>Status → GENERATING</li>
     *   <li>Provider gateway çağrısı</li>
     *   <li>ImageGenerationResult oluşturulur</li>
     *   <li>Success → request status GENERATED_PENDING_REVIEW</li>
     *   <li>Failure → request status FAILED</li>
     * </ol>
     *
     * <p>GeneratedAsset/MediaAsset bu methodda oluşturulmaz.</p>
     */
    @Transactional
    public ImageGenerationResultResponse executeGeneration(UUID requestId) {
        // 1. Request lookup
        ImageGenerationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("ImageGenerationRequest", requestId));

        // 2. Status validation — yalnızca REQUESTED kabul edilir
        if (request.getStatus() != ImageGenerationRequestStatus.REQUESTED) {
            throw new BusinessException(String.format(
                    "Generation yalnızca REQUESTED status'teki request için çalıştırılabilir. " +
                    "Mevcut status: %s, requestId: %s",
                    request.getStatus(), requestId));
        }

        // 3. Status → GENERATING
        request.setStatus(ImageGenerationRequestStatus.GENERATING);
        request.setProviderType(providerGateway.getProviderAlias());
        requestRepository.save(request);

        log.info("Generation başlatıldı: requestId={}, provider={}, correlationId={}",
                requestId, providerGateway.getProviderAlias(), request.getCorrelationId());

        // 4. Provider gateway çağrısı
        ImageProviderGenerationRequest providerRequest = ImageProviderGenerationRequest.builder()
                .requestId(request.getId())
                .visualAssetPlanId(request.getVisualAssetPlanId())
                .visualAssetItemId(request.getVisualAssetItemId())
                .contentProjectId(request.getContentProjectId())
                .itemType(request.getItemType() != null ? request.getItemType().name() : null)
                .safePromptSummary(request.getSafePromptSummary())
                .correlationId(request.getCorrelationId())
                .requestedAt(request.getRequestedAt())
                .build();

        // FP-010 Task 04: Use generateWithOutputReference for transient URL extraction
        ProviderOutputReference outputReference;
        try {
            outputReference = providerGateway.generateWithOutputReference(providerRequest);
        } catch (Exception e) {
            log.error("Provider gateway çağrısı başarısız: requestId={}, correlationId={}, errorType={}",
                    requestId, request.getCorrelationId(), e.getClass().getSimpleName());
            return handleProviderFailure(request, e);
        }

        // 5. Result oluştur ve request status güncelle
        ImageProviderGenerationResult providerResult = outputReference.generationResult();
        if (providerResult.isSuccess()) {
            return handleProviderSuccess(request, providerResult, outputReference);
        } else {
            return handleProviderError(request, providerResult);
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────

    private ImageGenerationResultResponse handleProviderSuccess(
            ImageGenerationRequest request,
            ImageProviderGenerationResult providerResult,
            ProviderOutputReference outputReference) {

        // ImageGenerationResult oluştur
        ImageGenerationResult result = ImageGenerationResult.builder()
                .imageGenerationRequestId(request.getId())
                .visualAssetPlanId(request.getVisualAssetPlanId())
                .visualAssetItemId(request.getVisualAssetItemId())
                .providerType(providerResult.getProviderAlias())
                .modelName(providerResult.getModelName())
                .generationDurationMs(providerResult.getGenerationDurationMs())
                .outputCount(providerResult.getOutputCount())
                .mimeType(providerResult.getMimeType())
                .width(providerResult.getWidth())
                .height(providerResult.getHeight())
                .fileSizeBytes(providerResult.getFileSizeBytes())
                .checksumSha256(providerResult.getChecksumSha256())
                .correlationId(request.getCorrelationId())
                .build();

        result = resultRepository.save(result);

        // Asset handoff — MediaAsset + GeneratedAsset oluştur
        var generatedAsset = handoffService.createAssetHandoff(request, result, providerResult);

        // FP-010 Task 04: Immediate storage handoff (non-fatal)
        // Provider URL is transient — used once then discarded via GC
        immediateStorageHandoffService.attemptImmediateHandoff(generatedAsset, outputReference);

        // Request status → GENERATED_PENDING_REVIEW
        request.setStatus(ImageGenerationRequestStatus.GENERATED_PENDING_REVIEW);
        request.setModelName(providerResult.getModelName());
        request.setCompletedAt(providerResult.getCompletedAt());
        requestRepository.save(request);

        log.info("Generation başarılı: requestId={}, resultId={}, status=GENERATED_PENDING_REVIEW",
                request.getId(), result.getId());

        return ImageGenerationResultMapper.toResponse(result);
    }

    private ImageGenerationResultResponse handleProviderError(
            ImageGenerationRequest request,
            ImageProviderGenerationResult providerResult) {

        // ImageGenerationResult oluştur (error kayıtlı)
        ImageGenerationResult result = ImageGenerationResult.builder()
                .imageGenerationRequestId(request.getId())
                .visualAssetPlanId(request.getVisualAssetPlanId())
                .visualAssetItemId(request.getVisualAssetItemId())
                .providerType(providerResult.getProviderAlias())
                .modelName(providerResult.getModelName())
                .generationDurationMs(providerResult.getGenerationDurationMs())
                .errorCode(providerResult.getErrorCode())
                .errorMessage(providerResult.getErrorMessage())
                .correlationId(request.getCorrelationId())
                .build();

        result = resultRepository.save(result);

        // Request status → FAILED
        request.setStatus(ImageGenerationRequestStatus.FAILED);
        request.setErrorCode(providerResult.getErrorCode());
        request.setErrorMessage(providerResult.getErrorMessage());
        request.setCompletedAt(providerResult.getCompletedAt());
        requestRepository.save(request);

        log.warn("Generation başarısız: requestId={}, resultId={}, errorCode={}, correlationId={}, status=FAILED",
                request.getId(), result.getId(), providerResult.getErrorCode(), request.getCorrelationId());

        return ImageGenerationResultMapper.toResponse(result);
    }

    private ImageGenerationResultResponse handleProviderFailure(
            ImageGenerationRequest request, Exception e) {

        // Unexpected exception → FAILED with UNKNOWN error
        ImageGenerationErrorCode failureCode = ImageGenerationErrorCode.UNKNOWN_GENERATION_FAILURE;
        String safeMessage = ImageGenerationErrorNormalizer.safeMessage(failureCode);

        ImageGenerationResult result = ImageGenerationResult.builder()
                .imageGenerationRequestId(request.getId())
                .visualAssetPlanId(request.getVisualAssetPlanId())
                .visualAssetItemId(request.getVisualAssetItemId())
                .providerType(providerGateway.getProviderAlias())
                .errorCode(failureCode)
                .errorMessage(safeMessage)
                .correlationId(request.getCorrelationId())
                .build();

        result = resultRepository.save(result);

        request.setStatus(ImageGenerationRequestStatus.FAILED);
        request.setErrorCode(failureCode);
        request.setErrorMessage(safeMessage);
        request.setCompletedAt(LocalDateTime.now());
        requestRepository.save(request);

        log.error("Generation exception: requestId={}, resultId={}, correlationId={}, status=FAILED",
                request.getId(), result.getId(), request.getCorrelationId());

        return ImageGenerationResultMapper.toResponse(result);
    }
}
