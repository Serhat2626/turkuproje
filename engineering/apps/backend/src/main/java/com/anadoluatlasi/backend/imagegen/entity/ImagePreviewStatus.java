package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-008: Preview erişilebilirlik durumu.
 *
 * <p>Admin preview proxy üzerinden görselin görüntülenebilirlik durumunu ifade eder.
 * Bu durum approval/review status değildir.</p>
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>PREVIEW_AVAILABLE olması GeneratedAsset'i otomatik APPROVED yapmaz.</li>
 *   <li>Preview status, {@link GeneratedAssetStatus} ile bağımsız concern'dır.</li>
 *   <li>READY_FOR_PUBLISH bu enum'da yoktur.</li>
 *   <li>COMPLETED bu enum'da yoktur.</li>
 *   <li>Preview access, review decision üretmez.</li>
 * </ul>
 */
public enum ImagePreviewStatus {

    /** Storage henüz tamamlanmamış veya başarısız — preview erişilemez */
    PREVIEW_UNAVAILABLE,

    /** Storage tamamlandı — preview backend proxy üzerinden erişilebilir */
    PREVIEW_AVAILABLE
}
