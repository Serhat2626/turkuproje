package com.anadoluatlasi.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * FP-012 Task 04: Repository for ReviewAuditEventEntity.
 * Simple lookups only — no custom queries exposing sensitive data.
 */
@Repository
public interface ReviewAuditEventRepository extends JpaRepository<ReviewAuditEventEntity, UUID> {

    /**
     * Find all audit events for a review record, ordered by occurrence time.
     */
    List<ReviewAuditEventEntity> findByReviewRecordIdOrderByOccurredAtAsc(UUID reviewRecordId);

    /**
     * Find the most recent audit events for a review record.
     */
    List<ReviewAuditEventEntity> findTop10ByReviewRecordIdOrderByOccurredAtDesc(UUID reviewRecordId);
}
