package com.anadoluatlasi.backend.review;

/**
 * FP-015 Task 03: Safe admin request DTO for internal revision request endpoint.
 *
 * <p>targetType and targetId are provided via path variables — NOT in the body.</p>
 *
 * <p>Does NOT contain:</p>
 * <ul>
 *   <li>publicUrl / signedUrl / cdnUrl</li>
 *   <li>providerUrl / storageKey / bucket / objectPath</li>
 *   <li>rawPayload / base64 / bytes</li>
 *   <li>secret / apiKey / accessKey / token</li>
 *   <li>publishStatus / exportStatus</li>
 *   <li>stackTrace / exceptionClass</li>
 * </ul>
 */
public final class AdminInternalRevisionRequest {

    private ReviewActorRole actorRole;
    private String actorReference;
    private String safeReasonCode;
    private String safeMessage;

    public AdminInternalRevisionRequest() {}

    public ReviewActorRole getActorRole() { return actorRole; }
    public void setActorRole(ReviewActorRole actorRole) { this.actorRole = actorRole; }

    public String getActorReference() { return actorReference; }
    public void setActorReference(String actorReference) { this.actorReference = actorReference; }

    public String getSafeReasonCode() { return safeReasonCode; }
    public void setSafeReasonCode(String safeReasonCode) { this.safeReasonCode = safeReasonCode; }

    public String getSafeMessage() { return safeMessage; }
    public void setSafeMessage(String safeMessage) { this.safeMessage = safeMessage; }

    /**
     * Safe toString — no secrets, no URLs, no storage keys.
     */
    @Override
    public String toString() {
        return "AdminInternalRevisionRequest{actorRole=" + actorRole
                + ", actorReference=" + actorReference
                + ", hasMessage=" + (safeMessage != null && !safeMessage.isBlank())
                + ", hasReasonCode=" + (safeReasonCode != null && !safeReasonCode.isBlank())
                + '}';
    }
}
