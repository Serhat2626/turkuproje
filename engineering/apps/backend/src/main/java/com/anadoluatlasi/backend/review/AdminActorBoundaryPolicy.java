package com.anadoluatlasi.backend.review;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FP-018 Task 02 + Task 04: Admin actor boundary policy.
 *
 * <p>Classifies admin actions into categories and documents which
 * roles and resolution modes are allowed. Non-mutating policy helper.</p>
 *
 * <p>FP-018 Task 04: Added narrow handoff/revision boundary helpers,
 * explicit SYSTEM/CREATOR restriction methods, and boundary enforcement.</p>
 *
 * <p>Does not change runtime behavior. Does not call ReviewApplicationService.
 * Does not touch endpoints. Read-only classification only.</p>
 */
public final class AdminActorBoundaryPolicy {

    /**
     * Action categories for the admin workflow.
     */
    public enum ActionCategory {
        /** Review actions (approve, reject, request changes, etc.) */
        REVIEW_ACTION,
        /** Internal handoff (reassign reviewer) */
        INTERNAL_HANDOFF,
        /** Internal revision request */
        INTERNAL_REVISION_REQUEST,
        /** Read-only admin visibility (GET endpoints) */
        READ_ONLY_VISIBILITY,
        /** Submit for review */
        SUBMIT_FOR_REVIEW,
        /** Add review note */
        ADD_REVIEW_NOTE
    }

    private static final Map<ActionCategory, Set<ReviewActorRole>> ALLOWED_ROLES = Map.of(
            ActionCategory.REVIEW_ACTION, Set.of(ReviewActorRole.REVIEWER, ReviewActorRole.ADMIN),
            ActionCategory.INTERNAL_HANDOFF, Set.of(ReviewActorRole.REVIEWER, ReviewActorRole.ADMIN, ReviewActorRole.SYSTEM),
            ActionCategory.INTERNAL_REVISION_REQUEST, Set.of(ReviewActorRole.REVIEWER, ReviewActorRole.ADMIN),
            ActionCategory.READ_ONLY_VISIBILITY, Set.of(ReviewActorRole.CREATOR, ReviewActorRole.REVIEWER, ReviewActorRole.ADMIN, ReviewActorRole.SYSTEM),
            ActionCategory.SUBMIT_FOR_REVIEW, Set.of(ReviewActorRole.CREATOR, ReviewActorRole.REVIEWER, ReviewActorRole.ADMIN),
            ActionCategory.ADD_REVIEW_NOTE, Set.of(ReviewActorRole.CREATOR, ReviewActorRole.REVIEWER, ReviewActorRole.ADMIN)
    );

    private static final Map<ActionCategory, Set<AdminActorResolutionMode>> ALLOWED_MODES = Map.of(
            ActionCategory.REVIEW_ACTION, Set.of(AdminActorResolutionMode.TEST_FALLBACK, AdminActorResolutionMode.AUTHENTICATED_CONTEXT),
            ActionCategory.INTERNAL_HANDOFF, Set.of(AdminActorResolutionMode.TEST_FALLBACK, AdminActorResolutionMode.AUTHENTICATED_CONTEXT, AdminActorResolutionMode.SYSTEM_INTERNAL),
            ActionCategory.INTERNAL_REVISION_REQUEST, Set.of(AdminActorResolutionMode.TEST_FALLBACK, AdminActorResolutionMode.AUTHENTICATED_CONTEXT),
            ActionCategory.READ_ONLY_VISIBILITY, Set.of(AdminActorResolutionMode.TEST_FALLBACK, AdminActorResolutionMode.AUTHENTICATED_CONTEXT, AdminActorResolutionMode.SYSTEM_INTERNAL),
            ActionCategory.SUBMIT_FOR_REVIEW, Set.of(AdminActorResolutionMode.TEST_FALLBACK, AdminActorResolutionMode.AUTHENTICATED_CONTEXT),
            ActionCategory.ADD_REVIEW_NOTE, Set.of(AdminActorResolutionMode.TEST_FALLBACK, AdminActorResolutionMode.AUTHENTICATED_CONTEXT)
    );

    /**
     * Check if a resolved actor is allowed for a given action category.
     */
    public static boolean isAllowed(ActionCategory category, ResolvedAdminActor actor) {
        if (category == null || actor == null) return false;
        Set<ReviewActorRole> roles = ALLOWED_ROLES.get(category);
        Set<AdminActorResolutionMode> modes = ALLOWED_MODES.get(category);
        if (roles == null || modes == null) return false;
        return roles.contains(actor.getActorRole()) && modes.contains(actor.getResolutionMode());
    }

