package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.imagegen.controller.dto.*;
import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FP-007: Generated asset read/query servisi.
 *
 * <p>Sorumluluklar:</p>
 * <ul>
 *   <li>Pending review listesi</li>
 *   <li>Generated asset listesi (filtered)</li>
 *   <li>Review detail aggregate</li>
 * </ul>
 *
 * <p>Bu service review decision yazmaz. Approve/reject yapmaz.
 * Preview üretmez. Storage URL üretmez. Generic status update sunmaz.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGeneratedAssetQueryService {

    private final ImageGeneratedAssetRepository generatedAssetRepository;
    private final ImageMediaAssetRepository mediaAssetRepository;
    private final ImageGenerationRequestRepository requestRepository;
    private final ImageGenerationResultRepository resultRepository;
    private final GeneratedAssetReviewRepository reviewRepository;

    // ── Pending Review List ─────────────────────────────────────────

    /**
     * GENERATED_PENDING_REVIEW status'lü generated asset'leri döner.
     */
    @Transactional(readOnly = true)
    public List<ImageGeneratedAssetResponse> listPendingReview() {
        List<ImageGeneratedAsset> assets = generatedAssetRepository
                .findByStatus(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);

        return assets.stream()
                .map(this::toResponseWithMedia)
                .collect(Collectors.toList());
    }

    // ── Generated Assets List ───────────────────────────────────────

    /**
     * Generated asset'leri opsiyonel filtrelerle listeler.
     */
    @Transactional(readOnly = true)
    public List<ImageGeneratedAssetResponse> listGeneratedAssets(
            String status,
            UUID visualAssetPlanId,
            UUID contentProjectId) {

        List<ImageGeneratedAsset> assets;

        if (visualAssetPlanId != null && status != null) {
            GeneratedAssetStatus statusEnum = parseAssetStatus(status);
            assets = generatedAssetRepository.findByVisualAssetPlanIdAndStatus(
                    visualAssetPlanId, statusEnum);
        } else if (visualAssetPlanId != null) {
            assets = generatedAssetRepository.findByVisualAssetPlanId(visualAssetPlanId);
        } else if (contentProjectId != null) {
            assets = generatedAssetRepository.findByContentProjectId(contentProjectId);
        } else if (status != null) {
            GeneratedAssetStatus statusEnum = parseAssetStatus(status);
            assets = generatedAssetRepository.findByStatus(statusEnum);
        } else {
            assets = generatedAssetRepository.findAll();
        }

        return assets.stream()
                .map(this::toResponseWithMedia)
                .collect(Collectors.toList());
    }

    // ── Review Detail ───────────────────────────────────────────────

    /**
     * Generated asset review detail — safe aggregate.
     *
     * <p>Asset, generation request, generation result, review geçmişi
     * ve lineage bilgisi içerir. Raw provider data, storage key,
     * providerRequestId, signed/public URL döndürülmez.</p>
     */
    @Transactional(readOnly = true)
    public GeneratedAssetReviewDetailResponse getReviewDetail(UUID generatedAssetId) {
        ImageGeneratedAsset asset = generatedAssetRepository.findById(generatedAssetId)
                .orElseThrow(() -> new NotFoundException("ImageGeneratedAsset", generatedAssetId));

        // Nested lookups
        ImageMediaAsset mediaAsset = asset.getImageMediaAssetId() != null
                ? mediaAssetRepository.findById(asset.getImageMediaAssetId()).orElse(null)
                : null;

        ImageGenerationRequestResponse requestResponse = asset.getImageGenerationRequestId() != null
                ? requestRepository.findById(asset.getImageGenerationRequestId())
                    .map(ImageGenerationRequestMapper::toResponse).orElse(null)
                : null;

        ImageGenerationResultResponse resultResponse = asset.getImageGenerationResultId() != null
                ? resultRepository.findById(asset.getImageGenerationResultId())
                    .map(ImageGenerationResultMapper::toResponse).orElse(null)
                : null;

        List<GeneratedAssetReviewResponse> reviewHistory = reviewRepository
                .findByGeneratedAssetId(generatedAssetId).stream()
                .map(review -> GeneratedAssetReviewResponse.builder()
                        .id(review.getId())
                        .generatedAssetId(review.getGeneratedAssetId())
                        .decision(review.getDecision() != null ? review.getDecision().name() : null)
                        .rejectionReason(review.getRejectionReason())
                        .reviewedBy(review.getReviewedBy())
                        .reviewedAt(review.getReviewedAt())
                        .createdAt(review.getCreatedAt())
                        .updatedAt(review.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return GeneratedAssetReviewDetailResponse.builder()
                .generatedAsset(ImageGeneratedAssetMapper.toResponse(asset, mediaAsset))
                .generationRequest(requestResponse)
                .generationResult(resultResponse)
                .reviewHistory(reviewHistory)
                .contentProjectId(asset.getContentProjectId())
                .visualAssetPlanId(asset.getVisualAssetPlanId())
                .visualAssetItemId(asset.getVisualAssetItemId())
                .build();
    }

    // ── Private Helpers ─────────────────────────────────────────────

    private ImageGeneratedAssetResponse toResponseWithMedia(ImageGeneratedAsset asset) {
        ImageMediaAsset mediaAsset = asset.getImageMediaAssetId() != null
                ? mediaAssetRepository.findById(asset.getImageMediaAssetId()).orElse(null)
                : null;
        return ImageGeneratedAssetMapper.toResponse(asset, mediaAsset);
    }

    private GeneratedAssetStatus parseAssetStatus(String status) {
        try {
            return GeneratedAssetStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new com.anadoluatlasi.backend.common.exception.BusinessException(
                    "Geçersiz generated asset status değeri: " + status);
        }
    }
}
