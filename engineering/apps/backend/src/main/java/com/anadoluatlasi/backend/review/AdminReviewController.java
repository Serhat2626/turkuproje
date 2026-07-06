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
 * FP-012 Task 07 + FP-013 Task 04 + FP-014 Task 03 + FP-018 Task 03:
 * Internal admin review workflow controller.
 *
 * <p>Explicit action endpoints — no generic status update.
 * No publish/export, no public/signed URL, no provider/storage inspection.
 * APPROVED_FOR_INTERNAL_USE is NOT publish/export approval.</p>
 *
 * <p>FP-013 Task 04: Handoff and safe preview endpoints added.
 * Handoff is idempotent. Preview returns safe metadata only — no bytes/URL/base64.</p>
 *
 * <p>FP-014 Task 03: Internal approval readiness endpoints added.
 * Read-only derived views — no state mutation, no external calls.</p>
 *
 * <p>FP-015 Task 03: Internal revision request endpoint added.
 * Narrow action endpoint — NOT generic status update.
 * Total endpoints: 17 (9 POST + 8 GET).</p>
 *
 * <p>FP-018 Task 03: Review actions now route through AdminActorResolver.
 * Request actorRole is treated as TEST_FALLBACK input, not production auth.
 * No JWT/session/principal/authorization header integration.
 * No new endpoints. Endpoint count unchanged.</p>
 *
 * <p>Safe error handling — no stack trace, no secrets, no raw payloads.</p>
 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin Review Workflow", description = "İç review aksiyonları (Admin)")
public class AdminReviewController {

    private final ReviewApplicationService reviewApplicationService;
    private final ReviewHandoffService reviewHandoffService;
    private final AdminPreviewService adminPreviewService;
    private final InternalApprovalReadinessService internalApprovalReadinessService;
    private final InternalRevisionRequestService internalRevisionRequestService;
    private final AdminActorResolver adminActorResolver;

    // ── Explicit Action Endpoints ───────────────────────────────────

