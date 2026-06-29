package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-001: AI response validation sonucu.
 * AI response alındıktan sonra draft'a dönüştürülmeden önce validation yapılır.
 */
@Value
@Builder
public class ContentAgentValidationResult {

    /** Response geçerli mi */
    boolean valid;

    /** Validation hataları */
    List<ValidationEntry> errors;

    /** Validation uyarıları */
    List<ValidationEntry> warnings;

    /** Çalıştırılan validation kuralları */
    List<String> checkedRules;

    /** Validation zamanı */
    LocalDateTime validatedAt;

    /**
     * Validation hata/uyarı öğesi.
     */
    @Value
    @Builder
    public static class ValidationEntry {

        /** Kural kodu (ör. TITLE_REQUIRED, BODY_MIN_LENGTH_100) */
        String code;

        /** Açıklama mesajı */
        String message;
    }
}
