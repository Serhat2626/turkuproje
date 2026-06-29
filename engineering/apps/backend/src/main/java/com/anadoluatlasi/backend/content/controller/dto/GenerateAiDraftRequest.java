package com.anadoluatlasi.backend.content.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: AI draft üretimi isteği DTO'su.
 * Opsiyonel insan geri bildirimi taşır (revision durumunda).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAiDraftRequest {

    /** Revizyon notu (opsiyonel, revision durumunda kullanılır) */
    private String humanFeedbackNote;

    /** Revizyon türü (opsiyonel, ör. MAKE_SHORTER, CUSTOM) */
    private String feedbackType;
}
