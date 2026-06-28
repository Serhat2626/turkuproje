package com.anadoluatlasi.backend.content.entity;

import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * İçerik projesi entity'si.
 * Bir bilgi öğesinden (KnowledgeItem) türetilen içerik üretim sürecinin ana kaydıdır.
 * Senaryo taslakları ve storyboard'lar bu projeye bağlıdır.
 */
@Entity
@Table(name = "content_projects")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentProject {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Bu içerik projesinin dayandığı bilgi öğesi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 50)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_platform", nullable = false, length = 50)
    private TargetPlatform targetPlatform;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContentProjectStatus status;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Bu projeye ait senaryo taslakları.
     */
    @OneToMany(mappedBy = "contentProject", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ScriptDraft> scriptDrafts = new ArrayList<>();

    /**
     * Bu projeye ait storyboard'lar.
     */
    @OneToMany(mappedBy = "contentProject", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Storyboard> storyboards = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
