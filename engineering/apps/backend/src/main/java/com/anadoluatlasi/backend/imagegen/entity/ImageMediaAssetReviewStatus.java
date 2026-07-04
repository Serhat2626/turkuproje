package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-007: Image media asset review lifecycle durumu.
 *
 * <p>Human review sonucuna göre atanır.
 * Bu status yalnızca explicit admin review action ile değişir.</p>
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>REVIEW_APPROVED yalnızca admin approve action sonrası atanır.</li>
 *   <li>REVIEW_REJECTED yalnızca admin reject action sonrası atanır.</li>
 *   <li>Otomatik status geçişi yoktur.</li>
 *   <li>READY_FOR_PUBLISH bu enum'da yoktur.</li>
 *   <li>Generic status update ile geçiş yapılmaz.</li>
 * </ul>
 */
public enum ImageMediaAssetReviewStatus {

    /** Admin tarafından onaylandı — yalnızca human review sonrası */
    REVIEW_APPROVED,

    /** Admin tarafından reddedildi — yalnızca human review sonrası */
    REVIEW_REJECTED
}
