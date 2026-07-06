package com.anadoluatlasi.backend.review;

/**
 * FP-018 Task 02: Safe actor resolution exception.
 *
 * <p>Thrown when actor resolution fails. Messages are safe — no token,
 * JWT, session, credential, password, stack trace, or principal
 * in user/admin-facing context.</p>
 */
public class AdminActorResolutionException extends RuntimeException {

    private final String safeErrorCode;
    private final ReviewActorRole requestedRole;

    public AdminActorResolutionException(String safeMessage, String safeErrorCode, ReviewActorRole requestedRole) {
        super(safeMessage);
        this.safeErrorCode = safeErrorCode;
        this.requestedRole = requestedRole;
    }

    public String getSafeErrorCode() { return safeErrorCode; }
    public ReviewActorRole getRequestedRole() { return requestedRole; }

    /**
     * Safe toString — no token, credential, session, or stack trace details.
     */
    @Override
    public String toString() {
        return "AdminActorResolutionException{" +
                "safeErrorCode='" + safeErrorCode + '\'' +
                ", requestedRole=" + requestedRole +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
