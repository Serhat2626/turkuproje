package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-007: Image generation request yaşam döngüsü durumu.
 *
 * <p>Bir image generation talebi oluşturulduğunda bu lifecycle'ı takip eder.
 * AI Platform boundary tarafından üretilebilen statüler burada tanımlanmıştır.</p>
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>REQUESTED → GENERATING → GENERATED_PENDING_REVIEW veya FAILED akışı izlenir.</li>
 *   <li>GENERATED_PENDING_REVIEW, üretimin başarılı olduğunu ve human review beklediğini ifade eder.</li>
 *   <li>Bu enum APPROVED/REJECTED içermez — bunlar {@link GeneratedAssetStatus} tarafındadır.</li>
 *   <li>READY_FOR_PUBLISH bu enum'da yoktur.</li>
 *   <li>COMPLETED kullanılmaz; yerine GENERATED_PENDING_REVIEW kullanılır.</li>
 * </ul>
 */
public enum ImageGenerationRequestStatus {

    /** Üretim talebi oluşturuldu, provider'a henüz gönderilmedi */
    REQUESTED,

    /** Provider'a gönderildi, yanıt bekleniyor */
    GENERATING,

    /** Üretim başarılı — görsel üretildi, human review bekliyor */
    GENERATED_PENDING_REVIEW,

    /** Üretim başarısız — normalized error kaydedildi */
    FAILED
}
