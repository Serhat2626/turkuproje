package com.anadoluatlasi.backend.scenario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

/**
 * FP-002: Senaryo beat entity'si.
 *
 * <p>Bir ScenarioDraft içindeki tek anlatım birimini temsil eder.
 * Her senaryo 6 zorunlu beat içerir (HOOK → INTRODUCTION → MAIN_NARRATIVE →
 * EMOTIONAL_CLIMAX → CONCLUSION → CALL_TO_ACTION).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scenario_beats")
@EntityListeners(AuditingEntityListener.class)
public class ScenarioBeat {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

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

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
