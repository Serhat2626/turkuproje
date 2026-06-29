package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * FP-001: AI Content Agent'ın normalize edilmiş başarılı yanıt contract'ı.
 * Provider raw response değildir — AI Platform tarafından normalize edilmiştir.
 */
@Value
@Builder
public class ContentAgentResponse {

    /** İlgili AI request referansı */
    UUID requestId;

    /** Content Project referansı */
    UUID projectId;

    /** Önerilen içerik başlığı */
    String title;

    /** Ana içerik metni */
    String body;

    /** Kısa özet */
    String summary;

    /** Önerilen etiketler */
    List<String> suggestedTags;

    /** İnsan editör için dikkat notları */
    List<ContentAgentWarning> warnings;

    /** AI'ın iç kalite değerlendirme notları */
    String qualityNotes;

    /** AI'ın üretim yaklaşımına dair kısa açıklama */
    String generationReason;

    /** Provider çalıştırma bilgisi */
    ProviderExecutionMetadata providerMetadata;

    /**
     * AI yanıtındaki uyarı öğesi.
     */
    @Value
    @Builder
    public static class ContentAgentWarning {

        /** Uyarı kodu (ör. NEEDS_SOURCE_CHECK, UNCERTAIN_HISTORICAL_DATE) */
        String code;

        /** İnsan tarafından anlaşılabilir uyarı mesajı */
        String message;
    }
}
