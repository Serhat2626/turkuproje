package com.anadoluatlasi.backend.content.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Storyboard sahnesi entity'si.
 * Bir storyboard'a ait tek bir sahneyi temsil eder.
 * Sahne sırası, anlatım metni, görsel açıklama, mood, kamera stili gibi
 * sahne detaylarını içerir.
 */
@Entity
@Table(name = "storyboard_scenes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"storyboard_id", "scene_order"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardScene {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Bu sahnenin ait olduğu storyboard.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storyboard_id", nullable = false)
    private Storyboard storyboard;

    @Column(name = "scene_order", nullable = false)
    private Integer sceneOrder;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "narration_text", columnDefinition = "TEXT")
    private String narrationText;

    @Column(name = "visual_description", columnDefinition = "TEXT")
    private String visualDescription;

    @Column(name = "mood", length = 100)
    private String mood;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "characters", columnDefinition = "TEXT")
    private String characters;

    @Column(name = "camera_style", length = 255)
    private String cameraStyle;

    @Column(name = "transition_note", columnDefinition = "TEXT")
    private String transitionNote;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
