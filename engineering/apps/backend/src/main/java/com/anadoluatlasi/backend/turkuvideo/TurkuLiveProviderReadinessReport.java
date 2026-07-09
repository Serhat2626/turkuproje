package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * LIVE-001: Provider readiness report.
 * Credential presence status only — no raw secret/token/env exposure.
 */
public final class TurkuLiveProviderReadinessReport {
    private final TurkuLiveProviderStatus status;
    private final List<TurkuLiveProviderIssueCode> issues;
    private final boolean credentialPresent;
    private final String credentialSourceLabel;
    private final boolean operatorEnabled;
    private final TurkuKillSwitch killSwitch;
    private final List<String> prerequisiteLabels;
    private final List<String> blockedReasons;
    private final String providerTypeLabel;
    private final String validationSummary;

    public TurkuLiveProviderReadinessReport(
            TurkuLiveProviderStatus status, List<TurkuLiveProviderIssueCode> issues,
            boolean credentialPresent, String credentialSourceLabel,
            boolean operatorEnabled, TurkuKillSwitch killSwitch,
            List<String> prerequisiteLabels, List<String> blockedReasons,
            String providerTypeLabel, String validationSummary) {
        this.status = status;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.credentialPresent = credentialPresent;
        this.credentialSourceLabel = credentialSourceLabel;
        this.operatorEnabled = operatorEnabled;
        this.killSwitch = killSwitch;
        this.prerequisiteLabels = prerequisiteLabels == null ? List.of() : List.copyOf(prerequisiteLabels);
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.providerTypeLabel = providerTypeLabel;
        this.validationSummary = validationSummary;
    }

    public TurkuLiveProviderStatus getStatus() { return status; }
    public List<TurkuLiveProviderIssueCode> getIssues() { return issues; }
    public boolean isCredentialPresent() { return credentialPresent; }
    public String getCredentialSourceLabel() { return credentialSourceLabel; }
    public boolean isOperatorEnabled() { return operatorEnabled; }
    public TurkuKillSwitch getKillSwitch() { return killSwitch; }
    public List<String> getPrerequisiteLabels() { return prerequisiteLabels; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public String getProviderTypeLabel() { return providerTypeLabel; }
    public String getValidationSummary() { return validationSummary; }
}
