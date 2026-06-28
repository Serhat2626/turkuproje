package com.anadoluatlasi.backend.application.knowledge;

import lombok.Builder;
import lombok.Value;

/**
 * FP-001: Yeni bir bilgi öğesi oluşturmak için gerekli giriş verisi.
 * İş kuralı içermez, yalnızca veri taşır.
 */
@Value
@Builder
public class CreateKnowledgeItemCommand {

    /** Başlık (zorunlu) */
    String title;

    /** Bilgi öğesi türü (zorunlu, KnowledgeItemType enum adı) */
    String type;

    /** Dil kodu (zorunlu, ör. "tr") */
    String language;

    /** Kısa özet */
    String summary;

    /** FP-001: Detaylı açıklama */
    String description;

    /** FP-001: Kategori */
    String category;

    /** FP-001: İl */
    String city;

    /** FP-001: İlçe */
    String district;

    /** FP-001: Lokasyon metni */
    String locationText;

    /** FP-001: Editör notları */
    String notes;
}
