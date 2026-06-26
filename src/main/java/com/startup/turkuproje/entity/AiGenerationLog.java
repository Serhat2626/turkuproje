package com.startup.turkuproje.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

// AI çağrı logları - maliyet takibi ve debug için
@Entity
@Table(name = "ai_generation_logs")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // İlişkili proje
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Prompt tipi (hangi aşama için üretim yapıldı)
    @Enumerated(EnumType.STRING)
    @Column(name = "prompt_type", nullable = false)
    private PromptType promptType;

    // Gönderilen prompt
    @Column(name = "prompt_sent", columnDefinition = "TEXT", nullable = false)
    private String promptSent;

    // Alınan yanıt
    @Column(name = "response_received", columnDefinition = "TEXT")
    private String responseReceived;

    // Kullanılan model (ör: gpt-4o)
    @Column
    private String model;

    // Token kullanım bilgisi
    @Column(name = "token_usage")
    private Integer tokenUsage;

    // İşlem süresi (milisaniye)
    @Column(name = "duration_ms")
    private Long durationMs;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
