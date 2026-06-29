package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

/**
 * FP-001: Revizyon sürecinde önceki draft bilgisini AI context'e taşıyan payload.
 * AI'ın önceki üretimini bilmesini ve farklı bir yaklaşım denemesini sağlar.
 */
@Value
@Builder
public class PreviousDraftPayload {

    /** Önceki draft referansı */
    String draftId;

    /** Önceki draft başlığı */
    String title;

    /** Önceki draft metni */
    String body;

    /** Önceki draft özeti */
    String summary;

    /** Önceki draft statüsü */
    String status;
}
