package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 02: Safe exception for türkü video project operations.
 *
 * <p>Never exposes tokens, credentials, stack traces, or internal data
 * in message or toString().</p>
 */
public class TurkuVideoProjectException extends RuntimeException {

    private final String safeErrorCode;

    public TurkuVideoProjectException(String message, String safeErrorCode) {
        super(message);
        this.safeErrorCode = safeErrorCode;
    }

    public String getSafeErrorCode() { return safeErrorCode; }

    @Override
    public String toString() {
        return "TurkuVideoProjectException{code='" + safeErrorCode + "', message='" + getMessage() + "'}";
    }

    // ── Factory methods ──

    public static TurkuVideoProjectException turkuNameRequired() {
        return new TurkuVideoProjectException("Türkü adı gereklidir", "TURKU_NAME_REQUIRED");
    }

    public static TurkuVideoProjectException turkuNameTooLong() {
        return new TurkuVideoProjectException("Türkü adı çok uzun (maks. 200 karakter)", "TURKU_NAME_TOO_LONG");
    }

    public static TurkuVideoProjectException unsafeInput(String fieldName) {
        return new TurkuVideoProjectException(
                "Güvenli olmayan girdi: " + fieldName, "UNSAFE_INPUT");
    }

    public static TurkuVideoProjectException fieldTooLong(String fieldName, int maxLength) {
        return new TurkuVideoProjectException(
                fieldName + " çok uzun (maks. " + maxLength + " karakter)", "FIELD_TOO_LONG");
    }

    public static TurkuVideoProjectException projectNotFound() {
        return new TurkuVideoProjectException("Proje bulunamadı", "PROJECT_NOT_FOUND");
    }

    public static TurkuVideoProjectException invalidDuration() {
        return new TurkuVideoProjectException(
                "Hedef süre 1-30 dakika arasında olmalıdır", "INVALID_DURATION");
    }

    public static TurkuVideoProjectException invalidStageTransition(
            TurkuVideoWorkflowStage current, TurkuVideoWorkflowStage target) {
        return new TurkuVideoProjectException(
                "Geçersiz aşama geçişi: " + current + " → " + target,
                "INVALID_STAGE_TRANSITION");
    }

    public static TurkuVideoProjectException missingPrerequisite(String detail) {
        return new TurkuVideoProjectException(
                "Ön koşul eksik: " + detail, "MISSING_PREREQUISITE");
    }
}
