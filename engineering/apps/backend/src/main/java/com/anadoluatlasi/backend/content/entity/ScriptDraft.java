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
 * Senaryo taslağı entity'si.
 * Bir içerik projesine ait versiyonlanmış senaryo metnidir.
 * Her taslak AI tarafından üretilip, insan incelemesinden geçebilir.
 */
@Entity
@Table(name = "script_drafts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"content_project_id", "version"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDraft {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Bu taslağın ait olduğu içerik projesi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id", nullable = false)
    private ContentProject contentProject;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ScriptDraftStatus status;

    @Column(name = "created_by", length = 255)
    private String createdBy;

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
