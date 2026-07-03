package com.anadoluatlasi.backend.platformpackage.controller;

import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageDraftEditCommand;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageGenerationResult;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageGenerationService;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageQueryService;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageReviewResult;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageReviewService;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageSceneUsageEditCommand;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageDraftEditRequest;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageDraftResponse;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageDraftSummaryResponse;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageGenerateResponse;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageRejectRequest;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageSceneUsageEditRequest;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageSceneUsageResponse;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageSceneUsage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FP-004: Admin Platform Package Workflow REST controller.
 *
 * <p>Thin controller — sadece service çağırır ve DTO dönüşümü yapar.
 * İş kuralı, repository veya entity logic içermez.</p>
 *
 * <p>Internal AI contract DTO'ları (PlatformPackageAgentRequest/Response/PlatformPackageOutput)
 * bu controller üzerinden hiçbir şekilde expose edilmez.</p>
 *
 * <p>Generic status update endpoint yoktur.
 * Approve/reject yalnızca explicit action endpoint'leri ile yapılır.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Platform Package Workflow", description = "Platform paketi üretim ve review aksiyonları (Admin)")
public class AdminPlatformPackageController {

    private static final String ADMIN_PLACEHOLDER = "admin-system";

    private final PlatformPackageGenerationService generationService;
    private final PlatformPackageQueryService queryService;
    private final PlatformPackageReviewService reviewService;

    // ── POST /api/admin/storyboards/{storyboardId}/platform-packages/generate ──

