package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * FP-001: Retry davranışını izlemek için kullanılan metadata.
 * Kaçıncı deneme olduğu, önceki hata bilgisi ve retry limitini taşır.
 */
@Value
@Builder
public class ContentAgentRetryMetadata {

    /** Mevcut deneme sayısı (1-based) */
    int attemptNumber;

    /** Maksimum deneme sayısı */
    int maxAttempts;

    /** Önceki hata tipi (AiErrorType enum adı) */
    String previousErrorType;

    /** Son deneme zamanı */
    LocalDateTime lastAttemptAt;

    /** Manuel müdahale gerekiyor mu */
    boolean manualRetryRequired;
}
