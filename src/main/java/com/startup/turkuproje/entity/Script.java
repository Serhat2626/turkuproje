package com.startup.turkuproje.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

// YouTube video senaryosu entity
@Entity
@Table(name = "scripts")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // İlişkili proje
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Tam senaryo metni
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Senaryo bölümleri
    @Column(columnDefinition = "TEXT")
    private String hook;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(columnDefinition = "TEXT")
    private String story;

    @Column(columnDefinition = "TEXT")
    private String climax;

    @Column(columnDefinition = "TEXT")
    private String closing;

    @Column(name = "call_to_action", columnDefinition = "TEXT")
    private String callToAction;

    // Senaryo versiyonu (aynı proje için birden fazla senaryo üretilebilir)
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
