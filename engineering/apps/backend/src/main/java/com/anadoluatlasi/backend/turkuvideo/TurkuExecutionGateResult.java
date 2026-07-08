package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-001: Execution safety gate result — audit-safe metadata.
 */
public final class TurkuExecutionGateResult {
    private final TurkuExecutionGateStatus status;
    private final List<TurkuExecutionGateIssueCode> blockedReasons;
    private final String opaqueExecutionRef;
    private final String opaqueWorkOrderRef;
    private final String safeTurkuTitleLabel;
    private final String auditDecisionSummary;
    private final List<TurkuExecutionCapability> capabilities;
    private final List<String> requiredOperatorGates;
    private final List<String> futureExecutionStepLabels;

    public TurkuExecutionGateResult(
            TurkuExecutionGateStatus status, List<TurkuExecutionGateIssueCode> blockedReasons,
            String opaqueExecutionRef, String opaqueWorkOrderRef,
            String safeTurkuTitleLabel, String auditDecisionSummary,
            List<TurkuExecutionCapability> capabilities,
            List<String> requiredOperatorGates, List<String> futureExecutionStepLabels) {
        this.status = status;
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.opaqueExecutionRef = opaqueExecutionRef;
        this.opaqueWorkOrderRef = opaqueWorkOrderRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.auditDecisionSummary = auditDecisionSummary;
        this.capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        this.requiredOperatorGates = requiredOperatorGates == null ? List.of() : List.copyOf(requiredOperatorGates);
        this.futureExecutionStepLabels = futureExecutionStepLabels == null ? List.of() : List.copyOf(futureExecutionStepLabels);
    }

    public TurkuExecutionGateStatus getStatus() { return status; }
    public List<TurkuExecutionGateIssueCode> getBlockedReasons() { return blockedReasons; }
    public String getOpaqueExecutionRef() { return opaqueExecutionRef; }
    public String getOpaqueWorkOrderRef() { return opaqueWorkOrderRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getAuditDecisionSummary() { return auditDecisionSummary; }
    public List<TurkuExecutionCapability> getCapabilities() { return capabilities; }
    public List<String> getRequiredOperatorGates() { return requiredOperatorGates; }
    public List<String> getFutureExecutionStepLabels() { return futureExecutionStepLabels; }
}
