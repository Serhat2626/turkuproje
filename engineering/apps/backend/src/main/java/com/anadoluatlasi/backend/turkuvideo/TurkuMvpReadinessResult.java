package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RP-005: MVP readiness package result — safe metadata aggregation.
 */
public final class TurkuMvpReadinessResult {
    private final TurkuMvpReadinessStatus status;
    private final List<TurkuMvpReadinessIssueCode> blockingIssues;
    private final String opaqueRequestRef;
    private final String safeTurkuTitleLabel;

    // Chain summaries
    private final String storyboardReadinessSummary;
    private final String assetPlanReadinessSummary;
    private final String timelineReadinessSummary;

    // Counts
    private final int totalScenes;
    private final int blockedScenes;
    private final int timelineItems;
    private final int totalDurationMs;

    // Safety checklist
    private final boolean copyrightSafeIntentPresent;
    private final boolean allScenesPlanned;
    private final boolean timelineComplete;
    private final boolean noBlockedScenes;

    // Next steps — labels only
    private final List<String> nextAllowedManualStepLabels;

    public TurkuMvpReadinessResult(
            TurkuMvpReadinessStatus status, List<TurkuMvpReadinessIssueCode> blockingIssues,
            String opaqueRequestRef, String safeTurkuTitleLabel,
            String storyboardReadinessSummary, String assetPlanReadinessSummary,
            String timelineReadinessSummary,
            int totalScenes, int blockedScenes, int timelineItems, int totalDurationMs,
            boolean copyrightSafeIntentPresent, boolean allScenesPlanned,
            boolean timelineComplete, boolean noBlockedScenes,
            List<String> nextAllowedManualStepLabels) {
        this.status = status;
        this.blockingIssues = blockingIssues == null ? List.of() : List.copyOf(blockingIssues);
        this.opaqueRequestRef = opaqueRequestRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.storyboardReadinessSummary = storyboardReadinessSummary;
        this.assetPlanReadinessSummary = assetPlanReadinessSummary;
        this.timelineReadinessSummary = timelineReadinessSummary;
        this.totalScenes = totalScenes;
        this.blockedScenes = blockedScenes;
        this.timelineItems = timelineItems;
        this.totalDurationMs = totalDurationMs;
        this.copyrightSafeIntentPresent = copyrightSafeIntentPresent;
        this.allScenesPlanned = allScenesPlanned;
        this.timelineComplete = timelineComplete;
        this.noBlockedScenes = noBlockedScenes;
        this.nextAllowedManualStepLabels = nextAllowedManualStepLabels == null ? List.of() : List.copyOf(nextAllowedManualStepLabels);
    }

    public TurkuMvpReadinessStatus getStatus() { return status; }
    public List<TurkuMvpReadinessIssueCode> getBlockingIssues() { return blockingIssues; }
    public String getOpaqueRequestRef() { return opaqueRequestRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public String getStoryboardReadinessSummary() { return storyboardReadinessSummary; }
    public String getAssetPlanReadinessSummary() { return assetPlanReadinessSummary; }
    public String getTimelineReadinessSummary() { return timelineReadinessSummary; }
    public int getTotalScenes() { return totalScenes; }
    public int getBlockedScenes() { return blockedScenes; }
    public int getTimelineItems() { return timelineItems; }
    public int getTotalDurationMs() { return totalDurationMs; }
    public boolean isCopyrightSafeIntentPresent() { return copyrightSafeIntentPresent; }
    public boolean isAllScenesPlanned() { return allScenesPlanned; }
    public boolean isTimelineComplete() { return timelineComplete; }
    public boolean isNoBlockedScenes() { return noBlockedScenes; }
    public List<String> getNextAllowedManualStepLabels() { return nextAllowedManualStepLabels; }
}
