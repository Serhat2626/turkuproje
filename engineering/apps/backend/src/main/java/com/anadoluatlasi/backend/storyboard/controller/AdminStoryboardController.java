package com.anadoluatlasi.backend.storyboard.controller;

import com.anadoluatlasi.backend.application.storyboard.StoryboardDraftEditCommand;
import com.anadoluatlasi.backend.application.storyboard.StoryboardGenerationResult;
import com.anadoluatlasi.backend.application.storyboard.StoryboardGenerationService;
import com.anadoluatlasi.backend.application.storyboard.StoryboardQueryService;
import com.anadoluatlasi.backend.application.storyboard.StoryboardReviewResult;
import com.anadoluatlasi.backend.application.storyboard.StoryboardReviewService;
import com.anadoluatlasi.backend.application.storyboard.StoryboardSceneEditCommand;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardDraftEditRequest;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardDraftResponse;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardDraftSummaryResponse;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardGenerateResponse;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardRejectRequest;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardSceneEditRequest;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardSceneResponse;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
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
 * FP-003: Admin Storyboard Workflow REST controller.
 *
 * <p>Thin controller — sadece service çağırır ve DTO dönüşümü yapar.
 * İş kuralı, repository veya entity logic içermez.</p>
 *
 * <p>Internal AI contract DTO'ları (StoryboardAgentRequest/Response/StoryboardSceneOutput)
 * bu controller üzerinden hiçbir şekilde expose edilmez.</p>
 *
 * <p>Generic status update endpoint yoktur.
 * Approve/reject yalnızca explicit action endpoint'leri ile yapılır.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Storyboard Workflow", description = "Storyboard üretim ve review aksiyonları (Admin)")
public class AdminStoryboardController {

    private static final String ADMIN_PLACEHOLDER = "admin-system";

    private final StoryboardGenerationService storyboardGenerationService;
    private final StoryboardReviewService storyboardReviewService;
    private final StoryboardQueryService storyboardQueryService;

    // ── POST /api/admin/scenarios/{scenarioId}/storyboards/generate ──

