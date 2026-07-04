package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-008: Storage handoff ve preview error code'ları.
 *
 * <p>Storage handoff ve preview proxy akışında oluşabilecek hata
 * kategorilerinin safe normalized code'ları. Raw provider/storage
 * error dump admin-facing response'a dönmez.</p>
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>Raw provider/storage error dump gösterilmez.</li>
 *   <li>Stack trace admin-facing değildir.</li>
 *   <li>Storage object key error response'a çıkmaz.</li>
 *   <li>Provider URL error response'a çıkmaz.</li>
 *   <li>Signed/public URL error response'a çıkmaz.</li>
 * </ul>
 */
public enum ImageStorageErrorCode {

    // ── Provider Output Retrieval ───────────────────────────────────

    /** Provider output reference erişilemez */
    PROVIDER_OUTPUT_UNAVAILABLE,

    /** Provider output TTL dolmuş */
    PROVIDER_OUTPUT_EXPIRED,

    /** Provider output download network/IO hatası */
    PROVIDER_OUTPUT_DOWNLOAD_FAILED,

    // ── Media Validation ────────────────────────────────────────────

    /** Genel media validation hatası */
    MEDIA_VALIDATION_FAILED,

    /** MIME type allowlist dışında */
    UNSUPPORTED_MEDIA_TYPE,

    /** Dosya boyutu limiti aşıldı */
    MEDIA_TOO_LARGE,

    /** Width/height sınır dışı veya okunamıyor */
    MEDIA_DIMENSION_INVALID,

    /** Checksum/hash hesaplanamadı */
    CHECKSUM_CALCULATION_FAILED,

    // ── Storage Operations ──────────────────────────────────────────

    /** Object store'a yazma başarısız */
    STORAGE_WRITE_FAILED,

    /** Object store'dan okuma başarısız */
    STORAGE_READ_FAILED,

    // ── Preview ─────────────────────────────────────────────────────

    /** Preview henüz hazır değil veya storage failed */
    PREVIEW_NOT_AVAILABLE,

    /** Preview authorization başarısız */
    PREVIEW_ACCESS_DENIED,

    // ── Catch-all ───────────────────────────────────────────────────

    /** Sınıflandırılamayan storage handoff hatası */
    UNKNOWN_STORAGE_HANDOFF_FAILURE
}
