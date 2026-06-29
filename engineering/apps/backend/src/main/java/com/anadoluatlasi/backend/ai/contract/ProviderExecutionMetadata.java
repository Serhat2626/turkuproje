package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * FP-001: AI provider çağrısına dair izlenebilir metadata.
 * Product akışına bağımlı değildir — debugging, maliyet ve kalite izleme için kullanılır.
 */
@Value
@Builder
public class ProviderExecutionMetadata {

    /** Kullanılan provider (ör. openai, anthropic, mock) */
    String providerName;

    /** Kullanılan model (ör. gpt-4o, claude-3) */
    String modelName;

    /** Çağrı süresi (milisaniye) */
    Long durationMs;

    /** Token kullanım bilgisi (opsiyonel, ör. "input:500,output:300") */
    String tokenUsage;

    /** Raw response ayrıca saklandı mı */
    Boolean rawResponseStored;

    /** Kullanılan prompt versiyonu */
    String promptVersion;

    /** Yaklaşık maliyet bilgisi */
    BigDecimal costEstimate;
}
