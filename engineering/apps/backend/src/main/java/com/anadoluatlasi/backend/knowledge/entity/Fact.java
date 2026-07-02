package com.anadoluatlasi.backend.knowledge.entity;

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
 * Olgu (fact) entity'si.
 * Bir bilgi öğesine ait doğrulanabilir ifadeleri temsil eder.
 * Her olgu isteğe bağlı olarak bir kaynak referansına bağlanabilir
 * ve doğrulama durumu takip edilir.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "facts")
public class Fact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_reference_id")
    private SourceReference sourceReference;

    @Column(name = "statement", nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
