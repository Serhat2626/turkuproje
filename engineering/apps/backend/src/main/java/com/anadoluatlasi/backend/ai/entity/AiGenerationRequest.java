package com.anadoluatlasi.backend.ai.entity;

import com.anadoluatlasi.backend.content.entity.ContentProject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001 AI üretim isteği entity'si.
 * Her AI agent çağrısının izlenebilir kaydını tutar.
 * Bir ContentProject'e bağlıdır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_generation_requests")
@EntityListeners(AuditingEntityListener.class)
public class AiGenerationRequest {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Bu isteğin ait olduğu içerik projesi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id", nullable = false)
    private ContentProject contentProject;

    /** Deneme numarası (1-based) */
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    /** İstek durumu */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AiGenerationStatus status;

    /** AI'a gönderilen istek payload'u (JSON) */
    @Column(name = "request_payload_json", columnDefinition = "TEXT")
    private String requestPayloadJson;

    /** AI'dan alınan yanıt payload'u (JSON) */
    @Column(name = "response_payload_json", columnDefinition = "TEXT")
    private String responsePayloadJson;

    /** Hata tipi (başarısız ise) */
    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", length = 50)
    private AiErrorType errorType;

    /** Hata mesajı (başarısız ise) */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** AI sağlayıcı adı (ör. openai, anthropic, mock) */
    @Column(name = "provider_name", length = 100)
    private String providerName;

    /** Kullanılan model adı (ör. gpt-4o, claude-3) */
    @Column(name = "model_name", length = 100)
    private String modelName;

    /** İşlem süresi (milisaniye) */
    @Column(name = "duration_ms")
    private Long durationMs;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** İşlem tamamlanma zamanı */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
