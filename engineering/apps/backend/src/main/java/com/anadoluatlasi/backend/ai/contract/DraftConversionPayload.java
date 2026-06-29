package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * FP-001: Geçerli AI response'un Content Draft'a dönüştürülmesi için ara yapı.
 * Validated response → ContentDraft entity dönüşümünde kullanılır.
 */
@Value
@Builder
public class DraftConversionPayload {

    /** Content Project referansı */
    UUID projectId;

    /** AI request referansı */
    UUID sourceRequestId;

    /** Draft başlığı */
    String title;

    /** Draft metni */
    String body;

    /** Draft özeti */
    String summary;

    /** Etiket önerileri */
    List<String> suggestedTags;

    /** Editör uyarıları (JSON-ready) */
    List<ContentAgentResponse.ContentAgentWarning> warnings;

    /** Üretici tipi (GeneratedBy enum adı) — default: AI_CONTENT_AGENT */
    String generatedBy;

    /** İlk draft statüsü (ContentDraftStatus enum adı) — default: GENERATED */
    String initialStatus;
}
