package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;

/**
 * RP-005: Aggregates the full RP chain into a single readiness assessment.
 * Deterministic fake/test-double only. No provider/render/FFmpeg calls.
 */
public class TurkuMvpReadinessService {

    public TurkuMvpReadinessResult assess(
            TurkuNameStoryboardRequestResult request,
            TurkuStoryboardDraftResult draft,
            TurkuSceneAssetPlanResult assetPlan,
            TurkuAnimaticTimelinePlanResult timeline) {

        List<TurkuMvpReadinessIssueCode> issues = new ArrayList<>();

        // Request check
        if (request == null) {
            issues.add(TurkuMvpReadinessIssueCode.REQUEST_MISSING);
        } else if (request.getStatus() != TurkuNameStoryboardRequestStatus.READY_FOR_STORYBOARD_GENERATION_BOUNDARY) {
            issues.add(TurkuMvpReadinessIssueCode.REQUEST_NOT_READY);
        }

        // Storyboard check
        if (draft == null) {
            issues.add(TurkuMvpReadinessIssueCode.STORYBOARD_MISSING);
        } else if (draft.getStatus() != TurkuStoryboardDraftStatus.READY_FOR_ADMIN_PREVIEW) {
            issues.add(TurkuMvpReadinessIssueCode.STORYBOARD_NOT_READY);
        }

        // Asset plan check
        boolean hasBlockedScenes = false;
        if (assetPlan == null) {
            issues.add(TurkuMvpReadinessIssueCode.ASSET_PLAN_MISSING);
        } else if (assetPlan.getOverallStatus() != TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY
                && assetPlan.getOverallStatus() != TurkuSceneAssetPlanStatus.VISUAL_BRIEF_GENERATED) {
            issues.add(TurkuMvpReadinessIssueCode.ASSET_PLAN_NOT_READY);
        } else if (assetPlan.getBlockedSceneCount() > 0) {
            issues.add(TurkuMvpReadinessIssueCode.ASSET_PLAN_HAS_BLOCKED_SCENES);
            hasBlockedScenes = true;
        }

        // Timeline check
        if (timeline == null) {
            issues.add(TurkuMvpReadinessIssueCode.TIMELINE_MISSING);
        } else if (timeline.getStatus() != TurkuAnimaticTimelinePlanStatus.READY_FOR_ASSEMBLY_BOUNDARY) {
            issues.add(TurkuMvpReadinessIssueCode.TIMELINE_NOT_READY);
        }

        // Determine status
        TurkuMvpReadinessStatus status;
        if (issues.isEmpty()) {
            status = TurkuMvpReadinessStatus.READY_FOR_MANUAL_REVIEW;
        } else if (hasBlockedScenes && issues.size() == 1) {
            status = TurkuMvpReadinessStatus.PARTIALLY_READY;
        } else {
            status = issues.stream().anyMatch(i ->
                    i == TurkuMvpReadinessIssueCode.ASSET_PLAN_HAS_BLOCKED_SCENES)
                    ? TurkuMvpReadinessStatus.BLOCKED_BY_SAFETY
                    : TurkuMvpReadinessStatus.NOT_READY;
        }

        // Summaries
        String reqRef = request != null ? request.getOpaqueStoryboardRequestRef() : null;
        String title = request != null ? request.getSafeTurkuTitleLabel() : null;
        boolean copyrightSafe = request != null && request.getCopyrightSafeIntentSummary() != null;

        String sbSummary = draft != null
                ? String.format("%d sahne, %s", draft.getTotalSceneCount(), draft.getOrchestrationMode())
                : "Eksik";
        String apSummary = assetPlan != null
                ? String.format("%d planlanm\u0131\u015f, %d engelli", assetPlan.getTotalPlannedScenes(), assetPlan.getBlockedSceneCount())
                : "Eksik";
        String tlSummary = timeline != null
                ? String.format("%d \u00f6\u011fe, %dms toplam", timeline.getTotalItems(), timeline.getTotalDurationMs())
                : "Eksik";

        int totalScenes = assetPlan != null ? assetPlan.getTotalPlannedScenes() : 0;
        int blocked = assetPlan != null ? assetPlan.getBlockedSceneCount() : 0;
        int tlItems = timeline != null ? timeline.getTotalItems() : 0;
        int tlDuration = timeline != null ? timeline.getTotalDurationMs() : 0;

        boolean allPlanned = assetPlan != null && assetPlan.getTotalPlannedScenes() > 0;
        boolean tlComplete = timeline != null && timeline.getTotalItems() > 0;

        // Next steps — labels only
        List<String> nextSteps = new ArrayList<>();
        if (status == TurkuMvpReadinessStatus.READY_FOR_MANUAL_REVIEW) {
            nextSteps.add("G\u00f6rsel varl\u0131k \u00fcretimi i\u00e7in manuel sa\u011flay\u0131c\u0131 yap\u0131land\u0131rmas\u0131");
            nextSteps.add("Sahne g\u00f6rselleri i\u00e7in manuel onay");
            nextSteps.add("Montaj i\u00e7in manuel FFmpeg yap\u0131land\u0131rmas\u0131");
        } else {
            nextSteps.add("Eksik ad\u0131mlar\u0131 tamamlay\u0131n");
            if (hasBlockedScenes) nextSteps.add("Engellenen sahneleri \u00e7\u00f6z\u00fcn");
        }

        return new TurkuMvpReadinessResult(
                status, issues, reqRef, title,
                sbSummary, apSummary, tlSummary,
                totalScenes, blocked, tlItems, tlDuration,
                copyrightSafe, allPlanned, tlComplete, blocked == 0,
                nextSteps);
    }
}
