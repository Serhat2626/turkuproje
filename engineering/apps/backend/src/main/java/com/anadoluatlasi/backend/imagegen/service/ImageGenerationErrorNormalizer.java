package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationErrorCode;

/**
 * FP-007: Safe error message normalization helper.
 *
 * <p>Raw provider error dump, exception stack trace, API key, prompt,
 * storage key ve URL bilgileri admin-facing hata mesajlarına sızmaz.
 * Bu helper tüm FP-007 error message oluşturma noktalarında kullanılır.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Raw exception message doğrudan DTO'ya yazılmaz</li>
 *   <li>Provider payload/response bilgisi içermez</li>
 *   <li>Internal prompt/API key/secret içermez</li>
 *   <li>Storage key/URL/bucket bilgisi içermez</li>
 *   <li>Stack trace dump içermez</li>
 * </ul>
 */
public final class ImageGenerationErrorNormalizer {

    private ImageGenerationErrorNormalizer() {
    }

    /** Maksimum safe error message uzunluğu */
    private static final int MAX_SAFE_MESSAGE_LENGTH = 200;

    /**
     * Error code'a göre safe, normalized error message üretir.
     * Raw exception message/provider payload kullanılmaz.
     */
    public static String safeMessage(ImageGenerationErrorCode errorCode) {
        if (errorCode == null) {
            return "Bilinmeyen hata oluştu";
        }
        return switch (errorCode) {
            case PROVIDER_UNAVAILABLE ->
                    "Image generation servisi şu anda erişilemez durumda";
            case PROVIDER_TIMEOUT ->
                    "Image generation isteği zaman aşımına uğradı";
            case PROVIDER_REJECTED_REQUEST ->
                    "Image generation isteği reddedildi";
            case PROVIDER_SAFETY_BLOCKED ->
                    "Image generation güvenlik politikası tarafından engellendi";
            case INVALID_GENERATION_INPUT ->
                    "Geçersiz generation parametreleri";
            case STORAGE_HANDOFF_FAILED ->
                    "Üretilen görsel depolamaya aktarılamadı";
            case UNKNOWN_GENERATION_FAILURE ->
                    "Image generation sırasında beklenmeyen bir hata oluştu";
        };
    }

    /**
     * Error code ve opsiyonel context ile safe message oluşturur.
     * Context bilgisi sanitize edilir — raw payload/URL/secret/key içermez.
     */
    public static String safeMessage(ImageGenerationErrorCode errorCode, String safeContext) {
        String base = safeMessage(errorCode);
        if (safeContext == null || safeContext.isBlank()) {
            return base;
        }
        String sanitized = sanitize(safeContext);
        return base + " — " + sanitized;
    }

    /**
     * Raw exception message'dan safe log/error context üretir.
     * Forbidden keyword'ler maskelenir, uzunluk sınırlanır.
     */
    public static String sanitize(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Detay mevcut değil";
        }

        String sanitized = rawMessage;

        // Forbidden patterns — mask anything that looks sensitive
        sanitized = sanitized.replaceAll("(?i)(api[_-]?key|secret|token|password|bearer)\\s*[:=]\\s*\\S+", "[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(https?://\\S+)", "[URL_REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(s3://\\S+|gs://\\S+)", "[STORAGE_REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(base64[,:]\\S+)", "[DATA_REDACTED]");

        // Truncate to safe max length
        if (sanitized.length() > MAX_SAFE_MESSAGE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_SAFE_MESSAGE_LENGTH) + "…";
        }

        return sanitized;
    }
}
