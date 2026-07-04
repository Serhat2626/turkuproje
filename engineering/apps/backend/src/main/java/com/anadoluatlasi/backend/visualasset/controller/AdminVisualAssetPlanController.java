package com.anadoluatlasi.backend.visualasset.controller;

import com.anadoluatlasi.backend.application.visualasset.*;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
import com.anadoluatlasi.backend.visualasset.controller.dto.*;
import com.anadoluatlasi.backend.visualasset.entity.SceneVisualPlan;
import com.anadoluatlasi.backend.visualasset.entity.ThumbnailPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FP-006: Admin Visual Asset Plan Workflow REST controller.
 *
 * <p>Thin controller — sadece service çağırır ve DTO dönüşümü yapar.
 * İş kuralı, repository veya entity logic içermez.</p>
 *
 * <p>Internal AI contract DTO'ları (VisualAssetPlanAgentRequest/Response)
 * bu controller üzerinden hiçbir şekilde expose edilmez.</p>
 *
 * <p>Generic status update endpoint yoktur.
 * Approve/reject yalnızca explicit action endpoint'leri ile yapılır.</p>
 *
 * <p>Image generation, media creation, publishing, upload endpoint yoktur.
 * Bu controller yalnızca prompt planlama metni üzerinde çalışır.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Visual Asset Plan Workflow", description = "Görsel varlık plan üretim ve review aksiyonları (Admin)")
public class AdminVisualAssetPlanController {

    private static final String ADMIN_PLACEHOLDER = "admin-system";

    private final VisualAssetPlanGenerationService generationService;
    private final VisualAssetPlanReviewService reviewService;
    private final VisualAssetPlanQueryService queryService;

    // ── POST /api/admin/storyboard-drafts/{storyboardDraftId}/visual-asset-plans/generate ──