    @PostMapping("/storyboards/{storyboardId}/platform-packages/generate")
    @Operation(summary = "Onaylanmış storyboard'dan platform paketleri üret")
    public ResponseEntity<ApiResponse<PlatformPackageGenerateResponse>> generate(
            @PathVariable UUID storyboardId) {

        PlatformPackageGenerationResult result = generationService
                .generatePlatformPackages(storyboardId, ADMIN_PLACEHOLDER);

        PlatformPackageGenerateResponse response = PlatformPackageGenerateResponse.builder()
                .success(result.isSuccess())
                .contentProjectId(result.getContentProjectId())
                .sourceStoryboardDraftId(result.getSourceStoryboardDraftId())
                .generatedPackageIds(result.getGeneratedPackageIds())
                .packageCount(result.getPackageCount())
                .message(result.getMessage())
                .errorCode(result.getErrorCode())
                .build();

        HttpStatus httpStatus = result.isSuccess() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).body(ApiResponse.success(response));
    }

    // ── GET /api/admin/content-projects/{projectId}/platform-packages ──

    @GetMapping("/content-projects/{projectId}/platform-packages")
    @Operation(summary = "Projeye ait platform paketlerini listele")
    public ResponseEntity<ApiResponse<List<PlatformPackageDraftSummaryResponse>>> listByProject(
            @PathVariable UUID projectId) {

        List<PlatformPackageDraftSummaryResponse> summaries = queryService
                .listByContentProject(projectId)
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    // ── GET /api/admin/storyboards/{storyboardId}/platform-packages ──

    @GetMapping("/storyboards/{storyboardId}/platform-packages")
    @Operation(summary = "Storyboard'a ait platform paketlerini listele")
    public ResponseEntity<ApiResponse<List<PlatformPackageDraftSummaryResponse>>> listByStoryboard(
            @PathVariable UUID storyboardId) {

        List<PlatformPackageDraftSummaryResponse> summaries = queryService
                .listBySourceStoryboard(storyboardId)
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    // ── GET /api/admin/platform-packages/{packageId} ──

    @GetMapping("/platform-packages/{packageId}")
    @Operation(summary = "Platform paketi detayı")
    public ResponseEntity<ApiResponse<PlatformPackageDraftResponse>> getDetail(
            @PathVariable UUID packageId) {

        PlatformPackageDraft draft = queryService.getDetail(packageId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(draft)));
    }

    // ── PUT /api/admin/platform-packages/{packageId} ──

    @PutMapping("/platform-packages/{packageId}")
    @Operation(summary = "Platform paketini düzenle")
    public ResponseEntity<ApiResponse<PlatformPackageDraftResponse>> edit(
            @PathVariable UUID packageId,
            @Valid @RequestBody PlatformPackageDraftEditRequest request) {

        PlatformPackageDraftEditCommand command = toEditCommand(request);
        reviewService.editPlatformPackageDraft(packageId, command, ADMIN_PLACEHOLDER);

        // Reload to get updated state with scene usages
        PlatformPackageDraft updated = queryService.getDetail(packageId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(updated)));
    }

    // ── POST /api/admin/platform-packages/{packageId}/approve ──

    @PostMapping("/platform-packages/{packageId}/approve")
    @Operation(summary = "Platform paketini onayla")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> approve(
            @PathVariable UUID packageId) {

        PlatformPackageReviewResult result = reviewService
                .approvePlatformPackageDraft(packageId, ADMIN_PLACEHOLDER);

        return ResponseEntity.ok(ApiResponse.success(toActionResponse(result, "Platform paketi onaylandı")));
    }

    // ── POST /api/admin/platform-packages/{packageId}/reject ──

    @PostMapping("/platform-packages/{packageId}/reject")
    @Operation(summary = "Platform paketini reddet")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> reject(
            @PathVariable UUID packageId,
            @Valid @RequestBody PlatformPackageRejectRequest request) {

        PlatformPackageReviewResult result = reviewService
                .rejectPlatformPackageDraft(packageId, ADMIN_PLACEHOLDER, request.getReason());

        return ResponseEntity.ok(ApiResponse.success(toActionResponse(result, "Platform paketi reddedildi")));
    }

    // ── Private Mapping Helpers ─────────────────────────────────────

    private PlatformPackageDraftResponse toDetailResponse(PlatformPackageDraft draft) {
        return PlatformPackageDraftResponse.builder()
                .id(draft.getId())
                .contentProjectId(draft.getContentProject().getId())
                .sourceStoryboardDraftId(draft.getSourceStoryboardDraft().getId())
                .packageType(draft.getPackageType().name())
                .status(draft.getStatus().name())
                .version(draft.getVersion())
                .title(draft.getTitle())
                .captionOrDescription(draft.getCaptionOrDescription())
                .hashtagsJson(draft.getHashtagsJson())
                .callToAction(draft.getCallToAction())
                .targetDurationSeconds(draft.getTargetDurationSeconds())
                .aspectRatio(draft.getAspectRatio())
                .contentStructureNotes(draft.getContentStructureNotes())
                .sceneUsageNotes(draft.getSceneUsageNotes())
                .reviewNotes(draft.getReviewNotes())
                .videoTitle(draft.getVideoTitle())
                .longDescription(draft.getLongDescription())
                .chapterOutlineJson(draft.getChapterOutlineJson())
                .pinnedCommentSuggestion(draft.getPinnedCommentSuggestion())
                .thumbnailTextSuggestion(draft.getThumbnailTextSuggestion())
                .tagsJson(draft.getTagsJson())
                .shortHook(draft.getShortHook())
                .shortCaption(draft.getShortCaption())
                .overlayTextSuggestionsJson(draft.getOverlayTextSuggestionsJson())
                .shortHashtagSetJson(draft.getShortHashtagSetJson())
                .shortCta(draft.getShortCta())
                .durationGuidance(draft.getDurationGuidance())
                .providerType(draft.getProviderType())
                .modelName(draft.getModelName())
                .generatedBy(draft.getGeneratedBy() != null ? draft.getGeneratedBy().name() : null)
                .editedAt(draft.getEditedAt())
                .editedBy(draft.getEditedBy())
                .approvedAt(draft.getApprovedAt())
                .approvedBy(draft.getApprovedBy())
                .rejectedAt(draft.getRejectedAt())
                .rejectedBy(draft.getRejectedBy())
                .rejectionReason(draft.getRejectionReason())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .sceneUsages(draft.getSceneUsages() != null
                        ? draft.getSceneUsages().stream().map(this::toSceneUsageResponse).collect(Collectors.toList())
                        : null)
                .build();
    }

    private PlatformPackageSceneUsageResponse toSceneUsageResponse(PlatformPackageSceneUsage usage) {
        return PlatformPackageSceneUsageResponse.builder()
                .id(usage.getId())
                .sceneNumber(usage.getSceneNumber())
                .sourceStoryboardSceneId(usage.getSourceStoryboardScene() != null
                        ? usage.getSourceStoryboardScene().getId() : null)
                .usageType(usage.getUsageType())
                .included(usage.getIncluded())
                .adaptationNote(usage.getAdaptationNote())
                .createdAt(usage.getCreatedAt())
                .updatedAt(usage.getUpdatedAt())
                .build();
    }

    private PlatformPackageDraftSummaryResponse toSummaryResponse(PlatformPackageDraft draft) {
        return PlatformPackageDraftSummaryResponse.builder()
                .id(draft.getId())
                .contentProjectId(draft.getContentProject().getId())
                .sourceStoryboardDraftId(draft.getSourceStoryboardDraft().getId())
                .packageType(draft.getPackageType().name())
                .status(draft.getStatus().name())
                .version(draft.getVersion())
                .title(draft.getTitle())
                .captionOrDescription(draft.getCaptionOrDescription())
                .generatedBy(draft.getGeneratedBy() != null ? draft.getGeneratedBy().name() : null)
                .providerType(draft.getProviderType())
                .targetDurationSeconds(draft.getTargetDurationSeconds())
                .aspectRatio(draft.getAspectRatio())
                .createdAt(draft.getCreatedAt())
                .editedAt(draft.getEditedAt())
                .approvedAt(draft.getApprovedAt())
                .rejectedAt(draft.getRejectedAt())
                .build();
    }

    private PlatformPackageDraftEditCommand toEditCommand(PlatformPackageDraftEditRequest request) {
        List<PlatformPackageSceneUsageEditCommand> usageCommands = null;
        if (request.getSceneUsages() != null) {
            usageCommands = request.getSceneUsages().stream()
                    .map(s -> PlatformPackageSceneUsageEditCommand.builder()
                            .sceneNumber(s.getSceneNumber())
                            .usageType(s.getUsageType())
                            .included(s.getIncluded())
                            .adaptationNote(s.getAdaptationNote())
                            .build())
                    .collect(Collectors.toList());
        }

        return PlatformPackageDraftEditCommand.builder()
                .title(request.getTitle())
                .captionOrDescription(request.getCaptionOrDescription())
                .hashtagsJson(request.getHashtagsJson())
                .callToAction(request.getCallToAction())
                .targetDurationSeconds(request.getTargetDurationSeconds())
                .aspectRatio(request.getAspectRatio())
                .contentStructureNotes(request.getContentStructureNotes())
                .sceneUsageNotes(request.getSceneUsageNotes())
                .reviewNotes(request.getReviewNotes())
                .videoTitle(request.getVideoTitle())
                .longDescription(request.getLongDescription())
                .chapterOutlineJson(request.getChapterOutlineJson())
                .pinnedCommentSuggestion(request.getPinnedCommentSuggestion())
                .thumbnailTextSuggestion(request.getThumbnailTextSuggestion())
                .shortHook(request.getShortHook())
                .shortCaption(request.getShortCaption())
                .overlayTextSuggestionsJson(request.getOverlayTextSuggestionsJson())
                .shortHashtagSetJson(request.getShortHashtagSetJson())
                .shortCta(request.getShortCta())
                .durationGuidance(request.getDurationGuidance())
                .sceneUsages(usageCommands)
                .build();
    }

    private ActionStatusResponse toActionResponse(PlatformPackageReviewResult result, String message) {
        return ActionStatusResponse.builder()
                .id(result.getPackageDraftId())
                .status(result.getStatus())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
