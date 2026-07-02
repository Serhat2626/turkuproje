package com.anadoluatlasi.backend.scenario.controller;

import com.anadoluatlasi.backend.application.scenario.ScenarioBeatEditCommand;
import com.anadoluatlasi.backend.application.scenario.ScenarioDraftEditCommand;
import com.anadoluatlasi.backend.application.scenario.ScenarioGenerationResult;
import com.anadoluatlasi.backend.application.scenario.ScenarioGenerationService;
import com.anadoluatlasi.backend.application.scenario.ScenarioQueryService;
import com.anadoluatlasi.backend.application.scenario.ScenarioReviewResult;
import com.anadoluatlasi.backend.application.scenario.ScenarioReviewService;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioBeatResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioDraftEditRequest;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioDraftResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioDraftSummaryResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioGenerateResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioRejectRequest;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeat;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
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
 * FP-002: Admin Scenario Workflow REST controller.
 *
 * <p>Thin controller — sadece service çağırır ve DTO dönüşümü yapar.
 * İş kuralı, repository veya entity logic içermez.</p>
 *
 * <p>Internal AI contract DTO'ları (ScenarioAgentRequest/Response/ScenarioBeatResult)
 * bu controller üzerinden hiçbir şekilde expose edilmez.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Scenario Workflow", description = "Senaryo üretim ve review aksiyonları (Admin)")
public class AdminScenarioController {

    private static final String ADMIN_PLACEHOLDER = "admin-system";

    private final ScenarioGenerationService scenarioGenerationService;
    private final ScenarioReviewService scenarioReviewService;
    private final ScenarioQueryService scenarioQueryService;

    // ── POST /api/admin/content-projects/{projectId}/scenarios/generate ──

