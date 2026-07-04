package com.anadoluatlasi.backend.imagegen.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Generated asset review kaydı.
 *
 * <p>Admin tarafından verilen her review kararının audit kaydı.
 * Her review bir {@link GeneratedAssetReviewDecision} ile sonuçlanır.</p>
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>Reject durumunda rejectionReason ileride zorunlu olacaktır.</li>
 *   <li>Otomatik review kararı yoktur.</li>
 *   <li>Generic status update ile review yapılmaz.</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fp007_generated_asset_reviews")
public class GeneratedAssetReview extends BaseEntity {

    // ── References ──────────────────────────────────────────────────

    @Column(name = "generated_asset_id", nullable = false)
    private UUID generatedAssetId;

    // ── Decision ────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private GeneratedAssetReviewDecision decision;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ── Audit ────────────────────────────────────────────────────────

    @Column(name = "reviewed_by", nullable = false, length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;
}
