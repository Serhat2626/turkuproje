package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGenerationRequestResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.StartImageGenerationRequest;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationItemType;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequest;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequestStatus;
import com.anadoluatlasi.backend.imagegen.repository.ImageGenerationRequestRepository;
import com.anadoluatlasi.backend.visualasset.entity.SceneVisualPlan;
import com.anadoluatlasi.backend.visualasset.entity.ThumbnailPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlanStatus;
import com.anadoluatlasi.backend.visualasset.repository.VisualAssetPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FP-007: Image generation request oluşturma ve sorgulama servisi.
 *
 * <p>Sorumluluklar:</p>
 * <ul>
 *   <li>APPROVED VisualAssetPlan doğrulama</li>
 *   <li>Item bağlılık doğrulama (SceneVisualPlan veya ThumbnailPlan)</li>
 *   <li>Idempotent request oluşturma</li>
 *   <li>Request listeleme ve detay görüntüleme</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Provider gateway çağırmaz</li>
 *   <li>ImageGenerationResult oluşturmaz</li>
 *   <li>ImageGeneratedAsset oluşturmaz</li>
 *   <li>ImageMediaAsset oluşturmaz</li>
 *   <li>Storage upload/download yapmaz</li>
 *   <li>Review approve/reject yapmaz</li>
 *   <li>Generic status update sunmaz</li>
 *   <li>READY_FOR_PUBLISH atamaz</li>
 *   <li>Raw provider payload/response saklamaz</li>
 *   <li>Internal full prompt saklamaz</li>
 *   <li>API key/secret saklamaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationRequestService {

    private static final String ADMIN_PLACEHOLDER = "admin-system";

    private final ImageGenerationRequestRepository requestRepository;
    private final VisualAssetPlanRepository visualAssetPlanRepository;

    // ── Start Generation ────────────────────────────────────────────

    /**
     * APPROVED VisualAssetPlan'dan idempotent image generation request oluşturur.
     *
     * <p>Kurallar:</p>
     * <ul>
     *   <li>VisualAssetPlan APPROVED olmalıdır</li>
     *   <li>visualAssetItemId zorunludur — bulk generation desteklenmez</li>
     *   <li>Item belirtilen plana ait olmalıdır</li>
     *   <li>Aynı idempotencyKey ile mevcut request varsa o döner</li>
     *   <li>Yeni request REQUESTED status ile oluşturulur</li>
     *   <li>Prompt üretimi bu service'de yapılmaz</li>
     * </ul>
     */
    @Transactional
    public ImageGenerationRequestResponse startGeneration(StartImageGenerationRequest request) {
        // 1. visualAssetItemId zorunlu — bulk generation desteklenmez
        if (request.getVisualAssetItemId() == null) {
            throw new BusinessException(
                    "visualAssetItemId zorunludur — kontrollü generation için item belirtilmelidir");
        }

        // 2. VisualAssetPlan var mı & APPROVED mı
        VisualAssetPlan plan = visualAssetPlanRepository
                .findById(request.getVisualAssetPlanId())
                .orElseThrow(() -> new NotFoundException(
                        "VisualAssetPlan", request.getVisualAssetPlanId()));

        if (plan.getStatus() != VisualAssetPlanStatus.APPROVED) {
            throw new BusinessException(String.format(
                    "Image generation yalnızca APPROVED plan için başlatılabilir. Mevcut status: %s",
                    plan.getStatus()));
        }

        // 3. Item bu plana ait mi ve item type belirle
        UUID itemId = request.getVisualAssetItemId();
        ImageGenerationItemType itemType = resolveItemType(plan, itemId);

        // 4. Idempotency key oluştur
        String idempotencyKey = resolveIdempotencyKey(request, plan, itemId, itemType);

        // 5. Duplicate check — aynı idempotencyKey varsa mevcut request döndür
        Optional<ImageGenerationRequest> existing = requestRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent duplicate — mevcut request döndürülüyor: idempotencyKey={}, requestId={}",
                    idempotencyKey, existing.get().getId());
            return ImageGenerationRequestMapper.toResponse(existing.get());
        }

        // 6. Yeni request oluştur — status: REQUESTED
        UUID correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId()
                : UUID.randomUUID();

        ImageGenerationRequest newRequest = ImageGenerationRequest.builder()
                .contentProjectId(plan.getContentProject().getId())
                .visualAssetPlanId(plan.getId())
                .visualAssetItemId(itemId)
                .itemType(itemType)
                .status(ImageGenerationRequestStatus.REQUESTED)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .requestedBy(ADMIN_PLACEHOLDER)
                .requestedAt(LocalDateTime.now())
                .build();

        // 7. Kaydet — unique constraint race condition koruması
        try {
            newRequest = requestRepository.save(newRequest);
        } catch (DataIntegrityViolationException e) {
            // Race condition: aynı idempotencyKey başka thread tarafından kaydedilmiş
            log.warn("Idempotency race condition — mevcut request aranıyor: idempotencyKey={}",
                    idempotencyKey);
            return requestRepository.findByIdempotencyKey(idempotencyKey)
                    .map(ImageGenerationRequestMapper::toResponse)
                    .orElseThrow(() -> new BusinessException(
                            "Generation request oluşturulamadı — lütfen tekrar deneyin"));
        }

        log.info("Image generation request oluşturuldu: requestId={}, planId={}, itemId={}, itemType={}, status={}",
                newRequest.getId(), plan.getId(), itemId, itemType, newRequest.getStatus());

        return ImageGenerationRequestMapper.toResponse(newRequest);
    }

    // ── Get Request Detail ──────────────────────────────────────────

    /**
     * Tek bir generation request'in safe detayını döner.
     */
    @Transactional(readOnly = true)
    public ImageGenerationRequestResponse getRequestDetail(UUID requestId) {
        ImageGenerationRequest entity = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("ImageGenerationRequest", requestId));
        return ImageGenerationRequestMapper.toResponse(entity);
    }

    // ── List Requests ───────────────────────────────────────────────

    /**
     * Generation request'leri opsiyonel filtrelerle listeler.
     * Safe response DTO döner.
     */
    @Transactional(readOnly = true)
    public List<ImageGenerationRequestResponse> listRequests(
            String status,
            UUID visualAssetPlanId,
            UUID visualAssetItemId,
            UUID contentProjectId,
            UUID correlationId) {

        // Öncelikli filtre sırasıyla tek filtre uygula
        List<ImageGenerationRequest> results;

        if (correlationId != null) {
            results = requestRepository.findByCorrelationId(correlationId)
                    .map(List::of)
                    .orElse(List.of());
        } else if (visualAssetPlanId != null && status != null) {
            ImageGenerationRequestStatus statusEnum = parseStatus(status);
            results = requestRepository.findByVisualAssetPlanIdAndStatus(
                    visualAssetPlanId, statusEnum);
        } else if (visualAssetPlanId != null) {
            results = requestRepository.findByVisualAssetPlanId(visualAssetPlanId);
        } else if (visualAssetItemId != null) {
            results = requestRepository.findByVisualAssetItemId(visualAssetItemId);
        } else if (status != null) {
            ImageGenerationRequestStatus statusEnum = parseStatus(status);
            results = requestRepository.findByStatus(statusEnum);
        } else {
            results = requestRepository.findAll();
        }

        return results.stream()
                .map(ImageGenerationRequestMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Private Helpers ─────────────────────────────────────────────

    /**
     * Item'ın bu plana ait olup olmadığını kontrol eder ve item type belirler.
     * SceneVisualPlan veya ThumbnailPlan olabilir.
     */
    private ImageGenerationItemType resolveItemType(VisualAssetPlan plan, UUID itemId) {
        // SceneVisualPlan kontrol
        if (plan.getScenePlans() != null) {
            boolean isSceneItem = plan.getScenePlans().stream()
                    .anyMatch(scene -> scene.getId().equals(itemId));
            if (isSceneItem) {
                return ImageGenerationItemType.SCENE_IMAGE;
            }
        }

        // ThumbnailPlan kontrol
        ThumbnailPlan thumbnail = plan.getThumbnailPlan();
        if (thumbnail != null && thumbnail.getId().equals(itemId)) {
            return ImageGenerationItemType.THUMBNAIL_IMAGE;
        }

        throw new BusinessException(String.format(
                "Belirtilen item bu plana ait değil: itemId=%s, planId=%s", itemId, plan.getId()));
    }

    /**
     * Idempotency key belirler. Client sağladıysa onu kullanır;
     * yoksa deterministik key üretir.
     */
    private String resolveIdempotencyKey(StartImageGenerationRequest request,
                                          VisualAssetPlan plan,
                                          UUID itemId,
                                          ImageGenerationItemType itemType) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            return request.getIdempotencyKey();
        }
        // Deterministik key: planId + itemId + itemType
        return String.format("fp007:%s:%s:%s", plan.getId(), itemId, itemType.name());
    }

    /**
     * Status string → enum. Geçersiz status için safe hata.
     */
    private ImageGenerationRequestStatus parseStatus(String status) {
        try {
            return ImageGenerationRequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz status değeri: " + status);
        }
    }
}
