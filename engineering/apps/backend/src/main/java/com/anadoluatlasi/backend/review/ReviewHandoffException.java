package com.anadoluatlasi.backend.review;

/**
 * FP-013 Task 02: Safe exception for invalid handoff request or unsafe state.
 *
 * <p>Safe message only — no provider URL, storage key, raw payload, base64,
 * secret, config, bucket/path, stack trace intended for API/admin display.</p>
 */
public class ReviewHandoffException extends RuntimeException {

    private final String safeErrorCode;

    public ReviewHandoffException(String message, String safeErrorCode) {
        super(message);
        this.safeErrorCode = safeErrorCode;
    }

    public String getSafeErrorCode() {
        return safeErrorCode;
    }

    @Override
    public String toString() {
        return "ReviewHandoffException{errorCode=" + safeErrorCode + "}";
    }
}
