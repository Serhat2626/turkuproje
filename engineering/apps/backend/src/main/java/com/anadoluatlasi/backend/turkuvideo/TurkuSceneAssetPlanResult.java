package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RP-003: Full scene asset plan result for a storyboard draft.
 */
public final class TurkuSceneAssetPlanResult {
    private final TurkuSceneAssetPlanStatus overallStatus;
    private final List<TurkuSceneAssetPlanIssueCode> overallIssueCodes;
    private final String opaquePlanRef;
    private final String opaqueDraftRef;
    private final String safeTurkuTitleLabel;
    private final String plannerMode;
    private final List<TurkuSceneAssetPlanEntry> entries;
    private final int totalPlannedScenes;
    private final int blockedSceneCount;

    public TurkuSceneAssetPlanResult(
            TurkuSceneAssetPlanStatus overallStatus,
            List<TurkuSceneAssetPlanIssueCode> overallIssueCodes,
            String opaquePlanRef, String opaqueDraftRef,
            String safeTurkuTitleLabel, String plannerMode,
            List<TurkuSceneAssetPlanEntry> entries,
            int totalPlannedScenes, int blockedSceneCount) {
        this.overallStatus = overallStatus;
        this.overallIssueCodes = overallIssueCodes == null ? List.of() : List.copyOf(overallIssueCodes);
        this.opaquePlanRef = opaquePlanRef;
        this.opaqueDraftRef = opaqueDraftRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.plannerMode = plannerMode;
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.totalPlannedScenes = totalPlannedScenes;
        this.blockedSceneCount = blockedSceneCount;
    }

    public static TurkuSceneAssetPlanResult rejected(TurkuSceneAssetPlanIssueCode... codes) {
        return new TurkuSceneAssetPlanResult(
                TurkuSceneAssetPlanStatus.REJECTED_UNSAFE, List.of(codes),
                null, null, null, null, List.of(), 0, 0);
    }

    public static TurkuSceneAssetPlanResult failed(TurkuSceneAssetPlanIssueCode... codes) {
        return new TurkuSceneAssetPlanResult(
                TurkuSceneAssetPlanStatus.FAILED_SAFE, List.of(codes),
                null, null, null, null, List.of(), 0, 0);
    }

    public TurkuSceneAssetPlanStatus getOverallStatus() { return overallStatus; }
    public List<TurkuSceneAssetPlanIssueCode> getOverallIssueCodes() { return overallIssueCodes; }
    public String getOpaquePlanRef() { return opaquePlanRef; }
    public String getOpaqueDraftRef() { return opaqueDraftRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getPlannerMode() { return plannerMode; }
    public List<TurkuSceneAssetPlanEntry> getEntries() { return entries; }
    public int getTotalPlannedScenes() { return totalPlannedScenes; }
    public int getBlockedSceneCount() { return blockedSceneCount; }
}
