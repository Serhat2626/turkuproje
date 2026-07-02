package com.anadoluatlasi.backend.ai.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * AI prompt şablonlarını temsil eden entity.
 * Her şablon versiyonlanır; aynı isimde birden fazla versiyon bulunabilir.
 * Aktif şablon üretim pipeline'ında kullanılır.
 */
@Entity
@Table(name = "prompt_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "version"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private PromptCategory category;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PromptTemplateStatus status;
}
