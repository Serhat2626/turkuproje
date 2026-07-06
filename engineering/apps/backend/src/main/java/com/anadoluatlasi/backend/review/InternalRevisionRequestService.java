package com.anadoluatlasi.backend.review;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * FP-015 Task 02: Safe internal revision request service.
 *
 * <p>Wraps existing ReviewApplicationService for the narrow
 * REQUEST_INTERNAL_REVISION action with additional validation.</p>
 *
 * <p>Allowed source states:</p>
 * <ul>
 *   <li>APPROVED_FOR_INTERNAL_USE → transitions to CHANGES_REQUESTED</li>
 *   <li>CHANGES_REQUESTED → appends safe note (idempotent, no duplicate transition)</li>
 * </ul>
 *
 * <p>Allowed actor roles:</p>
 * <ul>
 *   <li>REVIEWER</li>
 *   <li>ADMIN</li>
 * </ul>
 *
 * <p>Forbidden actor roles:</p>
 * <ul>
 *   <li>SYSTEM — cannot request revision</li>
 *   <li>CREATOR — cannot request revision (uses existing SUBMIT_FOR_REVIEW flow)</li>
 * </ul>
 *
 * <p>This service does NOT:</p>
 * <ul>
 *   <li>Publish or export</li>
 *   <li>Approve anything</li>
 *   <li>Return to draft</li>
 *   <li>Trigger generation or regeneration</li>
 *   <li>Invoke provider or storage</li>
 *   <li>Create public/signed/CDN URLs</li>
 *   <li>Expose provider URL, storage key, bucket, raw payload, base64, bytes, secrets</li>
 * </ul>
 */
@Service
public class InternalRevisionRequestService {

    private final ReviewApplicationService reviewApplicationService;

    public InternalRevisionRequestService(ReviewApplicationService reviewApplicationService) {
        this.reviewApplicationService = reviewApplicationService;
    }

    /**
     * Process an internal revision request.
     *
     * <p>For APPROVED_FOR_INTERNAL_USE: delegates to ReviewApplicationService.requestInternalRevision()
     * which transitions to CHANGES_REQUESTED via the domain model.</p>
     *
     * <p>For CHANGES_REQUESTED: appends a safe note via ReviewApplicationService.addReviewNote()
     * (idempotent — no duplicate state transition).</p>
     *
     * <p>For all other states: throws InternalRevisionRequestException with safe error code.</p>
     *
     * @param request validated internal revision request
     * @return safe result with transition details
     * @throws InternalRevisionRequestException if request is rejected
     */
    @Transactional
    public InternalRevisionRequestResult requestRevision(InternalRevisionRequest request) {
        if (request == null) {
            throw new InternalRevisionRequestException(
                    "Internal revision request must not be null",
                    "NULL_REQUEST");
        }

        // Validate actor role
        validateActorRole(request.getActorRole());

        // Get current review state
        ReviewState currentState = reviewApplicationService
                .getReviewState(request.getTargetType(), request.getTargetId())
                .orElseThrow(() -> new InternalRevisionRequestException(
                        "Review record not found for " + request.getTargetType()
                                + " " + request.getTargetId(),
                        "REVIEW_NOT_FOUND"));

        // Route based on current state
        return switch (currentState) {
            case APPROVED_FOR_INTERNAL_USE -> handleApprovedRevision(request, currentState);
            case CHANGES_REQUESTED -> handleIdempotentNoteAppend(request, currentState);
            default -> handleForbiddenState(currentState);
        };
    }

    // ── Internal handlers ───────────────────────────────────────────

    /**
     * APPROVED_FOR_INTERNAL_USE → CHANGES_REQUESTED via domain model.
     */
    private InternalRevisionRequestResult handleApprovedRevision(
            InternalRevisionRequest request, ReviewState previousState) {
        ReviewApplicationResult appResult = reviewApplicationService.requestInternalRevision(
                request.getTargetType(),
                request.getTargetId(),
                request.getActorReference(),
                request.getActorRole(),
                request.getSafeReasonCode(),
                request.getSafeMessage());

        return InternalRevisionRequestResult.builder()
                .previousState(previousState)
                .currentState(appResult.getCurrentState())
                .actionApplied("REQUEST_INTERNAL_REVISION")
                .safeReasonCode(request.getSafeReasonCode())
                .safeMessage(appResult.getSafeMessage())
                .updatedAt(LocalDateTime.now())
                .appendedNote(false)
                .auditRecorded(true)
                .build();
    }

    /**
     * CHANGES_REQUESTED → append safe note only (idempotent).
     * No duplicate state transition.
     */
    private InternalRevisionRequestResult handleIdempotentNoteAppend(
            InternalRevisionRequest request, ReviewState currentState) {
        ReviewApplicationResult appResult = reviewApplicationService.addReviewNote(
                request.getTargetType(),
                request.getTargetId(),
                request.getActorReference(),
                request.getActorRole(),
                request.getSafeMessage());

        return InternalRevisionRequestResult.builder()
                .previousState(currentState)
                .currentState(currentState)
                .actionApplied("NOTE_APPENDED")
                .safeReasonCode(request.getSafeReasonCode())
                .safeMessage(appResult.getSafeMessage())
                .updatedAt(LocalDateTime.now())
                .appendedNote(true)
                .auditRecorded(true)
                .build();
    }

    /**
     * Forbidden state — no mutation, safe exception.
     */
    private InternalRevisionRequestResult handleForbiddenState(ReviewState currentState) {
        throw new InternalRevisionRequestException(
                "Internal revision request not allowed from state " + currentState.name(),
                "STATE_NOT_ALLOWED");
    }

    /**
     * Validate actor role — only REVIEWER and ADMIN allowed.
     */
    private void validateActorRole(ReviewActorRole actorRole) {
        if (actorRole == null) {
            throw new InternalRevisionRequestException(
                    "Actor role must not be null",
                    "NULL_ACTOR_ROLE");
        }
        if (actorRole == ReviewActorRole.SYSTEM) {
            throw new InternalRevisionRequestException(
                    "SYSTEM cannot request internal revision",
                    "SYSTEM_NOT_ALLOWED");
        }
        if (actorRole == ReviewActorRole.CREATOR) {
            throw new InternalRevisionRequestException(
                    "CREATOR cannot request internal revision",
                    "CREATOR_NOT_ALLOWED");
        }
    }
}