    @PostMapping("/storyboard-drafts/{storyboardDraftId}/visual-asset-plans/generate")
    @Operation(summary = "Görsel varlık planı üret")
    public ResponseEntity<ApiResponse<VisualAssetPlanGenerateResponse>> generate(
            @PathVariable UUID storyboardDraftId) {

        VisualAssetPlanGenerationResult result = generationService
                .generateVisualAssetPlan(storyboardDraftId, ADMIN_PLACEHOLDER);

        VisualAssetPlanGenerateResponse response = VisualAssetPlanGenerateResponse.builder()
                .success(result.isSuccess())
                .contentProjectId(result.getContentProjectId())
                .storyboardDraftId(result.getStoryboardDraftId())
                .visualAssetPlanId(result.getVisualAssetPlanId())
                .version(result.getVersion())
                .status(result.getStatus())
                .planTitle(result.getPlanTitle())
                .scenePlanCount(result.getScenePlanCount())
                .hasThumbnailPlan(result.getHasThumbnailPlan())
                .message(result.getMessage())
                .errorCode(result.getErrorCode())
                .build();

        HttpStatus httpStatus = result.isSuccess() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).body(ApiResponse.success(response));
    }

    // ── GET /api/admin/storyboard-drafts/{storyboardDraftId}/visual-asset-plans ──

    @GetMapping("/storyboard-drafts/{storyboardDraftId}/visual-asset-plans")
    @Operation(summary = "Storyboard'a ait görsel planları listele")
    public ResponseEntity<ApiResponse<List<VisualAssetPlanResponse>>> listByStoryboard(
            @PathVariable UUID storyboardDraftId) {

        List<VisualAssetPlanResponse> responses = queryService
                .listByStoryboardDraft(storyboardDraftId)
                .stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ── GET /api/admin/content-projects/{contentProjectId}/visual-asset-plans ──

    @GetMapping("/content-projects/{contentProjectId}/visual-asset-plans")
    @Operation(summary = "Projeye ait görsel planları listele")
    public ResponseEntity<ApiResponse<List<VisualAssetPlanResponse>>> listByContentProject(
            @PathVariable UUID contentProjectId) {

        List<VisualAssetPlanResponse> responses = queryService
                .listByContentProject(contentProjectId)
                .stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ── GET /api/admin/visual-asset-plans/{planId} ──────────────────────

    @GetMapping("/visual-asset-plans/{planId}")
    @Operation(summary = "Görsel varlık planı detayı")
    public ResponseEntity<ApiResponse<VisualAssetPlanResponse>> getDetail(
            @PathVariable UUID planId) {

        VisualAssetPlan plan = queryService.getDetail(planId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(plan)));
    }

    // ── PATCH /api/admin/visual-asset-plans/{planId} ────────────────────

    @PatchMapping("/visual-asset-plans/{planId}")
    @Operation(summary = "Görsel varlık planını düzenle")
    public ResponseEntity<ApiResponse<VisualAssetPlanResponse>> editPlan(
            @PathVariable UUID planId,
            @Valid @RequestBody VisualAssetPlanEditRequest request) {

        UpdateVisualAssetPlanCommand command = toEditCommand(request);
        reviewService.updatePlan(planId, command, ADMIN_PLACEHOLDER);

        // Reload to get updated state with children
        VisualAssetPlan updated = queryService.getDetail(planId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(updated)));
    }

    // ── PATCH /api/admin/visual-asset-plans/{planId}/scenes/{sceneNumber} ──

    @PatchMapping("/visual-asset-plans/{planId}/scenes/{sceneNumber}")
    @Operation(summary = "Sahne görsel planını düzenle")
    public ResponseEntity<ApiResponse<VisualAssetPlanResponse>> editScene(
            @PathVariable UUID planId,
            @PathVariable int sceneNumber,
            @Valid @RequestBody SceneVisualPlanEditRequest request) {

        UpdateSceneVisualPlanCommand command = toSceneEditCommand(request);
        reviewService.updateScenePlan(planId, sceneNumber, command, ADMIN_PLACEHOLDER);

        // Reload to get updated state
        VisualAssetPlan updated = queryService.getDetail(planId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(updated)));
    }

    // ── PATCH /api/admin/visual-asset-plans/{planId}/thumbnail ──────────

    @PatchMapping("/visual-asset-plans/{planId}/thumbnail")
    @Operation(summary = "Thumbnail planını düzenle")
    public ResponseEntity<ApiResponse<VisualAssetPlanResponse>> editThumbnail(
            @PathVariable UUID planId,
            @Valid @RequestBody ThumbnailPlanEditRequest request) {

        UpdateThumbnailPlanCommand command = toThumbnailEditCommand(request);
        reviewService.updateThumbnailPlan(planId, command, ADMIN_PLACEHOLDER);

        // Reload to get updated state
        VisualAssetPlan updated = queryService.getDetail(planId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(updated)));
    }

    // ── POST /api/admin/visual-asset-plans/{planId}/approve ─────────────

    @PostMapping("/visual-asset-plans/{planId}/approve")
    @Operation(summary = "Görsel varlık planını onayla")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> approve(
            @PathVariable UUID planId) {

        VisualAssetPlanReviewResult result = reviewService.approve(planId, ADMIN_PLACEHOLDER);

        return ResponseEntity.ok(ApiResponse.success(toActionResponse(result, "Görsel varlık planı onaylandı")));
    }

    // ── POST /api/admin/visual-asset-plans/{planId}/reject ──────────────

    @PostMapping("/visual-asset-plans/{planId}/reject")
    @Operation(summary = "Görsel varlık planını reddet")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> reject(
            @PathVariable UUID planId,
            @Valid @RequestBody VisualAssetPlanRejectRequest request) {

        VisualAssetPlanReviewResult result = reviewService
                .reject(planId, ADMIN_PLACEHOLDER, request.getReason());

        return ResponseEntity.ok(ApiResponse.success(toActionResponse(result, "Görsel varlık planı reddedildi")));
    }

    // ── Private Mapping Helpers ─────────────────────────────────────────

    private VisualAssetPlanResponse toDetailResponse(VisualAssetPlan plan) {
        return VisualAssetPlanResponse.builder()
                .id(plan.getId())
                .sourceStoryboardDraftId(plan.getSourceStoryboardDraft().getId())
                .contentProjectId(plan.getContentProject().getId())
                .version(plan.getVersion())
                .status(plan.getStatus().name())
                .title(plan.getTitle())
                .summary(plan.getSummary())
                .overallVisualStyle(plan.getOverallVisualStyle())
                .aspectRatioStrategy(plan.getAspectRatioStrategy())
                .culturalAuthenticityGuidance(plan.getCulturalAuthenticityGuidance())
                .characterContinuityGuidance(plan.getCharacterContinuityGuidance())
                .locationContinuityGuidance(plan.getLocationContinuityGuidance())
                .colorPaletteGuidance(plan.getColorPaletteGuidance())
                .reviewNotes(plan.getReviewNotes())
                .generatedBy(plan.getGeneratedBy() != null ? plan.getGeneratedBy().name() : null)
                .providerType(plan.getProviderType())
                .modelName(plan.getModelName())
                .approvedAt(plan.getApprovedAt())
                .approvedBy(plan.getApprovedBy())
                .rejectedAt(plan.getRejectedAt())
                .rejectedBy(plan.getRejectedBy())
                .rejectionReason(plan.getRejectionReason())
                .editedAt(plan.getEditedAt())
                .editedBy(plan.getEditedBy())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .scenePlans(plan.getScenePlans() != null
                        ? plan.getScenePlans().stream()
                            .map(this::toSceneResponse)
                            .collect(Collectors.toList())
                        : null)
                .thumbnailPlan(plan.getThumbnailPlan() != null
                        ? toThumbnailResponse(plan.getThumbnailPlan())
                        : null)
                .build();
    }

    private SceneVisualPlanResponse toSceneResponse(SceneVisualPlan scene) {
        return SceneVisualPlanResponse.builder()
                .id(scene.getId())
                .sourceStoryboardSceneId(scene.getSourceStoryboardScene() != null
                        ? scene.getSourceStoryboardScene().getId() : null)
                .sceneNumber(scene.getSceneNumber())
                .sourceBeatType(scene.getSourceBeatType() != null ? scene.getSourceBeatType().name() : null)
                .sceneTitle(scene.getSceneTitle())
                .turkishVisualContext(scene.getTurkishVisualContext())
                .englishImagePrompt(scene.getEnglishImagePrompt())
                .negativePrompt(scene.getNegativePrompt())
                .styleDirective(scene.getStyleDirective())
                .aspectRatio(scene.getAspectRatio())
                .compositionNotes(scene.getCompositionNotes())
                .cameraFraming(scene.getCameraFraming())
                .lightingNotes(scene.getLightingNotes())
                .colorPalette(scene.getColorPalette())
                .mood(scene.getMood())
                .location(scene.getLocation())
                .characters(scene.getCharacters())
                .culturalDetails(scene.getCulturalDetails())
                .textOverlayGuidance(scene.getTextOverlayGuidance())
                .imageGenerationToolNotes(scene.getImageGenerationToolNotes())
                .safetyNotes(scene.getSafetyNotes())
                .createdAt(scene.getCreatedAt())
                .updatedAt(scene.getUpdatedAt())
                .build();
    }

    private ThumbnailPlanResponse toThumbnailResponse(ThumbnailPlan thumb) {
        return ThumbnailPlanResponse.builder()
                .id(thumb.getId())
                .sourceStoryboardSceneId(thumb.getSourceStoryboardScene() != null
                        ? thumb.getSourceStoryboardScene().getId() : null)
                .sourceSceneNumber(thumb.getSourceSceneNumber())
                .thumbnailPrompt(thumb.getThumbnailPrompt())
                .negativePrompt(thumb.getNegativePrompt())
                .textOverlayGuidance(thumb.getTextOverlayGuidance())
                .compositionNotes(thumb.getCompositionNotes())
                .emotionalHook(thumb.getEmotionalHook())
                .aspectRatio(thumb.getAspectRatio())
                .styleDirective(thumb.getStyleDirective())
                .colorPalette(thumb.getColorPalette())
                .createdAt(thumb.getCreatedAt())
                .updatedAt(thumb.getUpdatedAt())
                .build();
    }

    private UpdateVisualAssetPlanCommand toEditCommand(VisualAssetPlanEditRequest request) {
        return UpdateVisualAssetPlanCommand.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .overallVisualStyle(request.getOverallVisualStyle())
                .aspectRatioStrategy(request.getAspectRatioStrategy())
                .culturalAuthenticityGuidance(request.getCulturalAuthenticityGuidance())
                .characterContinuityGuidance(request.getCharacterContinuityGuidance())
                .locationContinuityGuidance(request.getLocationContinuityGuidance())
                .colorPaletteGuidance(request.getColorPaletteGuidance())
                .reviewNotes(request.getReviewNotes())
                .build();
    }

    private UpdateSceneVisualPlanCommand toSceneEditCommand(SceneVisualPlanEditRequest request) {
        return UpdateSceneVisualPlanCommand.builder()
                .englishImagePrompt(request.getEnglishImagePrompt())
                .turkishVisualContext(request.getTurkishVisualContext())
                .negativePrompt(request.getNegativePrompt())
                .styleDirective(request.getStyleDirective())
                .aspectRatio(request.getAspectRatio())
                .compositionNotes(request.getCompositionNotes())
                .cameraFraming(request.getCameraFraming())
                .lightingNotes(request.getLightingNotes())
                .colorPalette(request.getColorPalette())
                .mood(request.getMood())
                .location(request.getLocation())
                .characters(request.getCharacters())
                .culturalDetails(request.getCulturalDetails())
                .textOverlayGuidance(request.getTextOverlayGuidance())
                .imageGenerationToolNotes(request.getImageGenerationToolNotes())
                .safetyNotes(request.getSafetyNotes())
                .build();
    }

    private UpdateThumbnailPlanCommand toThumbnailEditCommand(ThumbnailPlanEditRequest request) {
        return UpdateThumbnailPlanCommand.builder()
                .thumbnailPrompt(request.getThumbnailPrompt())
                .negativePrompt(request.getNegativePrompt())
                .textOverlayGuidance(request.getTextOverlayGuidance())
                .compositionNotes(request.getCompositionNotes())
                .emotionalHook(request.getEmotionalHook())
                .aspectRatio(request.getAspectRatio())
                .styleDirective(request.getStyleDirective())
                .colorPalette(request.getColorPalette())
                .build();
    }

    private ActionStatusResponse toActionResponse(VisualAssetPlanReviewResult result, String message) {
        return ActionStatusResponse.builder()
                .id(result.getVisualAssetPlanId())
                .status(result.getStatus())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
