package com.anadoluatlasi.backend.content.controller;

import com.anadoluatlasi.backend.application.ai.AiGenerationResult;
import com.anadoluatlasi.backend.application.ai.AiGenerationService;
import com.anadoluatlasi.backend.application.content.ContentProjectResult;
import com.anadoluatlasi.backend.application.content.ContentProjectService;
import com.anadoluatlasi.backend.application.content.CreateContentProjectCommand;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.content.controller.dto.AiGenerationRequestSummaryResponse;
import com.anadoluatlasi.backend.content.controller.dto.ApproveDraftRequest;
import com.anadoluatlasi.backend.content.controller.dto.CancelContentProjectRequest;
import com.anadoluatlasi.backend.content.controller.dto.ContentProjectResponse;
import com.anadoluatlasi.backend.content.controller.dto.ContentProjectSummaryResponse;
import com.anadoluatlasi.backend.content.controller.dto.CreateContentProjectRequest;
import com.anadoluatlasi.backend.content.controller.dto.GenerateAiDraftRequest;
import com.anadoluatlasi.backend.content.controller.dto.RejectDraftRequest;
import com.anadoluatlasi.backend.content.controller.dto.RequestRevisionRequest;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-001: Admin Content Project REST controller.
 * Yalnızca ContentProjectService ve AiGenerationService çağırır.
 * İş kuralı, repository veya entity kullanmaz.
 */
@RestController
@RequestMapping("/api/admin/content-projects")
@RequiredArgsConstructor
@Tag(name = "Admin Content Projects", description = "İçerik projesi yönetimi ve AI üretim aksiyonları (Admin)")
public class ContentProjectController {

    private final ContentProjectService contentProjectService;
    private final AiGenerationService aiGenerationService;

    // ── POST /api/admin/content-projects ────────────────────

