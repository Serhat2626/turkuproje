package com.anadoluatlasi.backend.content.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: AI draft retry isteği DTO'su.
 * Şu an ek alan gerektirmez ama genişletilebilirlik için DTO olarak tanımlı.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryAiDraftRequest {

    /** Opsiyonel retry notu */
    private String note;
}