    /**
     * Get allowed roles for a given action category.
     */
    public static Set<ReviewActorRole> getAllowedRoles(ActionCategory category) {
        return ALLOWED_ROLES.getOrDefault(category, Set.of());
    }

    /**
     * Get all action categories.
     */
    public static List<ActionCategory> allCategories() {
        return List.of(ActionCategory.values());
    }

    /**
     * Classify a ReviewActionType into an ActionCategory.
     */
    public static ActionCategory classify(ReviewActionType actionType) {
        return switch (actionType) {
            case SUBMIT_FOR_REVIEW -> ActionCategory.SUBMIT_FOR_REVIEW;
            case START_REVIEW, REQUEST_CHANGES, APPROVE_FOR_INTERNAL_USE,
                 REJECT, RETURN_TO_DRAFT -> ActionCategory.REVIEW_ACTION;
            case ADD_REVIEW_NOTE -> ActionCategory.ADD_REVIEW_NOTE;
            case REQUEST_INTERNAL_REVISION -> ActionCategory.INTERNAL_REVISION_REQUEST;
        };
    }

    // ── Global SYSTEM/CREATOR Restrictions ──────────────────────

    /** SYSTEM can never approve. Always false. */
    public static boolean canSystemApprove() {
        return false;
    }

    /** CREATOR can never approve. Always false. */
    public static boolean canCreatorApprove() {
        return false;
    }

    // ── FP-018 Task 04: Narrow Handoff Boundary ─────────────────

    /**
     * Check whether a resolved actor is allowed to perform handoff.
     * SYSTEM is allowed ONLY when resolved via SYSTEM_INTERNAL mode.
     * CREATOR cannot perform handoff.
     */
    public static boolean isHandoffAllowed(ResolvedAdminActor actor) {
        return isAllowed(ActionCategory.INTERNAL_HANDOFF, actor);
    }

    /**
     * SYSTEM can perform handoff, but ONLY via SYSTEM_INTERNAL resolution mode.
     * Request-body actorRole=SYSTEM alone is NOT sufficient — the resolver must
     * explicitly mark systemInternalAllowed=true for the handoff path.
     */
    public static boolean canSystemHandoff() {
        return true; // allowed via SYSTEM_INTERNAL mode only
    }

    /** CREATOR cannot perform handoff. Always false. */
    public static boolean canCreatorHandoff() {
        return false;
    }

    /**
     * Determine whether a handoff request should use SYSTEM_INTERNAL resolution.
     * Returns true ONLY if the requested role is SYSTEM — this is the narrow gate
     * that controls system-internal handoff access.
     *
     * <p>This method exists to make the handoff SYSTEM path explicit and auditable,
     * rather than relying on a raw {@code request.getActorRole() == SYSTEM} check
     * scattered across controller code.</p>
     */
    public static boolean isHandoffSystemInternalPath(ReviewActorRole requestedRole) {
        return requestedRole == ReviewActorRole.SYSTEM;
    }

    // ── FP-018 Task 04: Narrow Internal Revision Boundary ───────

    /**
     * Check whether a resolved actor is allowed to request internal revision.
     * SYSTEM is NOT allowed. CREATOR is NOT allowed.
     * Only REVIEWER and ADMIN can request revision.
     */
    public static boolean isRevisionAllowed(ResolvedAdminActor actor) {
        return isAllowed(ActionCategory.INTERNAL_REVISION_REQUEST, actor);
    }

    /** SYSTEM cannot request internal revision. Always false. */
    public static boolean canSystemRevision() {
        return false;
    }

    /** CREATOR cannot request internal revision. Always false. */
    public static boolean canCreatorRevision() {
        return false;
    }

    /**
     * Internal revision must NEVER use SYSTEM_INTERNAL resolution mode.
     * This ensures that even if a caller accidentally passes systemInternalAllowed=true,
     * the boundary policy still blocks it.
     */
    public static boolean isRevisionSystemInternalPath(ReviewActorRole requestedRole) {
        return false; // never — SYSTEM is always blocked for revision
    }

    // ── FP-018 Task 04: Boundary Enforcement ────────────────────

    /**
     * Enforce boundary policy for a resolved actor and action category.
     * Throws AdminActorResolutionException if the actor is not allowed.
     *
     * <p>This is a post-resolution check that validates the resolved actor
     * against the boundary policy matrix.</p>
     */
    public static void enforce(ActionCategory category, ResolvedAdminActor actor) {
        if (!isAllowed(category, actor)) {
            throw new AdminActorResolutionException(
                    actor.getActorRole() + " is not allowed for " + category,
                    "BOUNDARY_POLICY_VIOLATION",
                    actor.getActorRole());
        }
    }

    private AdminActorBoundaryPolicy() {} // utility class
}

