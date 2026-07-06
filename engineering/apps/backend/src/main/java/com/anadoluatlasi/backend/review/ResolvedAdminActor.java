package com.anadoluatlasi.backend.review;

import java.time.LocalDateTime;

/**
 * FP-018 Task 02: Resolved admin actor value object.
 *
 * <p>Immutable record of a resolved actor identity. Created by
 * {@link AdminActorResolver}. No token, JWT, session, credential,
 * password, principal (raw), or authorization header exposure.</p>
 */
public final class ResolvedAdminActor {

    private final ReviewActorRole actorRole;
    private final String actorReference;
    private final AdminActorResolutionMode resolutionMode;
    private final String safeDisplayName;
    private final String safeReasonCode;
    private final String safeMessage;
    private final LocalDateTime resolvedAt;

    private ResolvedAdminActor(Builder builder) {
        this.actorRole = builder.actorRole;
        this.actorReference = builder.actorReference;
        this.resolutionMode = builder.resolutionMode;
        this.safeDisplayName = builder.safeDisplayName;
        this.safeReasonCode = builder.safeReasonCode;
        this.safeMessage = builder.safeMessage;
        this.resolvedAt = builder.resolvedAt != null ? builder.resolvedAt : LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public ReviewActorRole getActorRole() { return actorRole; }
    public String getActorReference() { return actorReference; }
    public AdminActorResolutionMode getResolutionMode() { return resolutionMode; }
    public String getSafeDisplayName() { return safeDisplayName; }
    public String getSafeReasonCode() { return safeReasonCode; }
    public String getSafeMessage() { return safeMessage; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }

    /**
     * Safe toString — no token, JWT, session, credential, password,
     * principal (raw), authorization header, or stack trace.
     */
    @Override
    public String toString() {
        return "ResolvedAdminActor{" +
                "actorRole=" + actorRole +
                ", actorReference='" + actorReference + '\'' +
                ", resolutionMode=" + resolutionMode +
                ", safeDisplayName='" + safeDisplayName + '\'' +
                ", safeReasonCode='" + safeReasonCode + '\'' +
                '}';
    }

    public static final class Builder {
        private ReviewActorRole actorRole;
        private String actorReference;
        private AdminActorResolutionMode resolutionMode;
        private String safeDisplayName;
        private String safeReasonCode;
        private String safeMessage;
        private LocalDateTime resolvedAt;

        private Builder() {}

        public Builder actorRole(ReviewActorRole actorRole) { this.actorRole = actorRole; return this; }
        public Builder actorReference(String actorReference) { this.actorReference = actorReference; return this; }
        public Builder resolutionMode(AdminActorResolutionMode resolutionMode) { this.resolutionMode = resolutionMode; return this; }
        public Builder safeDisplayName(String safeDisplayName) { this.safeDisplayName = safeDisplayName; return this; }
        public Builder safeReasonCode(String safeReasonCode) { this.safeReasonCode = safeReasonCode; return this; }
        public Builder safeMessage(String safeMessage) { this.safeMessage = safeMessage; return this; }
        public Builder resolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; return this; }

        public ResolvedAdminActor build() {
            if (actorRole == null) throw new IllegalArgumentException("actorRole is required");
            if (resolutionMode == null) throw new IllegalArgumentException("resolutionMode is required");
            return new ResolvedAdminActor(this);
        }
    }
}
