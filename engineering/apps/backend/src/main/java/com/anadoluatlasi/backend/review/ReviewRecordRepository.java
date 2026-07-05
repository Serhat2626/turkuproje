package com.anadoluatlasi.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-012 Task 04: Repository for ReviewRecordEntity.
 * Simple lookups only — no custom queries exposing sensitive data.
 */
@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecordEntity, UUID> {

    /**
     * Find review record by target type and target ID.
     */
    Optional<ReviewRecordEntity> findByReviewTargetTypeAndReviewTargetId(
            ReviewTargetType reviewTargetType, UUID reviewTargetId);

    /**
     * Find all review records with a given current state.
     */
    List<ReviewRecordEntity> findByCurrentState(ReviewState currentState);

    /**
     * Find all review records assigned to a given reviewer.
     */
    List<ReviewRecordEntity> findByCurrentReviewerActorReference(String reviewerActorReference);

    /**
     * Check if a review record exists for a given target.
     */
    boolean existsByReviewTargetTypeAndReviewTargetId(
            ReviewTargetType reviewTargetType, UUID reviewTargetId);

    /**
     * FP-012 Task 10: Find records by target type.
     */
    List<ReviewRecordEntity> findByReviewTargetType(ReviewTargetType reviewTargetType);

    /**
     * FP-012 Task 10: Find records by state and target type.
     */
    List<ReviewRecordEntity> findByCurrentStateAndReviewTargetType(
            ReviewState currentState, ReviewTargetType reviewTargetType);
}
