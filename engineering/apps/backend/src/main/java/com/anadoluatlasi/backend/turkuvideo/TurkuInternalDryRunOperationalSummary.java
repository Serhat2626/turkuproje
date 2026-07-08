package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-053: Safe operational summary — counts and statuses only.
 * No record bodies, no paths, no logs, no storage, no URLs, no media.
 */
public final class TurkuInternalDryRunOperationalSummary {
    private final int totalRecordCount;
    private final int acceptedCount;
    private final int failedCount;
    private final int rejectedCount;
    private final int readyForAdminHistoryOnlyCount;
    private final TurkuInternalDryRunOperationalSummaryStatus overallStatus;
    private final List<TurkuInternalDryRunOperationalSummaryIssueCode> issueCodes;
    private final int totalSceneCount;
    private final int totalClipCount;
    private final int stitchedCount;
    private final boolean internalOnly;

    public TurkuInternalDryRunOperationalSummary(
            int totalRecordCount, int acceptedCount, int failedCount, int rejectedCount,
            int readyForAdminHistoryOnlyCount,
            TurkuInternalDryRunOperationalSummaryStatus overallStatus,
            List<TurkuInternalDryRunOperationalSummaryIssueCode> issueCodes,
            int totalSceneCount, int totalClipCount, int stitchedCount) {
        this.totalRecordCount = totalRecordCount;
        this.acceptedCount = acceptedCount;
        this.failedCount = failedCount;
        this.rejectedCount = rejectedCount;
        this.readyForAdminHistoryOnlyCount = readyForAdminHistoryOnlyCount;
        this.overallStatus = overallStatus;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.totalSceneCount = totalSceneCount;
        this.totalClipCount = totalClipCount;
        this.stitchedCount = stitchedCount;
        this.internalOnly = true;
    }

    public int getTotalRecordCount() { return totalRecordCount; }
    public int getAcceptedCount() { return acceptedCount; }
    public int getFailedCount() { return failedCount; }
    public int getRejectedCount() { return rejectedCount; }
    public int getReadyForAdminHistoryOnlyCount() { return readyForAdminHistoryOnlyCount; }
    public TurkuInternalDryRunOperationalSummaryStatus getOverallStatus() { return overallStatus; }
    public List<TurkuInternalDryRunOperationalSummaryIssueCode> getIssueCodes() { return issueCodes; }
    public int getTotalSceneCount() { return totalSceneCount; }
    public int getTotalClipCount() { return totalClipCount; }
    public int getStitchedCount() { return stitchedCount; }
    public boolean isInternalOnly() { return internalOnly; }
}
