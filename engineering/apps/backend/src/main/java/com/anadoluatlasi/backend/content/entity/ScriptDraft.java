package com.anadoluatlasi.backend.content.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Senaryo taslağı entity'si.
 * Bir içerik projesine ait versiyonlanmış senaryo metnidir.
 * Her taslak AI tarafından üretilip, insan incelemesinden geçebilir.
 */
@Entity
@Table(name = "script_drafts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"content_project_id", "version"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDraft extends BaseEntity {

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
}
