package com.anadoluatlasi.backend.review;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FP-018 Task 02: Admin actor boundary policy.
 *
 * <p>Classifies admin actions into categories and documents which
 * roles and resolution modes are allowed. This is a non-mutating
 * inventory/policy helper for future controller integration.</p>
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

    /**
     * Check whether SYSTEM can approve. Always false.
     */
    public static boolean canSystemApprove() {
        return false;
    }

    /**
     * Check whether CREATOR can approve. Always false.
     */
    public static boolean canCreatorApprove() {
        return false;
    }

    private AdminActorBoundaryPolicy() {} // utility class
}
