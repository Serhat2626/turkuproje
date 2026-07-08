package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * MP-001: Manual enablement candidate result.
 * Non-executable safe summary for operator review.
 */
public final class TurkuManualEnablementResult {
    private final TurkuManualEnablementStatus status;
    private final List<TurkuManualEnablementIssueCode> blockedReasons;
    private final String opaqueReadinessRef;
    private final String safeTurkuTitleLabel;
    private final String operatorSummary;
    private final List<String> safetyPrerequisites;
    private final List<String> manualStepLabels;
    private final boolean copyrightSafeIntentPresent;
    private final boolean allScenesPlanned;
    private final boolean timelineComplete;
    private final boolean noBlockedScenes;

    public TurkuManualEnablementResult(
            TurkuManualEnablementStatus status,
            List<TurkuManualEnablementIssueCode> blockedReasons,
            String opaqueReadinessRef, String safeTurkuTitleLabel,
            String operatorSummary,
            List<String> safetyPrerequisites,
            List<String> manualStepLabels,
            boolean copyrightSafeIntentPresent, boolean allScenesPlanned,
            boolean timelineComplete, boolean noBlockedScenes) {
        this.status = status;
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.opaqueReadinessRef = opaqueReadinessRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.operatorSummary = operatorSummary;
        this.safetyPrerequisites = safetyPrerequisites == null ? List.of() : List.copyOf(safetyPrerequisites);
        this.manualStepLabels = manualStepLabels == null ? List.of() : List.copyOf(manualStepLabels);
        this.copyrightSafeIntentPresent = copyrightSafeIntentPresent;
        this.allScenesPlanned = allScenesPlanned;
        this.timelineComplete = timelineComplete;
        this.noBlockedScenes = noBlockedScenes;
    }

    public TurkuManualEnablementStatus getStatus() { return status; }
    public List<TurkuManualEnablementIssueCode> getBlockedReasons() { return blockedReasons; }
    public String getOpaqueReadinessRef() { return opaqueReadinessRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getOperatorSummary() { return operatorSummary; }
    public List<String> getSafetyPrerequisites() { return safetyPrerequisites; }
    public List<String> getManualStepLabels() { return manualStepLabels; }
    public boolean isCopyrightSafeIntentPresent() { return copyrightSafeIntentPresent; }
    public boolean isAllScenesPlanned() { return allScenesPlanned; }
    public boolean isTimelineComplete() { return timelineComplete; }
    public boolean isNoBlockedScenes() { return noBlockedScenes; }
}
