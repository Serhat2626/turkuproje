package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-001: Knowledge Item bilgisini AI context'e taşıyan payload.
 * ContentAgentRequest içinde gömülüdür.
 */
@Value
@Builder
public class ContentAgentKnowledgeItemPayload {

    /** Knowledge Item referansı */
    UUID id;

    /** Kültürel konu başlığı */
    String title;

    /** Ek açıklama */
    String description;

    /** Kültürel kategori */
    String category;

    /** Şehir, ilçe veya lokasyon bilgisi */
    String location;

    /** Admin / editör notu */
    String notes;
}