    @PostMapping
    @Operation(summary = "Yeni içerik projesi oluştur")
    public ResponseEntity<ApiResponse<ContentProjectResponse>> create(
            @Valid @RequestBody CreateContentProjectRequest request) {

        CreateContentProjectCommand command = CreateContentProjectCommand.builder()
                .knowledgeItemId(request.getKnowledgeItemId())
                .contentPurpose(request.getContentPurpose())
                .targetAudience(request.getTargetAudience())
                .tone(request.getTone())
                .language(request.getLanguage())
                .formatHint(request.getFormatHint())
                .lengthHint(request.getLengthHint())
                .build();

        ContentProjectResult result = contentProjectService.create(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(result)));
    }

    // ── GET /api/admin/content-projects ─────────────────────

    @GetMapping
    @Operation(summary = "İçerik projelerini listele")
    public ResponseEntity<ApiResponse<List<ContentProjectSummaryResponse>>> list(
            @RequestParam(required = false) UUID knowledgeItemId,
            @RequestParam(required = false) String status) {

        ContentProjectStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            statusFilter = ContentProjectStatus.valueOf(status);
        }

        List<ContentProjectSummaryResponse> summaries = contentProjectService
                .list(knowledgeItemId, statusFilter)
                .stream()
                .map(this::toSummaryResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    // ── GET /api/admin/content-projects/{id} ────────────────

    @GetMapping("/{contentProjectId}")
    @Operation(summary = "İçerik projesi detayı")
    public ResponseEntity<ApiResponse<ContentProjectResponse>> getDetail(
            @PathVariable UUID contentProjectId) {

        ContentProjectResult result = contentProjectService.getDetail(contentProjectId);

        return ResponseEntity.ok(ApiResponse.success(toResponse(result)));
    }

    // ── POST .../generate-ai-draft ──────────────────────────

    @PostMapping("/{contentProjectId}/generate-ai-draft")
    @Operation(summary = "AI draft üretimi başlat")
    public ResponseEntity<ApiResponse<AiGenerationRequestSummaryResponse>> generateAiDraft(
            @PathVariable UUID contentProjectId,
            @RequestBody(required = false) GenerateAiDraftRequest request) {

        String feedbackNote = request != null ? request.getHumanFeedbackNote() : null;
        String feedbackType = request != null ? request.getFeedbackType() : null;

        AiGenerationResult result = aiGenerationService.generateAiDraft(
                contentProjectId, feedbackNote, feedbackType);

        return ResponseEntity.ok(ApiResponse.success(toAiSummary(result)));
    }

    // ── POST .../retry-ai-draft ─────────────────────────────

    @PostMapping("/{contentProjectId}/retry-ai-draft")
    @Operation(summary = "AI draft üretimini yeniden dene")
    public ResponseEntity<ApiResponse<AiGenerationRequestSummaryResponse>> retryAiDraft(
            @PathVariable UUID contentProjectId) {

        AiGenerationResult result = aiGenerationService.retryAiDraft(contentProjectId);

        return ResponseEntity.ok(ApiResponse.success(toAiSummary(result)));
    }

    // ── POST .../start-review ───────────────────────────────

    @PostMapping("/{contentProjectId}/start-review")
    @Operation(summary = "İnsan review sürecini başlat")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> startReview(
            @PathVariable UUID contentProjectId) {

        ContentProjectResult result = contentProjectService.startReview(contentProjectId);

        return ResponseEntity.ok(ApiResponse.success(toAction(result, "Review süreci başlatıldı")));
    }

    // ── POST .../approve ────────────────────────────────────

    @PostMapping("/{contentProjectId}/approve")
    @Operation(summary = "Draft'ı onayla")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> approve(
            @PathVariable UUID contentProjectId,
            @RequestBody(required = false) ApproveDraftRequest request) {

        String reviewNote = request != null ? request.getReviewNote() : null;
        ContentProjectResult result = contentProjectService.approve(contentProjectId, reviewNote);

        return ResponseEntity.ok(ApiResponse.success(toAction(result, "Draft onaylandı")));
    }

    // ── POST .../request-revision ───────────────────────────

    @PostMapping("/{contentProjectId}/request-revision")
    @Operation(summary = "Revizyon talep et")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> requestRevision(
            @PathVariable UUID contentProjectId,
            @Valid @RequestBody RequestRevisionRequest request) {

        ContentProjectResult result = contentProjectService.requestRevision(
                contentProjectId, request.getReviewNote());

        return ResponseEntity.ok(ApiResponse.success(toAction(result, "Revizyon talep edildi")));
    }

    // ── POST .../reject ─────────────────────────────────────

    @PostMapping("/{contentProjectId}/reject")
    @Operation(summary = "Draft'ı reddet")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> reject(
            @PathVariable UUID contentProjectId,
            @Valid @RequestBody RejectDraftRequest request) {

        ContentProjectResult result = contentProjectService.reject(
                contentProjectId, request.getReviewNote());

        return ResponseEntity.ok(ApiResponse.success(toAction(result, "Draft reddedildi")));
    }

    // ── POST .../mark-ready-for-publish ─────────────────────

    @PostMapping("/{contentProjectId}/mark-ready-for-publish")
    @Operation(summary = "Yayına hazır işaretle")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> markReadyForPublish(
            @PathVariable UUID contentProjectId) {

        ContentProjectResult result = contentProjectService.markReadyForPublish(contentProjectId);

        return ResponseEntity.ok(ApiResponse.success(toAction(result, "Proje yayına hazır")));
    }

    // ── POST .../cancel ─────────────────────────────────────

    @PostMapping("/{contentProjectId}/cancel")
    @Operation(summary = "Projeyi iptal et")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> cancel(
            @PathVariable UUID contentProjectId) {

        ContentProjectResult result = contentProjectService.cancel(contentProjectId);

        return ResponseEntity.ok(ApiResponse.success(toAction(result, "Proje iptal edildi")));
    }

    // ── Private Mapping Helpers ─────────────────────────────

    private ContentProjectResponse toResponse(ContentProjectResult result) {
        return ContentProjectResponse.builder()
                .id(result.getId())
                .knowledgeItemId(result.getKnowledgeItemId())
                .knowledgeItemTitle(result.getKnowledgeItemTitle())
                .contentPurpose(result.getContentPurpose())
                .targetAudience(result.getTargetAudience())
                .tone(result.getTone())
                .language(result.getLanguage())
                .formatHint(result.getFormatHint())
                .lengthHint(result.getLengthHint())
                .status(result.getStatus())
                .retryCount(result.getRetryCount())
                .lastErrorType(result.getLastErrorType())
                .lastErrorMessage(result.getLastErrorMessage())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    private ContentProjectSummaryResponse toSummaryResponse(ContentProjectResult result) {
        return ContentProjectSummaryResponse.builder()
                .id(result.getId())
                .knowledgeItemId(result.getKnowledgeItemId())
                .knowledgeItemTitle(result.getKnowledgeItemTitle())
                .contentPurpose(result.getContentPurpose())
                .status(result.getStatus())
                .language(result.getLanguage())
                .createdAt(result.getCreatedAt())
                .build();
    }

    private AiGenerationRequestSummaryResponse toAiSummary(AiGenerationResult result) {
        return AiGenerationRequestSummaryResponse.builder()
                .contentProjectId(result.getContentProjectId())
                .success(result.isSuccess())
                .contentDraftId(result.getContentDraftId())
                .projectStatus(result.getProjectStatus())
                .draftTitle(result.getDraftTitle())
                .message(result.getMessage())
                .errorType(result.getErrorType())
                .retryable(result.getRetryable())
                .retryCount(result.getRetryCount())
                .maxRetryCount(result.getMaxRetryCount())
                .build();
    }

    private ActionStatusResponse toAction(ContentProjectResult result, String message) {
        return ActionStatusResponse.builder()
                .id(result.getId())
                .status(result.getStatus())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
