package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-053: Produces operational summary from FP-051 in-memory record service.
 * Counts and statuses only — no record bodies, no raw detail leakage.
 */
public class TurkuInternalDryRunOperationalSummaryService {

    public TurkuInternalDryRunOperationalSummary summarize(TurkuInternalDryRunRecordService recordService) {
        var history = recordService.getHistory();
        int total = history.size();
        if (total == 0) {
            return new TurkuInternalDryRunOperationalSummary(
                    0, 0, 0, 0, 0,
                    TurkuInternalDryRunOperationalSummaryStatus.NO_RECORDS,
                    List.of(TurkuInternalDryRunOperationalSummaryIssueCode.NO_RECORDS_AVAILABLE),
                    0, 0, 0);
        }

        int accepted = 0, failed = 0, rejected = 0, ready = 0;
        int scenes = 0, clips = 0, stitched = 0;
        for (var r : history) {
            switch (r.getStatus()) {
                case READY_FOR_ADMIN_HISTORY_ONLY -> { accepted++; ready++; }
                case METADATA_CAPTURED, TRIGGER_ACCEPTED, DRY_RUN_STARTED, CREATED_INTERNAL_ONLY -> accepted++;
                case FAILED_SAFE -> failed++;
                case REJECTED_UNSAFE -> rejected++;
            }
            scenes += r.getSceneCount();
            clips += r.getGeneratedClipCount();
            if (r.isStitchedArtifactPresent()) stitched++;
        }

        var status = computeStatus(accepted, failed, rejected, total);
        var issues = computeIssues(failed, rejected, stitched, total);

        return new TurkuInternalDryRunOperationalSummary(
                total, accepted, failed, rejected, ready,
                status, issues, scenes, clips, stitched);
    }

    private TurkuInternalDryRunOperationalSummaryStatus computeStatus(
            int accepted, int failed, int rejected, int total) {
        if (failed == total) return TurkuInternalDryRunOperationalSummaryStatus.ALL_FAILED;
        if (rejected == total) return TurkuInternalDryRunOperationalSummaryStatus.ALL_REJECTED;
        if (accepted == total) return TurkuInternalDryRunOperationalSummaryStatus.ALL_ACCEPTED;
        return TurkuInternalDryRunOperationalSummaryStatus.MIXED;
    }

    private List<TurkuInternalDryRunOperationalSummaryIssueCode> computeIssues(
            int failed, int rejected, int stitched, int total) {
        var issues = new ArrayList<TurkuInternalDryRunOperationalSummaryIssueCode>();
        if (failed > total / 2) issues.add(TurkuInternalDryRunOperationalSummaryIssueCode.HIGH_FAILURE_RATE);
        if (rejected > total / 2) issues.add(TurkuInternalDryRunOperationalSummaryIssueCode.HIGH_REJECTION_RATE);
        if (stitched == 0 && total > 0) issues.add(TurkuInternalDryRunOperationalSummaryIssueCode.STITCH_SUCCESS_LOW);
        return issues;
    }
}
