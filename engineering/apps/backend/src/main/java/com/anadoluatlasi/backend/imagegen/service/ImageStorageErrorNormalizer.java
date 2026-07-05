package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;

/**
 * FP-008: Safe storage/preview error message normalization helper.
 *
 * <p>Raw exception message, provider/storage URL, storage key, base64,
 * secret/API key bilgileri admin-facing hata mesajlarına sızmaz.
 * Bu helper FP-008 storage handoff ve preview error message
 * oluşturma noktalarında kullanılır.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Raw exception message doğrudan DTO'ya yazılmaz</li>
 *   <li>Storage object key/reference/bucket bilgisi içermez</li>
 *   <li>Provider URL/signed URL/public URL/CDN URL içermez</li>
 *   <li>Base64/binary data içermez</li>
 *   <li>API key/secret/token/password içermez</li>
 *   <li>Stack trace dump içermez</li>
 *   <li>Internal prompt/config bilgisi içermez</li>
 * </ul>
 */
public final class ImageStorageErrorNormalizer {

    private ImageStorageErrorNormalizer() {
    }

    /** Maksimum safe error message uzunluğu */
    private static final int MAX_SAFE_MESSAGE_LENGTH = 200;

    /**
     * ImageStorageErrorCode için safe, normalized error message üretir.
     * Raw exception message/provider/storage payload kullanılmaz.
     */
    public static String safeMessage(ImageStorageErrorCode errorCode) {
        if (errorCode == null) {
            return "Bilinmeyen depolama hatası oluştu";
        }
        return switch (errorCode) {
            case PROVIDER_OUTPUT_UNAVAILABLE ->
                    "Üretilen görsel kaynağına erişilemiyor";
            case PROVIDER_OUTPUT_EXPIRED ->
                    "Üretilen görselin geçerlilik süresi dolmuş";
            case PROVIDER_OUTPUT_DOWNLOAD_FAILED ->
                    "Üretilen görsel indirilemedi";
            case MEDIA_VALIDATION_FAILED ->
                    "Medya doğrulama başarısız";
            case UNSUPPORTED_MEDIA_TYPE ->
                    "Desteklenmeyen dosya formatı";
            case MEDIA_TOO_LARGE ->
                    "Dosya boyutu izin verilen sınırı aşıyor";
            case MEDIA_DIMENSION_INVALID ->
                    "Görsel boyutları geçersiz veya okunamıyor";
            case CHECKSUM_CALCULATION_FAILED ->
                    "Dosya bütünlük kontrolü hesaplanamadı";
            case STORAGE_WRITE_FAILED ->
                    "Depolama yazma işlemi başarısız";
            case STORAGE_READ_FAILED ->
                    "Depolama okuma işlemi başarısız";
            case PREVIEW_NOT_AVAILABLE ->
                    "Önizleme mevcut değil";
            case PREVIEW_ACCESS_DENIED ->
                    "Önizleme erişim yetkisi yok";
            case UNKNOWN_STORAGE_HANDOFF_FAILURE ->
                    "Depolama aktarımı sırasında beklenmeyen hata oluştu";
        };
    }

    /**
     * Error code ve opsiyonel safe context ile message oluşturur.
     * Context sanitize edilir — raw payload/URL/secret/key içermez.
     */
    public static String safeMessage(ImageStorageErrorCode errorCode, String safeContext) {
        String base = safeMessage(errorCode);
        if (safeContext == null || safeContext.isBlank()) {
            return base;
        }
        String sanitized = sanitize(safeContext);
        return base + " — " + sanitized;
    }

    /**
     * Raw exception/error message'dan safe admin/log context üretir.
     * Forbidden keyword ve pattern'ler maskelenir, uzunluk sınırlanır.
     */
    public static String sanitize(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Detay mevcut değil";
        }

        String sanitized = rawMessage;

        // 1. API key/secret/token/password
        sanitized = sanitized.replaceAll(
                "(?i)(api[_-]?key|secret|token|password|bearer|credential)\\s*[:=]\\s*\\S+",
                "[REDACTED]");

        // 2. HTTP/HTTPS URLs
        sanitized = sanitized.replaceAll("(?i)(https?://\\S+)", "[URL_REDACTED]");

        // 3. Cloud storage URIs (s3://, gs://, az://)
        sanitized = sanitized.replaceAll("(?i)(s3://\\S+|gs://\\S+|az://\\S+)", "[STORAGE_REDACTED]");

        // 4. Storage path patterns (imagegen/uuid/... or bucket/key patterns)
        sanitized = sanitized.replaceAll(
                "(?i)imagegen/[a-f0-9-]+/[a-f0-9-]+", "[STORAGE_KEY_REDACTED]");

        // 5. Base64 data patterns
        sanitized = sanitized.replaceAll("(?i)(base64[,:]\\S+)", "[DATA_REDACTED]");

        // 6. Long base64-like strings (>64 chars of alphanumeric+/+=)
        sanitized = sanitized.replaceAll("[A-Za-z0-9+/=]{64,}", "[LONG_DATA_REDACTED]");

        // 7. Stack trace patterns
        sanitized = sanitized.replaceAll("(?m)^\\s+at\\s+\\S+.*$", "[STACK_REDACTED]");

        // 8. Truncate
        if (sanitized.length() > MAX_SAFE_MESSAGE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_SAFE_MESSAGE_LENGTH) + "…";
        }

        return sanitized;
    }

    /**
     * Belirli bir exception'dan safe correlationId-aware log mesajı üretir.
     * Raw exception message admin-facing result'a yazılmaz.
     *
     * @param errorCode  normalized error code
     * @param correlationId trace identifier (null-safe)
     * @return safe log context string
     */
    public static String safeLogContext(ImageStorageErrorCode errorCode, String correlationId) {
        String code = errorCode != null ? errorCode.name() : "UNKNOWN";
        String corr = correlationId != null ? correlationId : "none";
        return "errorCode=" + code + ", correlationId=" + corr;
    }
}
