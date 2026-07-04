package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-008: Storage handoff lifecycle durumu.
 *
 * <p>Provider output'un server-side storage'a aktarılma sürecini izler.
 * Bu durum review status değildir ve approval anlamına gelmez.</p>
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>STORAGE_STORED olması GeneratedAsset'i otomatik APPROVED yapmaz.</li>
 *   <li>Storage status, {@link GeneratedAssetStatus} ile bağımsız concern'dır.</li>
 *   <li>READY_FOR_PUBLISH bu enum'da yoktur.</li>
 *   <li>COMPLETED bu enum'da yoktur.</li>
 *   <li>Generic status update endpoint ile değiştirilemez.</li>
 * </ul>
 */
public enum ImageStorageHandoffStatus {

    /** Handoff henüz başlamadı veya devam ediyor */
    STORAGE_PENDING,

    /** MIME/size/dimension validation sürüyor */
    STORAGE_VALIDATING,

    /** Başarıyla depolandı — preview hazır */
    STORAGE_STORED,

    /** Handoff başarısız — normalized error kaydedildi */
    STORAGE_FAILED
}
