package com.anadoluatlasi.backend.scenario.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * FP-002: Senaryo beat entity'si.
 *
 * <p>Bir ScenarioDraft içindeki tek anlatım birimini temsil eder.
 * Her senaryo 6 zorunlu beat içerir (HOOK → INTRODUCTION → MAIN_NARRATIVE →
 * EMOTIONAL_CLIMAX → CONCLUSION → CALL_TO_ACTION).</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scenario_beats")
public class ScenarioBeat extends BaseEntity {

    /** Bu beat'in ait olduğu senaryo taslağı */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_draft_id", nullable = false)
    private ScenarioDraft scenarioDraft;

    /** Beat tipi */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private ScenarioBeatType type;

    /** İnsan-okunur etiket */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /** Beat içerik metni */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /** Sıralama numarası (1-6) */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /** Tahmini süre (saniye) */
    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds;
}
