package com.anadoluatlasi.backend.review;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FP-012 Task 07: Internal admin review workflow controller.
 *
 * <p>Explicit action endpoints — no generic status update.
 * No publish/export, no public/signed URL, no provider/storage inspection.
 * APPROVED_FOR_INTERNAL_USE is NOT publish/export approval.</p>
 *
 * <p>Safe error handling — no stack trace, no secrets, no raw payloads.</p>
 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin Review Workflow", description = "İç review aksiyonları (Admin)")
public class AdminReviewController {

    private final ReviewApplicationService reviewApplicationService;

    // ── Explicit Action Endpoints ───────────────────────────────────

    @PostMapping("/submit")
    @Operation(summary = "Submit for review")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> submitForReview(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ReviewApplicationResult result = reviewApplicationService.submitForReview(
                request.getTargetType(), request.getTargetId(),
                request.getActorReference(), request.getActorRole(),
                request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/start")
    @Operation(summary = "Start review")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> startReview(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ReviewApplicationResult result = reviewApplicationService.startReview(
                request.getTargetType(), request.getTargetId(),
                request.getActorReference(), request.getActorRole(),
                request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/request-changes")
    @Operation(summary = "Request changes")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> requestChanges(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ReviewApplicationResult result = reviewApplicationService.requestChanges(
                request.getTargetType(), request.getTargetId(),
                request.getActorReference(), request.getActorRole(),
                request.getReasonCode(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/approve-internal")
    @Operation(summary = "Approve for internal use only — NOT publish/export")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> approveForInternalUse(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ReviewApplicationResult result = reviewApplicationService.approveForInternalUse(
                request.getTargetType(), request.getTargetId(),
                request.getActorReference(), request.getActorRole(),
                request.getReasonCode(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject content")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> reject(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ReviewApplicationResult result = reviewApplicationService.reject(
                request.getTargetType(), request.getTargetId(),
                request.getActorReference(), request.getActorRole(),
                request.getReasonCode(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/return-to-draft")
    @Operation(summary = "Return to draft")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> returnToDraft(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ReviewApplicationResult result = reviewApplicationService.returnToDraft(
                request.getTargetType(), request.getTargetId(),
                request.getActorReference(), request.getActorRole(),
                request.getReasonCode(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/note")
    @Operation(summary = "Add review note without state change")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> addReviewNote(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        if (request.getNote() == null || request.getNote().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Note is required for add-note action"));
        }
        ReviewApplicationResult result = reviewApplicationService.addReviewNote(
                request.getTargetType(), request.getTargetId(),
                request.getActorReference(), request.getActorRole(),
                request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    // ── Query Endpoints ─────────────────────────────────────────────

    @GetMapping("/worklist")
    @Operation(summary = "Get review worklist")
    public ResponseEntity<ApiResponse<List<AdminReviewWorklistItem>>> getWorklist(
            @org.springframework.web.bind.annotation.RequestParam(required = false) ReviewState state,
            @org.springframework.web.bind.annotation.RequestParam(required = false) ReviewTargetType targetType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String reviewer,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int limit) {
        List<AdminReviewWorklistItem> items = reviewApplicationService.listWorkItems(
                state, targetType, reviewer, limit);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/{targetType}/{targetId}/context")
    @Operation(summary = "Get review target context")
    public ResponseEntity<ApiResponse<AdminReviewTargetContext>> getTargetContext(
            @PathVariable ReviewTargetType targetType,
            @PathVariable UUID targetId) {
        Optional<AdminReviewTargetContext> context =
                reviewApplicationService.getTargetContext(targetType, targetId);
        if (context.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Review target context not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(context.get()));
    }

    @GetMapping("/{targetType}/{targetId}/state")
    @Operation(summary = "Get current review state summary")
    public ResponseEntity<ApiResponse<AdminReviewStateSummary>> getReviewState(
            @PathVariable ReviewTargetType targetType,
            @PathVariable UUID targetId) {
        Optional<AdminReviewStateSummary> summary =
                reviewApplicationService.getReviewStateSummary(targetType, targetId);
        if (summary.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Review record not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(summary.get()));
    }

    @GetMapping("/{targetType}/{targetId}/audit")
    @Operation(summary = "Get review audit timeline")
    public ResponseEntity<ApiResponse<List<AdminReviewAuditTimelineItem>>> getAuditTimeline(
            @PathVariable ReviewTargetType targetType,
            @PathVariable UUID targetId) {
        List<ReviewAuditEventEntity> events = reviewApplicationService.getAuditTimeline(targetType, targetId);
        List<AdminReviewAuditTimelineItem> timeline = events.stream()
                .map(AdminReviewDtoMapper::toTimelineItem)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    @GetMapping("/{targetType}/{targetId}/actions")
    @Operation(summary = "Get available review actions for actor role")
    public ResponseEntity<ApiResponse<AdminReviewActionAvailability>> getAvailableActions(
            @PathVariable ReviewTargetType targetType,
            @PathVariable UUID targetId,
            @org.springframework.web.bind.annotation.RequestParam ReviewActorRole actorRole) {
        Optional<AdminReviewActionAvailability> availability =
                reviewApplicationService.getAvailableActions(targetType, targetId, actorRole);
        if (availability.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Review record not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(availability.get()));
    }

    // ── Request Validation ──────────────────────────────────────────

    private void validateActionRequest(AdminReviewActionRequest request) {
        if (request.getTargetType() == null) {
            throw new IllegalArgumentException("targetType is required");
        }
        if (request.getTargetId() == null) {
            throw new IllegalArgumentException("targetId is required");
        }
        if (request.getActorRole() == null) {
            throw new IllegalArgumentException("actorRole is required");
        }
        if (request.getActorReference() == null || request.getActorReference().isBlank()) {
            throw new IllegalArgumentException("actorReference is required");
        }
    }

    // ── Exception Handlers ──────────────────────────────────────────

    @ExceptionHandler(ReviewApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleReviewApplicationException(ReviewApplicationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ReviewTransitionViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransitionViolation(ReviewTransitionViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }
}
