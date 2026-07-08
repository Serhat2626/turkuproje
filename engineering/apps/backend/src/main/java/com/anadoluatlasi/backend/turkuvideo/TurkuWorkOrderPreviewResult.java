package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * MP-002: Gated work order preview result — non-executable metadata.
 */
public final class TurkuWorkOrderPreviewResult {
    private final TurkuWorkOrderPreviewStatus status;
    private final List<TurkuWorkOrderPreviewIssueCode> blockedReasons;
    private final String opaqueWorkOrderRef;
    private final String opaqueEnablementRef;
    private final String safeTurkuTitleLabel;
    private final String operatorSummary;
    private final List<TurkuProductionChecklistItem> checklistItems;
    private final int totalItems;
    private final int completedItems;
    private final int blockedItems;
    private final List<String> workOrderStepLabels;

    public TurkuWorkOrderPreviewResult(
            TurkuWorkOrderPreviewStatus status,
            List<TurkuWorkOrderPreviewIssueCode> blockedReasons,
            String opaqueWorkOrderRef, String opaqueEnablementRef,
            String safeTurkuTitleLabel, String operatorSummary,
            List<TurkuProductionChecklistItem> checklistItems,
            int totalItems, int completedItems, int blockedItems,
            List<String> workOrderStepLabels) {
        this.status = status;
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.opaqueWorkOrderRef = opaqueWorkOrderRef;
        this.opaqueEnablementRef = opaqueEnablementRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.operatorSummary = operatorSummary;
        this.checklistItems = checklistItems == null ? List.of() : List.copyOf(checklistItems);
        this.totalItems = totalItems;
        this.completedItems = completedItems;
        this.blockedItems = blockedItems;
        this.workOrderStepLabels = workOrderStepLabels == null ? List.of() : List.copyOf(workOrderStepLabels);
    }

    public TurkuWorkOrderPreviewStatus getStatus() { return status; }
    public List<TurkuWorkOrderPreviewIssueCode> getBlockedReasons() { return blockedReasons; }
    public String getOpaqueWorkOrderRef() { return opaqueWorkOrderRef; }
    public String getOpaqueEnablementRef() { return opaqueEnablementRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getOperatorSummary() { return operatorSummary; }
    public List<TurkuProductionChecklistItem> getChecklistItems() { return checklistItems; }
    public int getTotalItems() { return totalItems; }
    public int getCompletedItems() { return completedItems; }
    public int getBlockedItems() { return blockedItems; }
    public List<String> getWorkOrderStepLabels() { return workOrderStepLabels; }
}
