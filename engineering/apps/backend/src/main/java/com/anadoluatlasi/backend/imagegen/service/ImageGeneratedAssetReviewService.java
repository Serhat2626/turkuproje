package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.imagegen.controller.dto.ApproveGeneratedAssetRequest;
import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGeneratedAssetResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.RejectGeneratedAssetRequest;
import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.GeneratedAssetReviewRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageGeneratedAssetRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Human review workflow servisi.
 *
 * <p>Sorumluluklar:</p>
 * <ul>
 *   <li>GENERATED_PENDING_REVIEW asset'i approve etmek</li>
 *   <li>GENERATED_PENDING_REVIEW asset'i reject etmek (reason zorunlu)</li>
 *   <li>GeneratedAssetReview audit kaydı oluşturmak</li>
 *   <li>Bağlı ImageMediaAsset review status'unu güncellemek</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Automatic approval yapmaz</li>
 *   <li>Generic status update sunmaz</li>
 *   <li>READY_FOR_PUBLISH/COMPLETED status atamaz</li>
 *   <li>ImageGenerationRequest/Result status'unu değiştirmez</li>
 *   <li>Storage/preview/publish/export yapmaz</li>
 *   <li>Raw provider payload/response saklamaz</li>
 *   <li>Public/signed URL üretmez</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGeneratedAssetReviewService {

    private final ImageGeneratedAssetRepository generatedAssetRepository;
    private final ImageMediaAssetRepository mediaAssetRepository;
    private final GeneratedAssetReviewRepository reviewRepository;

    /**
     * Generated asset'i onaylar.
     *
     * <p>Sadece GENERATED_PENDING_REVIEW status'ten APPROVED'a geçiş.
     * Bağlı MediaAsset review status'u REVIEW_APPROVED olur.
     * GeneratedAssetReview audit kaydı oluşturulur.</p>
     */
    @Transactional
    public ImageGeneratedAssetResponse approveGeneratedAsset(UUID generatedAssetId,
                                                              ApproveGeneratedAssetRequest request) {
        ImageGeneratedAsset asset = findAndValidateForReview(generatedAssetId);

        // 1. GeneratedAsset status → APPROVED
        asset.setStatus(GeneratedAssetStatus.APPROVED);
        asset.setReviewedBy("admin-system");
        asset.setReviewedAt(LocalDateTime.now());
        generatedAssetRepository.save(asset);

        // 2. Bağlı MediaAsset review status → REVIEW_APPROVED
        updateMediaAssetReviewStatus(asset.getImageMediaAssetId(),
                ImageMediaAssetReviewStatus.REVIEW_APPROVED);

        // 3. GeneratedAssetReview audit kaydı
        String reviewNote = request != null ? request.getReviewNote() : null;
        createReviewRecord(generatedAssetId, GeneratedAssetReviewDecision.APPROVED, reviewNote);

        log.info("Generated asset onaylandı: generatedAssetId={}, status=APPROVED",
                generatedAssetId);

        return toResponseWithMedia(asset);
    }

    /**
     * Generated asset'i reddeder.
     *
     * <p>Sadece GENERATED_PENDING_REVIEW status'ten REJECTED'a geçiş.
     * Rejection reason zorunludur.
     * Bağlı MediaAsset review status'u REVIEW_REJECTED olur.
     * GeneratedAssetReview audit kaydı oluşturulur.</p>
     */
    @Transactional
    public ImageGeneratedAssetResponse rejectGeneratedAsset(UUID generatedAssetId,
                                                             RejectGeneratedAssetRequest request) {
        ImageGeneratedAsset asset = findAndValidateForReview(generatedAssetId);

        // 1. GeneratedAsset status → REJECTED
        asset.setStatus(GeneratedAssetStatus.REJECTED);
        asset.setReviewedBy("admin-system");
        asset.setReviewedAt(LocalDateTime.now());
        asset.setRejectionReason(request.getRejectionReason());
        generatedAssetRepository.save(asset);

        // 2. Bağlı MediaAsset review status → REVIEW_REJECTED
        updateMediaAssetReviewStatus(asset.getImageMediaAssetId(),
                ImageMediaAssetReviewStatus.REVIEW_REJECTED);

        // 3. GeneratedAssetReview audit kaydı
        createReviewRecord(generatedAssetId, GeneratedAssetReviewDecision.REJECTED,
                request.getRejectionReason());

        log.info("Generated asset reddedildi: generatedAssetId={}, status=REJECTED, reason={}",
                generatedAssetId, request.getRejectionReason());

        return toResponseWithMedia(asset);
    }

    // ── Private Helpers ─────────────────────────────────────────────

    private ImageGeneratedAsset findAndValidateForReview(UUID generatedAssetId) {
        ImageGeneratedAsset asset = generatedAssetRepository.findById(generatedAssetId)
                .orElseThrow(() -> new NotFoundException("ImageGeneratedAsset", generatedAssetId));

        if (asset.getStatus() != GeneratedAssetStatus.GENERATED_PENDING_REVIEW) {
            throw new BusinessException(
                    "Review yalnızca GENERATED_PENDING_REVIEW status'teki asset için yapılabilir. " +
                    "Mevcut status: " + asset.getStatus());
        }

        return asset;
    }

    private void updateMediaAssetReviewStatus(UUID mediaAssetId,
                                               ImageMediaAssetReviewStatus reviewStatus) {
        if (mediaAssetId == null) {
            log.warn("MediaAsset ID null — review status güncellenemedi");
            return;
        }

        mediaAssetRepository.findById(mediaAssetId).ifPresentOrElse(
                mediaAsset -> {
                    mediaAsset.setReviewStatus(reviewStatus);
                    mediaAssetRepository.save(mediaAsset);
                    log.debug("MediaAsset review status güncellendi: mediaAssetId={}, reviewStatus={}",
                            mediaAssetId, reviewStatus);
                },
                () -> log.warn("MediaAsset bulunamadı: mediaAssetId={} — review status güncellenemedi",
                        mediaAssetId)
        );
    }

    private void createReviewRecord(UUID generatedAssetId,
                                     GeneratedAssetReviewDecision decision,
                                     String reason) {
        GeneratedAssetReview review = GeneratedAssetReview.builder()
                .generatedAssetId(generatedAssetId)
                .decision(decision)
                .rejectionReason(reason)
                .reviewedBy("admin-system")
                .reviewedAt(LocalDateTime.now())
                .build();

        review = reviewRepository.save(review);
        log.debug("Review kaydı oluşturuldu: reviewId={}, decision={}", review.getId(), decision);
    }

    private ImageGeneratedAssetResponse toResponseWithMedia(ImageGeneratedAsset asset) {
        ImageMediaAsset mediaAsset = asset.getImageMediaAssetId() != null
                ? mediaAssetRepository.findById(asset.getImageMediaAssetId()).orElse(null)
                : null;
        return ImageGeneratedAssetMapper.toResponse(asset, mediaAsset);
    }
}
