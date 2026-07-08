package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-004: Execution package result — aggregates work order + provider + render config.
 */
public final class TurkuExecutionPackageResult {
    private final TurkuExecutionPackageStatus status;
    private final List<TurkuExecutionPackageIssueCode> blockedReasons;
    private final String opaquePackageRef;
    private final String opaqueWorkOrderRef;
    private final String safeTurkuTitleLabel;
    private final String providerConfigSummary;
    private final String renderConfigSummary;
    private final List<TurkuManualGate> manualGates;
    private final List<TurkuExecutionCapability> executionCapabilities;
    private final String operatorExecutionSummary;
    private final List<String> futureExecutionStepLabels;
    private final String validationSummary;

    public TurkuExecutionPackageResult(
            TurkuExecutionPackageStatus status, List<TurkuExecutionPackageIssueCode> blockedReasons,
            String opaquePackageRef, String opaqueWorkOrderRef, String safeTurkuTitleLabel,
            String providerConfigSummary, String renderConfigSummary,
            List<TurkuManualGate> manualGates, List<TurkuExecutionCapability> executionCapabilities,
            String operatorExecutionSummary, List<String> futureExecutionStepLabels, String validationSummary) {
        this.status = status;
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.opaquePackageRef = opaquePackageRef;
        this.opaqueWorkOrderRef = opaqueWorkOrderRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.providerConfigSummary = providerConfigSummary;
        this.renderConfigSummary = renderConfigSummary;
        this.manualGates = manualGates == null ? List.of() : List.copyOf(manualGates);
        this.executionCapabilities = executionCapabilities == null ? List.of() : List.copyOf(executionCapabilities);
        this.operatorExecutionSummary = operatorExecutionSummary;
        this.futureExecutionStepLabels = futureExecutionStepLabels == null ? List.of() : List.copyOf(futureExecutionStepLabels);
        this.validationSummary = validationSummary;
    }

    public TurkuExecutionPackageStatus getStatus() { return status; }
    public List<TurkuExecutionPackageIssueCode> getBlockedReasons() { return blockedReasons; }
    public String getOpaquePackageRef() { return opaquePackageRef; }
    public String getOpaqueWorkOrderRef() { return opaqueWorkOrderRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getProviderConfigSummary() { return providerConfigSummary; }
    public String getRenderConfigSummary() { return renderConfigSummary; }
    public List<TurkuManualGate> getManualGates() { return manualGates; }
    public List<TurkuExecutionCapability> getExecutionCapabilities() { return executionCapabilities; }
    public String getOperatorExecutionSummary() { return operatorExecutionSummary; }
    public List<String> getFutureExecutionStepLabels() { return futureExecutionStepLabels; }
    public String getValidationSummary() { return validationSummary; }
}
