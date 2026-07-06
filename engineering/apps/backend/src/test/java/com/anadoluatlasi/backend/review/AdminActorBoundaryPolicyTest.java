package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-018 Task 02: Admin Actor Boundary Policy tests.
 */
@DisplayName("FP-018 Task 02 — AdminActorBoundaryPolicy")
class AdminActorBoundaryPolicyTest {

    private final AdminActorResolver resolver = new AdminActorResolver();

    // ── A. Action Classification ──────────────────────────────

    @Nested
    @DisplayName("A. Action Classification")
    class Classification {

        @Test
        @DisplayName("SUBMIT_FOR_REVIEW classified correctly")
        void submitClassified() {
            assertThat(AdminActorBoundaryPolicy.classify(ReviewActionType.SUBMIT_FOR_REVIEW))
                    .isEqualTo(AdminActorBoundaryPolicy.ActionCategory.SUBMIT_FOR_REVIEW);
        }

        @Test
        @DisplayName("START_REVIEW classified as REVIEW_ACTION")
        void startReview() {
            assertThat(AdminActorBoundaryPolicy.classify(ReviewActionType.START_REVIEW))
                    .isEqualTo(AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION);
        }

        @Test
        @DisplayName("APPROVE_FOR_INTERNAL_USE classified as REVIEW_ACTION")
        void approve() {
            assertThat(AdminActorBoundaryPolicy.classify(ReviewActionType.APPROVE_FOR_INTERNAL_USE))
                    .isEqualTo(AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION);
        }

        @Test
        @DisplayName("REJECT classified as REVIEW_ACTION")
        void reject() {
            assertThat(AdminActorBoundaryPolicy.classify(ReviewActionType.REJECT))
                    .isEqualTo(AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION);
        }

        @Test
        @DisplayName("ADD_REVIEW_NOTE classified as ADD_REVIEW_NOTE")
        void addNote() {
            assertThat(AdminActorBoundaryPolicy.classify(ReviewActionType.ADD_REVIEW_NOTE))
                    .isEqualTo(AdminActorBoundaryPolicy.ActionCategory.ADD_REVIEW_NOTE);
        }

        @Test
        @DisplayName("all action types classifiable")
        void allClassifiable() {
            for (ReviewActionType t : ReviewActionType.values()) {
                assertThat(AdminActorBoundaryPolicy.classify(t)).isNotNull();
            }
        }
    }

    // ── B. SYSTEM/CREATOR Restrictions ────────────────────────

    @Nested
    @DisplayName("B. SYSTEM/CREATOR Restrictions")
    class Restrictions {

        @Test
        @DisplayName("SYSTEM cannot approve")
        void systemCannotApprove() {
            assertThat(AdminActorBoundaryPolicy.canSystemApprove()).isFalse();
        }

        @Test
        @DisplayName("CREATOR cannot approve")
        void creatorCannotApprove() {
            assertThat(AdminActorBoundaryPolicy.canCreatorApprove()).isFalse();
        }

        @Test
        @DisplayName("SYSTEM not in REVIEW_ACTION allowed roles")
        void systemNotInReviewAction() {
            Set<ReviewActorRole> roles = AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION);
            assertThat(roles).doesNotContain(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("CREATOR not in REVIEW_ACTION allowed roles")
        void creatorNotInReviewAction() {
            Set<ReviewActorRole> roles = AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION);
            assertThat(roles).doesNotContain(ReviewActorRole.CREATOR);
        }

        @Test
        @DisplayName("SYSTEM in INTERNAL_HANDOFF allowed roles")
        void systemInHandoff() {
            Set<ReviewActorRole> roles = AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF);
            assertThat(roles).contains(ReviewActorRole.SYSTEM);
        }
    }

    // ── C. isAllowed Integration ──────────────────────────────

    @Nested
    @DisplayName("C. isAllowed Integration")
    class IsAllowed {

        @Test
        @DisplayName("ADMIN + REVIEW_ACTION allowed in TEST_FALLBACK")
        void adminReviewAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor)).isTrue();
        }

        @Test
        @DisplayName("REVIEWER + REVIEW_ACTION allowed in TEST_FALLBACK")
        void reviewerReviewAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "rev");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor)).isTrue();
        }

        @Test
        @DisplayName("CREATOR + REVIEW_ACTION not allowed")
        void creatorReviewNotAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor)).isFalse();
        }

        @Test
        @DisplayName("SYSTEM + INTERNAL_HANDOFF allowed in SYSTEM_INTERNAL")
        void systemHandoffAllowed() {
            var actor = resolver.resolveSystemInternal("HANDOFF");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF, actor)).isTrue();
        }

        @Test
        @DisplayName("null actor not allowed")
        void nullNotAllowed() {
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, null)).isFalse();
        }

        @Test
        @DisplayName("null category not allowed")
        void nullCategoryNotAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "a");
            assertThat(AdminActorBoundaryPolicy.isAllowed(null, actor)).isFalse();
        }
    }

    // ── D. All Categories Documented ──────────────────────────

    @Nested
    @DisplayName("D. All Categories")
    class AllCategories {

        @Test
        @DisplayName("7 action categories")
        void sixCategories() {
            assertThat(AdminActorBoundaryPolicy.allCategories()).hasSize(7);
        }

        @Test
        @DisplayName("each category has allowed roles")
        void eachHasRoles() {
            for (var cat : AdminActorBoundaryPolicy.allCategories()) {
                assertThat(AdminActorBoundaryPolicy.getAllowedRoles(cat)).isNotEmpty();
            }
        }
    }
}
