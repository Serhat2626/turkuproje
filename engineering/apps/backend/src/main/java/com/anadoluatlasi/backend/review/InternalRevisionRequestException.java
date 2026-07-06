package com.anadoluatlasi.backend.review;

/**
 * FP-015 Task 02: Safe exception for internal revision request failures.
 *
 * <p>Contains safe error code and safe message only.
 * No stack traces, no provider/storage details, no raw payloads.</p>
 */
public class InternalRevisionRequestException extends RuntimeException {

    private final String safeErrorCode;

    public InternalRevisionRequestException(String message, String safeErrorCode) {
        super(message);
        this.safeErrorCode = safeErrorCode;
    }

    public String getSafeErrorCode() {
        return safeErrorCode;
    }

    /**
     * Safe toString — no secrets, no URLs, no storage keys, no stack traces.
     */
    @Override
    public String toString() {
        return "InternalRevisionRequestException{safeErrorCode=" + safeErrorCode
                + ", message=" + getMessage()
                + '}';
    }
}
