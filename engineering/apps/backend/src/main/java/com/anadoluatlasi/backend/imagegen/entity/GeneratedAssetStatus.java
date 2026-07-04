package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-007: Generated asset yaşam döngüsü durumu.
 *
 * <p>Image generation sonucu oluşturulan asset'in review lifecycle'ı.
 * Bu statüler yalnızca human review kararı ile değişir.</p>
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>İlk oluşturulduğunda mutlaka GENERATED_PENDING_REVIEW olmalıdır.</li>
 *   <li>APPROVED yalnızca admin tarafından explicit human review sonrası atanır.</li>
 *   <li>REJECTED yalnızca admin tarafından explicit human review sonrası atanır.</li>
 *   <li>AI output hiçbir zaman otomatik APPROVED olamaz.</li>
 *   <li>READY_FOR_PUBLISH bu enum'da yoktur.</li>
 *   <li>Generic status update ile geçiş yapılmaz.</li>
 * </ul>
 */
public enum GeneratedAssetStatus {

    /** Üretildi, human review bekliyor — AI output bu durumda başlar */
    GENERATED_PENDING_REVIEW,

    /** Admin tarafından onaylandı — yalnızca human review sonrası */
    APPROVED,

    /** Admin tarafından reddedildi — yalnızca human review sonrası */
    REJECTED
}