    @PostMapping("/content-projects/{projectId}/scenarios/generate")
    @Operation(summary = "Senaryo candidate üret")
    public ResponseEntity<ApiResponse<ScenarioGenerateResponse>> generate(
            @PathVariable UUID projectId) {

        ScenarioGenerationResult result = scenarioGenerationService
                .generateScenarioForContentProject(projectId, ADMIN_PLACEHOLDER);

        ScenarioGenerateResponse response = ScenarioGenerateResponse.builder()
                .success(result.isSuccess())
                .contentProjectId(result.getContentProjectId())
                .scenarioDraftId(result.getScenarioDraftId())
                .version(result.getVersion())
                .status(result.getStatus())
                .scenarioTitle(result.getScenarioTitle())
                .message(result.getMessage())
                .errorCode(result.getErrorCode())
                .build();

        HttpStatus httpStatus = result.isSuccess() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).body(ApiResponse.success(response));
    }

    // ── GET /api/admin/content-projects/{projectId}/scenarios ────────────

    @GetMapping("/content-projects/{projectId}/scenarios")
    @Operation(summary = "Proje senaryolarını listele")
    public ResponseEntity<ApiResponse<List<ScenarioDraftSummaryResponse>>> list(
            @PathVariable UUID projectId) {

        List<ScenarioDraftSummaryResponse> summaries = scenarioQueryService
                .listByContentProject(projectId)
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    // ── GET /api/admin/scenarios/{scenarioId} ────────────────────────────

    @GetMapping("/scenarios/{scenarioId}")
    @Operation(summary = "Senaryo detayı")
    public ResponseEntity<ApiResponse<ScenarioDraftResponse>> getDetail(
            @PathVariable UUID scenarioId) {

        ScenarioDraft draft = scenarioQueryService.getDetail(scenarioId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(draft)));
    }

    // ── PUT /api/admin/scenarios/{scenarioId} ────────────────────────────

    @PutMapping("/scenarios/{scenarioId}")
    @Operation(summary = "Senaryoyu düzenle")
    public ResponseEntity<ApiResponse<ScenarioDraftResponse>> edit(
            @PathVariable UUID scenarioId,
            @Valid @RequestBody ScenarioDraftEditRequest request) {

        ScenarioDraftEditCommand command = toEditCommand(request);
        scenarioReviewService.editScenarioDraft(scenarioId, command, ADMIN_PLACEHOLDER);

        // Reload to get updated state with beats
        ScenarioDraft updated = scenarioQueryService.getDetail(scenarioId);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(updated)));
    }

    // ── POST /api/admin/scenarios/{scenarioId}/approve ───────────────────

    @PostMapping("/scenarios/{scenarioId}/approve")
    @Operation(summary = "Senaryoyu onayla")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> approve(
            @PathVariable UUID scenarioId) {

        ScenarioReviewResult result = scenarioReviewService
                .approveScenarioDraft(scenarioId, ADMIN_PLACEHOLDER);

        return ResponseEntity.ok(ApiResponse.success(toActionResponse(result, "Senaryo onaylandı")));
    }

    // ── POST /api/admin/scenarios/{scenarioId}/reject ────────────────────

    @PostMapping("/scenarios/{scenarioId}/reject")
    @Operation(summary = "Senaryoyu reddet")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> reject(
            @PathVariable UUID scenarioId,
            @Valid @RequestBody ScenarioRejectRequest request) {

        ScenarioReviewResult result = scenarioReviewService
                .rejectScenarioDraft(scenarioId, ADMIN_PLACEHOLDER, request.getReason());

        return ResponseEntity.ok(ApiResponse.success(toActionResponse(result, "Senaryo reddedildi")));
    }

    // ── Private Mapping Helpers ──────────────────────────────────────────

    private ScenarioDraftResponse toDetailResponse(ScenarioDraft draft) {
        return ScenarioDraftResponse.builder()
                .id(draft.getId())
                .contentProjectId(draft.getContentProject().getId())
                .sourceDraftId(draft.getSourceDraft().getId())
                .version(draft.getVersion())
                .status(draft.getStatus().name())
                .title(draft.getTitle())
                .summary(draft.getSummary())
                .targetAudience(draft.getTargetAudience())
                .culturalObjective(draft.getCulturalObjective())
                .tone(draft.getTone())
                .estimatedDurationSeconds(draft.getEstimatedDurationSeconds())
                .setting(draft.getSetting())
                .characters(draft.getCharacters())
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
                .beats(draft.getBeats() != null
                        ? draft.getBeats().stream().map(this::toBeatResponse).collect(Collectors.toList())
                        : null)
                .build();
    }

    private ScenarioBeatResponse toBeatResponse(ScenarioBeat beat) {
        return ScenarioBeatResponse.builder()
                .type(beat.getType() != null ? beat.getType().name() : null)
                .label(beat.getLabel())
                .body(beat.getBody())
                .orderIndex(beat.getOrderIndex())
                .estimatedDurationSeconds(beat.getEstimatedDurationSeconds())
                .build();
    }

    private ScenarioDraftSummaryResponse toSummaryResponse(ScenarioDraft draft) {
        return ScenarioDraftSummaryResponse.builder()
                .id(draft.getId())
                .version(draft.getVersion())
                .status(draft.getStatus().name())
                .title(draft.getTitle())
                .generatedBy(draft.getGeneratedBy() != null ? draft.getGeneratedBy().name() : null)
                .providerType(draft.getProviderType())
                .createdAt(draft.getCreatedAt())
                .approvedAt(draft.getApprovedAt())
                .rejectedAt(draft.getRejectedAt())
                .editedAt(draft.getEditedAt())
                .build();
    }

    private ScenarioDraftEditCommand toEditCommand(ScenarioDraftEditRequest request) {
        List<ScenarioBeatEditCommand> beatCommands = null;
        if (request.getBeats() != null) {
            beatCommands = request.getBeats().stream()
                    .map(b -> ScenarioBeatEditCommand.builder()
                            .type(ScenarioBeatType.valueOf(b.getType()))
                            .label(b.getLabel())
                            .body(b.getBody())
                            .orderIndex(b.getOrderIndex())
                            .estimatedDurationSeconds(b.getEstimatedDurationSeconds())
                            .build())
                    .collect(Collectors.toList());
        }

        return ScenarioDraftEditCommand.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .targetAudience(request.getTargetAudience())
                .culturalObjective(request.getCulturalObjective())
                .tone(request.getTone())
                .estimatedDurationSeconds(request.getEstimatedDurationSeconds())
                .setting(request.getSetting())
                .characters(request.getCharacters())
                .reviewNotes(request.getReviewNotes())
                .beats(beatCommands)
                .build();
    }

    private ActionStatusResponse toActionResponse(ScenarioReviewResult result, String message) {
        return ActionStatusResponse.builder()
                .id(result.getScenarioDraftId())
                .status(result.getStatus())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
