package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RP-004: Animatic timeline plan result.
 */
public final class TurkuAnimaticTimelinePlanResult {
    private final TurkuAnimaticTimelinePlanStatus status;
    private final List<TurkuAnimaticTimelinePlanIssueCode> issueCodes;
    private final String opaqueTimelineRef;
    private final String opaquePlanRef;
    private final String opaqueDraftRef;
    private final String safeTurkuTitleLabel;
    private final String plannerMode;
    private final List<TurkuAnimaticTimelineItem> items;
    private final int totalItems;
    private final int totalDurationMs;

    public TurkuAnimaticTimelinePlanResult(
            TurkuAnimaticTimelinePlanStatus status,
            List<TurkuAnimaticTimelinePlanIssueCode> issueCodes,
            String opaqueTimelineRef, String opaquePlanRef, String opaqueDraftRef,
            String safeTurkuTitleLabel, String plannerMode,
            List<TurkuAnimaticTimelineItem> items, int totalItems, int totalDurationMs) {
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.opaqueTimelineRef = opaqueTimelineRef;
        this.opaquePlanRef = opaquePlanRef;
        this.opaqueDraftRef = opaqueDraftRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.plannerMode = plannerMode;
        this.items = items == null ? List.of() : List.copyOf(items);
        this.totalItems = totalItems;
        this.totalDurationMs = totalDurationMs;
    }

    public static TurkuAnimaticTimelinePlanResult rejected(TurkuAnimaticTimelinePlanIssueCode... codes) {
        return new TurkuAnimaticTimelinePlanResult(
                TurkuAnimaticTimelinePlanStatus.REJECTED_UNSAFE, List.of(codes),
                null, null, null, null, null, List.of(), 0, 0);
    }

    public static TurkuAnimaticTimelinePlanResult failed(TurkuAnimaticTimelinePlanIssueCode... codes) {
        return new TurkuAnimaticTimelinePlanResult(
                TurkuAnimaticTimelinePlanStatus.FAILED_SAFE, List.of(codes),
                null, null, null, null, null, List.of(), 0, 0);
    }

    public TurkuAnimaticTimelinePlanStatus getStatus() { return status; }
    public List<TurkuAnimaticTimelinePlanIssueCode> getIssueCodes() { return issueCodes; }
    public String getOpaqueTimelineRef() { return opaqueTimelineRef; }
    public String getOpaquePlanRef() { return opaquePlanRef; }
    public String getOpaqueDraftRef() { return opaqueDraftRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getPlannerMode() { return plannerMode; }
    public List<TurkuAnimaticTimelineItem> getItems() { return items; }
    public int getTotalItems() { return totalItems; }
    public int getTotalDurationMs() { return totalDurationMs; }
}
