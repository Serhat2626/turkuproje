package com.anadoluatlasi.backend.review;

/**
 * FP-018 Task 02: Admin actor resolution request.
 *
 * <p>Internal object for requesting actor resolution.
 * Contains the client-provided role/reference and resolution flags.
 * No token, JWT, session, credential exposure.</p>
 */
public final class AdminActorResolutionRequest {

    private final ReviewActorRole requestedActorRole;
    private final String requestedActorReference;
    private final boolean fallbackAllowed;
    private final boolean systemInternalAllowed;
    private final String safeReasonCode;

    private AdminActorResolutionRequest(Builder builder) {
        this.requestedActorRole = builder.requestedActorRole;
        this.requestedActorReference = builder.requestedActorReference;
        this.fallbackAllowed = builder.fallbackAllowed;
        this.systemInternalAllowed = builder.systemInternalAllowed;
        this.safeReasonCode = builder.safeReasonCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ReviewActorRole getRequestedActorRole() { return requestedActorRole; }
    public String getRequestedActorReference() { return requestedActorReference; }
    public boolean isFallbackAllowed() { return fallbackAllowed; }
    public boolean isSystemInternalAllowed() { return systemInternalAllowed; }
    public String getSafeReasonCode() { return safeReasonCode; }

    /**
     * Safe toString — no token, credential, or security context.
     */
    @Override
    public String toString() {
        return "AdminActorResolutionRequest{" +
                "requestedActorRole=" + requestedActorRole +
                ", hasReference=" + (requestedActorReference != null && !requestedActorReference.isBlank()) +
                ", fallbackAllowed=" + fallbackAllowed +
                ", systemInternalAllowed=" + systemInternalAllowed +
                ", safeReasonCode='" + safeReasonCode + '\'' +
                '}';
    }

    public static final class Builder {
        private ReviewActorRole requestedActorRole;
        private String requestedActorReference;
        private boolean fallbackAllowed = true;
        private boolean systemInternalAllowed = false;
        private String safeReasonCode;

        private Builder() {}

        public Builder requestedActorRole(ReviewActorRole role) { this.requestedActorRole = role; return this; }
        public Builder requestedActorReference(String ref) { this.requestedActorReference = ref; return this; }
        public Builder fallbackAllowed(boolean allowed) { this.fallbackAllowed = allowed; return this; }
        public Builder systemInternalAllowed(boolean allowed) { this.systemInternalAllowed = allowed; return this; }
        public Builder safeReasonCode(String code) { this.safeReasonCode = code; return this; }

        public AdminActorResolutionRequest build() {
            if (requestedActorRole == null) throw new IllegalArgumentException("requestedActorRole is required");
            return new AdminActorResolutionRequest(this);
        }
    }
}
