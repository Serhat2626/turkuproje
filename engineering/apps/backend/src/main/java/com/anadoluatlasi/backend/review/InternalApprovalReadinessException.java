package com.anadoluatlasi.backend.review;

/**
 * FP-014 Task 02: Safe exception for internal approval readiness service.
 *
 * <p>Safe message and error code only — no provider URL, storage key,
 * raw payload, base64, secret, config, bucket/path, or stack trace
 * intended for API/admin display.</p>
 */
public class InternalApprovalReadinessException extends RuntimeException {

    private final String safeErrorCode;

    public InternalApprovalReadinessException(String message, String safeErrorCode) {
        super(message);
        this.safeErrorCode = safeErrorCode;
    }

    public String getSafeErrorCode() {
        return safeErrorCode;
    }

    @Override
    public String toString() {
        return "InternalApprovalReadinessException{errorCode=" + safeErrorCode + "}";
    }
}
