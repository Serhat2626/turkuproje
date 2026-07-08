package com.anadoluatlasi.backend.turkuvideo;

/**
 * RPE-002: Credential vault boundary placeholder.
 * Only presence status — never raw credential/token/secret.
 */
public final class TurkuCredentialVaultBoundary {
    private final String vaultLabel;
    private final boolean credentialPresent;
    private final String presenceStatusLabel;

    public TurkuCredentialVaultBoundary(String vaultLabel, boolean credentialPresent, String presenceStatusLabel) {
        this.vaultLabel = vaultLabel;
        this.credentialPresent = credentialPresent;
        this.presenceStatusLabel = presenceStatusLabel;
    }

    public String getVaultLabel() { return vaultLabel; }
    public boolean isCredentialPresent() { return credentialPresent; }
    public String getPresenceStatusLabel() { return presenceStatusLabel; }
}
