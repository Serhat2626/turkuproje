package com.anadoluatlasi.backend.review;

/**
 * FP-013 Task 03: Safe exception for missing/invalid preview target.
 *
 * <p>Safe message only — no provider URL, storage key, raw payload, base64,
 * secret, config, bucket/path, stack trace intended for API/admin display.</p>
 */
public class AdminPreviewException extends RuntimeException {

    private final String safeErrorCode;

    public AdminPreviewException(String message, String safeErrorCode) {
        super(message);
        this.safeErrorCode = safeErrorCode;
    }

    public String getSafeErrorCode() {
        return safeErrorCode;
    }

    @Override
    public String toString() {
        return "AdminPreviewException{errorCode=" + safeErrorCode + "}";
    }
}
