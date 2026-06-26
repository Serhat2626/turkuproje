package com.startup.turkuproje.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

// Versiyonlanmış AI prompt şablonları
@Entity
@Table(name = "prompt_templates")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Şablon adı (ör: "script_generation_v1")
    @Column(nullable = false)
    private String name;

    // Prompt tipi
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromptType type;

    // Şablon içeriği - {{topic}}, {{language}}, {{duration}} gibi değişkenler içerir
    @Column(name = "template_content", columnDefinition = "TEXT", nullable = false)
    private String templateContent;

    // Versiyon numarası
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    // Bu tip için aktif olan şablon mu?
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
