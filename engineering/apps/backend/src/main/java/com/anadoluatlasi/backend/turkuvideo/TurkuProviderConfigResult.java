package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-002: Image provider configuration result — audit-safe metadata.
 */
public final class TurkuProviderConfigResult {
    private final TurkuProviderConfigStatus status;
    private final List<TurkuProviderConfigIssueCode> issues;
    private final String opaqueConfigRef;
    private final String providerLabel;
    private final TurkuImageProviderType providerType;
    private final List<String> capabilityLabels;
    private final TurkuCredentialVaultBoundary vaultBoundary;
    private final String selectionPolicySummary;
    private final String fallbackPolicySummary;
    private final boolean executionEnabled;
    private final String safetyValidationSummary;

    public TurkuProviderConfigResult(
            TurkuProviderConfigStatus status, List<TurkuProviderConfigIssueCode> issues,
            String opaqueConfigRef, String providerLabel, TurkuImageProviderType providerType,
            List<String> capabilityLabels, TurkuCredentialVaultBoundary vaultBoundary,
            String selectionPolicySummary, String fallbackPolicySummary,
            boolean executionEnabled, String safetyValidationSummary) {
        this.status = status;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.opaqueConfigRef = opaqueConfigRef;
        this.providerLabel = providerLabel;
        this.providerType = providerType;
        this.capabilityLabels = capabilityLabels == null ? List.of() : List.copyOf(capabilityLabels);
        this.vaultBoundary = vaultBoundary;
        this.selectionPolicySummary = selectionPolicySummary;
        this.fallbackPolicySummary = fallbackPolicySummary;
        this.executionEnabled = executionEnabled;
        this.safetyValidationSummary = safetyValidationSummary;
    }

    public TurkuProviderConfigStatus getStatus() { return status; }
    public List<TurkuProviderConfigIssueCode> getIssues() { return issues; }
    public String getOpaqueConfigRef() { return opaqueConfigRef; }
    public String getProviderLabel() { return providerLabel; }
    public TurkuImageProviderType getProviderType() { return providerType; }
    public List<String> getCapabilityLabels() { return capabilityLabels; }
    public TurkuCredentialVaultBoundary getVaultBoundary() { return vaultBoundary; }
    public String getSelectionPolicySummary() { return selectionPolicySummary; }
    public String getFallbackPolicySummary() { return fallbackPolicySummary; }
    public boolean isExecutionEnabled() { return executionEnabled; }
    public String getSafetyValidationSummary() { return safetyValidationSummary; }
}