    @PostMapping("/submit")
    @Operation(summary = "Submit for review")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> submitForReview(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ResolvedAdminActor actor = resolveReviewActor(request);
        ReviewApplicationResult result = reviewApplicationService.submitForReview(
                request.getTargetType(), request.getTargetId(),
                actor.getActorReference(), actor.getActorRole(),
                request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/start")
    @Operation(summary = "Start review")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> startReview(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ResolvedAdminActor actor = resolveReviewActor(request);
        ReviewApplicationResult result = reviewApplicationService.startReview(
                request.getTargetType(), request.getTargetId(),
                actor.getActorReference(), actor.getActorRole(),
                request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/request-changes")
    @Operation(summary = "Request changes")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> requestChanges(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ResolvedAdminActor actor = resolveReviewActor(request);
        ReviewApplicationResult result = reviewApplicationService.requestChanges(
                request.getTargetType(), request.getTargetId(),
                actor.getActorReference(), actor.getActorRole(),
                request.getReasonCode(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/approve-internal")
    @Operation(summary = "Approve for internal use only — NOT publish/export")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> approveForInternalUse(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ResolvedAdminActor actor = resolveReviewActor(request);
        ReviewApplicationResult result = reviewApplicationService.approveForInternalUse(
                request.getTargetType(), request.getTargetId(),
                actor.getActorReference(), actor.getActorRole(),
                request.getReasonCode(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject content")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> reject(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ResolvedAdminActor actor = resolveReviewActor(request);
        ReviewApplicationResult result = reviewApplicationService.reject(
                request.getTargetType(), request.getTargetId(),
                actor.getActorReference(), actor.getActorRole(),
                request.getReasonCode(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    @PostMapping("/return-to-draft")
    @Operation(summary = "Return to draft")
    public ResponseEntity<ApiResponse<AdminReviewActionResponse>> returnToDraft(
            @Valid @RequestBody AdminReviewActionRequest request) {
        validateActionRequest(request);
        ResolvedAdminActor actor = resolveReviewActor(request);
        ReviewApplicationResult result = reviewApplicationService.returnToDraft(
                request.getTargetType(), request.getTargetId(),
                actor.getActorReference(), actor.getActorRole(),
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
        ResolvedAdminActor actor = resolveReviewActor(request);
        ReviewApplicationResult result = reviewApplicationService.addReviewNote(
                request.getTargetType(), request.getTargetId(),
                actor.getActorReference(), actor.getActorRole(),
                request.getNote());
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toActionResponse(result)));
    }

    // ── FP-013 Task 04: Handoff & Preview Endpoints ─────────────────

    @PostMapping("/handoff")
    @Operation(summary = "Hand off generated output to review workflow — idempotent, no auto-approval")
    public ResponseEntity<ApiResponse<AdminReviewHandoffResponse>> handoffForReview(
            @Valid @RequestBody AdminReviewHandoffRequest request) {
        validateHandoffRequest(request);
        ResolvedAdminActor actor = resolveHandoffActor(request);
        ReviewHandoffRequest internalRequest = AdminReviewDtoMapper.toHandoffRequest(
                request, actor.getActorRole(), actor.getActorReference());
        ReviewHandoffResult result = reviewHandoffService.handoffForReview(internalRequest);
        return ResponseEntity.ok(ApiResponse.success(AdminReviewDtoMapper.toHandoffResponse(result)));
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

    @GetMapping("/{targetType}/{targetId}/preview")
    @Operation(summary = "Get safe preview metadata — no bytes/URL/base64/storage key")
    public ResponseEntity<ApiResponse<SafeAdminPreview>> getPreview(
            @PathVariable ReviewTargetType targetType,
            @PathVariable UUID targetId) {
        SafeAdminPreview preview = adminPreviewService.getPreview(targetType, targetId);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    // ── FP-014 Task 03: Internal Approval Readiness Endpoints ────────

    @GetMapping("/{targetType}/{targetId}/internal-readiness")
    @Operation(summary = "Get internal approval readiness — read-only, no publish/export")
    public ResponseEntity<ApiResponse<SafeInternalApprovalReadiness>> getInternalReadiness(
            @PathVariable ReviewTargetType targetType,
            @PathVariable UUID targetId) {
        SafeInternalApprovalReadiness readiness =
                internalApprovalReadinessService.getReadiness(targetType, targetId);
        return ResponseEntity.ok(ApiResponse.success(readiness));
    }

    @GetMapping("/approved/internal-readiness")
    @Operation(summary = "List approved internal readiness candidates — read-only, no publish/export")
    public ResponseEntity<ApiResponse<List<SafeInternalApprovalReadiness>>> getApprovedReadinessList(
            @org.springframework.web.bind.annotation.RequestParam(required = false) ReviewTargetType targetType) {
        List<SafeInternalApprovalReadiness> readinessList =
                internalApprovalReadinessService.getApprovedReadinessList(targetType);
        return ResponseEntity.ok(ApiResponse.success(readinessList));
    }

    // ── FP-015 Task 03: Internal Revision Request Endpoint ───────────

    @PostMapping("/{targetType}/{targetId}/request-internal-revision")
    @Operation(summary = "Request internal revision — narrow action, NOT generic status update, NOT publish/export")
    public ResponseEntity<ApiResponse<AdminInternalRevisionResponse>> requestInternalRevision(
            @PathVariable ReviewTargetType targetType,
            @PathVariable UUID targetId,
            @Valid @RequestBody AdminInternalRevisionRequest request) {
        validateInternalRevisionRequest(request);
        ResolvedAdminActor actor = resolveRevisionActor(request);
        InternalRevisionRequest serviceRequest = InternalRevisionRequest.builder()
                .targetType(targetType)
                .targetId(targetId)
                .actorRole(actor.getActorRole())
                .actorReference(actor.getActorReference())
                .safeReasonCode(request.getSafeReasonCode())
                .safeMessage(request.getSafeMessage())
                .build();
        InternalRevisionRequestResult result = internalRevisionRequestService.requestRevision(serviceRequest);
        AdminInternalRevisionResponse response = AdminInternalRevisionResponse.builder()
                .targetType(targetType)
                .targetId(targetId)
                .previousState(result.getPreviousState())
                .currentState(result.getCurrentState())
                .actionApplied(result.getActionApplied())
                .safeReasonCode(result.getSafeReasonCode())
                .safeMessage(result.getSafeMessage())
                .appendedNote(result.isAppendedNote())
                .auditRecorded(result.isAuditRecorded())
                .updatedAt(result.getUpdatedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
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

    private void validateInternalRevisionRequest(AdminInternalRevisionRequest request) {
        if (request.getActorRole() == null) {
            throw new IllegalArgumentException("actorRole is required");
        }
        if (request.getActorReference() == null || request.getActorReference().isBlank()) {
            throw new IllegalArgumentException("actorReference is required");
        }
        if (request.getSafeMessage() == null || request.getSafeMessage().isBlank()) {
            throw new IllegalArgumentException("safeMessage is required");
        }
    }

    private void validateHandoffRequest(AdminReviewHandoffRequest request) {
        if (request.getTargetType() == null) {
            throw new IllegalArgumentException("targetType is required");
        }
        if (request.getTargetId() == null) {
            throw new IllegalArgumentException("targetId is required");
        }
        if (request.getSource() == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (request.getActorRole() == null) {
            throw new IllegalArgumentException("actorRole is required");
        }
        if (request.getActorReference() == null || request.getActorReference().isBlank()) {
            throw new IllegalArgumentException("actorReference is required");
        }
    }

    // ── FP-018 Task 03 + Task 04: Actor Resolution Helpers ───────────

    /**
     * Resolve actor for standard review actions.
     * SYSTEM is NOT allowed through review controller request body.
     * fallbackAllowed=true for current TEST_FALLBACK compatibility.
     */
    private ResolvedAdminActor resolveReviewActor(AdminReviewActionRequest request) {
        return adminActorResolver.resolve(AdminActorResolutionRequest.builder()
                .requestedActorRole(request.getActorRole())
                .requestedActorReference(request.getActorReference())
                .fallbackAllowed(true)
                .systemInternalAllowed(false)
                .safeReasonCode("REVIEW_ACTION")
                .build());
    }

    /**
     * FP-018 Task 04: Resolve actor for handoff with narrow system boundary.
     *
     * <p>SYSTEM is allowed ONLY through the narrow handoff-specific policy gate
     * ({@link AdminActorBoundaryPolicy#isHandoffSystemInternalPath}), not by
     * raw request-body actorRole alone.</p>
     *
     * <p>After resolution, enforces handoff boundary policy to ensure the
     * resolved actor is actually permitted for INTERNAL_HANDOFF.</p>
     */
    private ResolvedAdminActor resolveHandoffActor(AdminReviewHandoffRequest request) {
        boolean isSystemPath = AdminActorBoundaryPolicy.isHandoffSystemInternalPath(request.getActorRole());
        ResolvedAdminActor actor = adminActorResolver.resolve(AdminActorResolutionRequest.builder()
                .requestedActorRole(request.getActorRole())
                .requestedActorReference(request.getActorReference())
                .fallbackAllowed(true)
                .systemInternalAllowed(isSystemPath)
                .safeReasonCode("HANDOFF")
                .build());
        AdminActorBoundaryPolicy.enforce(
                AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF, actor);
        return actor;
    }

    /**
     * FP-018 Task 04: Resolve actor for internal revision request with hardened boundary.
     *
     * <p>SYSTEM is NEVER allowed for revision requests. The systemInternalAllowed flag
     * is always false, and the boundary policy is enforced after resolution.</p>
     *
     * <p>This double-check ensures even accidental configuration cannot allow SYSTEM
     * through the revision path.</p>
     */
    private ResolvedAdminActor resolveRevisionActor(AdminInternalRevisionRequest request) {
        // isRevisionSystemInternalPath always returns false — SYSTEM is always blocked
        boolean isSystemPath = AdminActorBoundaryPolicy.isRevisionSystemInternalPath(request.getActorRole());
        ResolvedAdminActor actor = adminActorResolver.resolve(AdminActorResolutionRequest.builder()
                .requestedActorRole(request.getActorRole())
                .requestedActorReference(request.getActorReference())
                .fallbackAllowed(true)
                .systemInternalAllowed(isSystemPath)
                .safeReasonCode("INTERNAL_REVISION_REQUEST")
                .build());
        AdminActorBoundaryPolicy.enforce(
                AdminActorBoundaryPolicy.ActionCategory.INTERNAL_REVISION_REQUEST, actor);
        return actor;
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

    @ExceptionHandler(ReviewHandoffException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandoffException(ReviewHandoffException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AdminPreviewException.class)
    public ResponseEntity<ApiResponse<Void>> handlePreviewException(AdminPreviewException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InternalApprovalReadinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleReadinessException(InternalApprovalReadinessException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AdminActorResolutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleActorResolutionException(AdminActorResolutionException ex) {
        HttpStatus status = switch (ex.getSafeErrorCode()) {
            case "SYSTEM_NOT_ALLOWED", "FALLBACK_NOT_ALLOWED",
                 "BOUNDARY_POLICY_VIOLATION" -> HttpStatus.FORBIDDEN;
            case "ACTOR_ROLE_NULL", "RESOLUTION_REQUEST_NULL" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InternalRevisionRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalRevisionException(InternalRevisionRequestException ex) {
        HttpStatus status = switch (ex.getSafeErrorCode()) {
            case "REVIEW_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "SYSTEM_NOT_ALLOWED", "CREATOR_NOT_ALLOWED" -> HttpStatus.FORBIDDEN;
            case "STATE_NOT_ALLOWED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
