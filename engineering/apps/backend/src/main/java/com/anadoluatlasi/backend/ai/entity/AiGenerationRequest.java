package com.anadoluatlasi.backend.ai.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import com.anadoluatlasi.backend.content.entity.ContentProject;
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

import java.time.LocalDateTime;

/**
 * FP-001 AI üretim isteği entity'si.
 * Her AI agent çağrısının izlenebilir kaydını tutar.
 * Bir ContentProject'e bağlıdır.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_generation_requests")
public class AiGenerationRequest extends BaseEntity {

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

    /** İşlem tamamlanma zamanı */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