    @PostMapping("/scenarios/{scenarioId}/storyboards/generate")
    @Operation(summary = "Storyboard candidate üret")
    public ResponseEntity<ApiResponse<StoryboardGenerateResponse>> generate(
            @PathVariable UUID scenarioId) {

        StoryboardGenerationResult result = storyboardGenerationService
                .generateStoryboardForScenario(scenarioId, ADMIN_PLACEHOLDER);

        StoryboardGenerateResponse response = StoryboardGenerateResponse.builder()
                .success(result.isSuccess())
                .contentProjectId(result.getContentProjectId())
                .storyboardDraftId(result.getStoryboardDraftId())
                .version(result.getVersion())
                .status(result.getStatus())
                .storyboardTitle(result.getStoryboardTitle())
                .sceneCount(result.getSceneCount())
                .message(result.getMessage())
                .errorCode(result.getErrorCode())
                .build();

        HttpStatus httpStatus = result.isSuccess() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).body(ApiResponse.success(response));
    }

    // ── GET /api/admin/content-projects/{projectId}/storyboards ──────

    @GetMapping("/content-projects/{projectId}/storyboards")
    @Operation(summary = "Proje storyboard'larını listele")
    public ResponseEntity<ApiResponse<List<StoryboardDraftSummaryResponse>>> list(
            @PathVariable UUID projectId) {

        List<StoryboardDraftSummaryResponse> summaries = storyboardQueryService
                .listByContentProject(projectId)
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    // ── GET /api/admin/storyboards/{storyboardId} ───────────────────

    @GetMapping("/storyboards/{storyboardId}")
    @Operation(summary = "Storyboard detayı")
    public ResponseEntity<ApiResponse<StoryboardDraftResponse>> getDetail(
            @PathVariable UUID storyboardId) {

        StoryboardDraft draft = storyboardQueryService.getDetail(storyboardId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(draft)));
    }

    // ── PUT /api/admin/storyboards/{storyboardId} ───────────────────

    @PutMapping("/storyboards/{storyboardId}")
    @Operation(summary = "Storyboard'u düzenle")
    public ResponseEntity<ApiResponse<StoryboardDraftResponse>> edit(
            @PathVariable UUID storyboardId,
            @Valid @RequestBody StoryboardDraftEditRequest request) {

        StoryboardDraftEditCommand command = toEditCommand(request);
        storyboardReviewService.editStoryboardDraft(storyboardId, command, ADMIN_PLACEHOLDER);

        // Reload to get updated state with scenes
        StoryboardDraft updated = storyboardQueryService.getDetail(storyboardId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(updated)));
    }

    // ── POST /api/admin/storyboards/{storyboardId}/approve ──────────

    @PostMapping("/storyboards/{storyboardId}/approve")
    @Operation(summary = "Storyboard'u onayla")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> approve(
            @PathVariable UUID storyboardId) {

        StoryboardReviewResult result = storyboardReviewService
                .approveStoryboardDraft(storyboardId, ADMIN_PLACEHOLDER);

        return ResponseEntity.ok(ApiResponse.success(toActionResponse(result, "Storyboard onaylandı")));
    }

    // ── POST /api/admin/storyboards/{storyboardId}/reject ───────────

    @PostMapping("/storyboards/{storyboardId}/reject")
    @Operation(summary = "Storyboard'u reddet")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> reject(
            @PathVariable UUID storyboardId,
            @Valid @RequestBody StoryboardRejectRequest request) {

        StoryboardReviewResult result = storyboardReviewService
                .rejectStoryboardDraft(storyboardId, ADMIN_PLACEHOLDER, request.getReason());

        return ResponseEntity.ok(ApiResponse.success(toActionResponse(result, "Storyboard reddedildi")));
    }

    // ── Private Mapping Helpers ─────────────────────────────────────

    private StoryboardDraftResponse toDetailResponse(StoryboardDraft draft) {
        return StoryboardDraftResponse.builder()
                .id(draft.getId())
                .contentProjectId(draft.getContentProject().getId())
                .sourceDraftId(draft.getSourceDraft().getId())
                .sourceScenarioDraftId(draft.getSourceScenarioDraft().getId())
                .sourceScenarioVersion(draft.getSourceScenarioVersion())
                .version(draft.getVersion())
                .status(draft.getStatus().name())
                .title(draft.getTitle())
                .summary(draft.getSummary())
                .targetPlatform(draft.getTargetPlatform())
                .aspectRatio(draft.getAspectRatio())
                .estimatedDurationSeconds(draft.getEstimatedDurationSeconds())
                .visualStyleGuide(draft.getVisualStyleGuide())
                .narrationStyle(draft.getNarrationStyle())
                .reviewNotes(draft.getReviewNotes())
                .generatedBy(draft.getGeneratedBy() != null ? draft.getGeneratedBy().name() : null)
                .providerType(draft.getProviderType())
                .modelName(draft.getModelName())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .approvedAt(draft.getApprovedAt())
                .rejectedAt(draft.getRejectedAt())
                .editedAt(draft.getEditedAt())
                .createdBy(draft.getCreatedBy())
                .approvedBy(draft.getApprovedBy())
                .rejectedBy(draft.getRejectedBy())
                .editedBy(draft.getEditedBy())
                .scenes(draft.getScenes() != null
                        ? draft.getScenes().stream().map(this::toSceneResponse).collect(Collectors.toList())
                        : null)
                .build();
    }

    private StoryboardSceneResponse toSceneResponse(StoryboardScene scene) {
        return StoryboardSceneResponse.builder()
                .id(scene.getId())
                .sceneNumber(scene.getSceneNumber())
                .sourceBeatType(scene.getSourceBeatType() != null ? scene.getSourceBeatType().name() : null)
                .sourceBeatOrder(scene.getSourceBeatOrder())
                .sceneTitle(scene.getSceneTitle())
                .visualDescription(scene.getVisualDescription())
                .narrationText(scene.getNarrationText())
                .onScreenText(scene.getOnScreenText())
                .cameraDirection(scene.getCameraDirection())
                .transitionNote(scene.getTransitionNote())
                .mood(scene.getMood())
                .location(scene.getLocation())
                .characters(scene.getCharacters())
                .estimatedDurationSeconds(scene.getEstimatedDurationSeconds())
                .build();
    }

    private StoryboardDraftSummaryResponse toSummaryResponse(StoryboardDraft draft) {
        return StoryboardDraftSummaryResponse.builder()
                .id(draft.getId())
                .version(draft.getVersion())
                .status(draft.getStatus().name())
                .title(draft.getTitle())
                .sceneCount(draft.getScenes() != null ? draft.getScenes().size() : 0)
                .generatedBy(draft.getGeneratedBy() != null ? draft.getGeneratedBy().name() : null)
                .providerType(draft.getProviderType())
                .createdAt(draft.getCreatedAt())
                .approvedAt(draft.getApprovedAt())
                .rejectedAt(draft.getRejectedAt())
                .editedAt(draft.getEditedAt())
                .build();
    }

    private StoryboardDraftEditCommand toEditCommand(StoryboardDraftEditRequest request) {
        List<StoryboardSceneEditCommand> sceneCommands = null;
        if (request.getScenes() != null) {
            sceneCommands = request.getScenes().stream()
                    .map(s -> StoryboardSceneEditCommand.builder()
                            .sceneNumber(s.getSceneNumber())
                            .sceneTitle(s.getSceneTitle())
                            .visualDescription(s.getVisualDescription())
                            .narrationText(s.getNarrationText())
                            .onScreenText(s.getOnScreenText())
                            .cameraDirection(s.getCameraDirection())
                            .transitionNote(s.getTransitionNote())
                            .mood(s.getMood())
                            .location(s.getLocation())
                            .characters(s.getCharacters())
                            .estimatedDurationSeconds(s.getEstimatedDurationSeconds())
                            .build())
                    .collect(Collectors.toList());
        }

        return StoryboardDraftEditCommand.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .targetPlatform(request.getTargetPlatform())
                .aspectRatio(request.getAspectRatio())
                .estimatedDurationSeconds(request.getEstimatedDurationSeconds())
                .visualStyleGuide(request.getVisualStyleGuide())
                .narrationStyle(request.getNarrationStyle())
                .reviewNotes(request.getReviewNotes())
                .scenes(sceneCommands)
                .build();
    }

    private ActionStatusResponse toActionResponse(StoryboardReviewResult result, String message) {
        return ActionStatusResponse.builder()
                .id(result.getStoryboardDraftId())
                .status(result.getStatus())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
