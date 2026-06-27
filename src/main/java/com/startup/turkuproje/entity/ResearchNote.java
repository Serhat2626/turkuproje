package com.startup.turkuproje.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

// Konu hakkında AI araştırma çıktısı
@Entity
@Table(name = "research_notes")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Tam araştırma metni
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Kısa özet
    @Column(columnDefinition = "TEXT")
    private String summary;

    // Yöresi / coğrafi bağlam
    @Column
    private String region;

    // Tarihsel dönem
    @Column(name = "historical_period")
    private String historicalPeriod;

    // Duygu tonu
    @Column(name = "emotional_tone")
    private String emotionalTone;

    // Belirsiz/riskli bilgiler
    @Column(name = "uncertainties", columnDefinition = "TEXT")
    private String uncertainties;

    // Kullanıcı tarafından onaylandı mı?
    @Column(nullable = false)
    @Builder.Default
    private Boolean approved = false;

    // Kullanıcı notları/düzeltmeleri
    @Column(name = "user_notes", columnDefinition = "TEXT")
    private String userNotes;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
