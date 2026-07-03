package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

/**
 * FP-004: Bilgi/konu context'i input contract'ı.
 */
@Value
@Builder
public class PlatformPackageKnowledgeContextInput {

    /** Konu başlığı */
    String title;

    /** Konu açıklaması */
    String description;

    /** Şehir/bölge */
    String city;

    /** Dönem/tarih bilgisi */
    String period;
}
