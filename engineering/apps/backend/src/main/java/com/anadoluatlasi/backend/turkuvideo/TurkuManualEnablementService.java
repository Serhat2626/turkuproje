package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;

/**
 * MP-001: Maps RP readiness package to manual enablement candidate.
 * Labels/instructions only — no executable actions.
 */
public class TurkuManualEnablementService {

    private static final List<String> SAFETY_PREREQUISITES = List.of(
            "Telif g\u00fcvenli niyet onaylanm\u0131\u015f olmal\u0131",
            "T\u00fcm sahneler planlanm\u0131\u015f olmal\u0131",
            "Zaman \u00e7izelgesi tamamlanm\u0131\u015f olmal\u0131",
            "Engellenen sahne bulunmamal\u0131",
            "Storyboard taslak haz\u0131r olmal\u0131",
            "Varl\u0131k plan\u0131 haz\u0131r olmal\u0131"
    );

    private static final List<String> MANUAL_STEPS_ELIGIBLE = List.of(
            "G\u00f6rsel sa\u011flay\u0131c\u0131 yap\u0131land\u0131rmas\u0131n\u0131 yap\u0131n",
            "G\u00f6rsel \u00f6zetleri inceleyin ve onaylay\u0131n",
            "Sahne varl\u0131klar\u0131n\u0131 onaylay\u0131n",
            "Render ortam\u0131n\u0131 yap\u0131land\u0131r\u0131n",
            "Zaman \u00e7izelgesi s\u0131ras\u0131n\u0131 inceleyin",
            "Son manuel kalite kontrol\u00fc yap\u0131n"
    );

    private static final List<String> MANUAL_STEPS_BLOCKED = List.of(
            "Eksik \u00f6n ko\u015fullar\u0131 tamamlay\u0131n",
            "Engellenen sahneleri \u00e7\u00f6z\u00fcn"
    );

    public TurkuManualEnablementResult evaluate(TurkuMvpReadinessResult readiness) {
        if (readiness == null) {
            return new TurkuManualEnablementResult(
                    TurkuManualEnablementStatus.NOT_ELIGIBLE,
                    List.of(TurkuManualEnablementIssueCode.READINESS_MISSING),
                    null, null, "Haz\u0131rl\u0131k paketi eksik",
                    SAFETY_PREREQUISITES, MANUAL_STEPS_BLOCKED,
                    false, false, false, false);
        }

        List<TurkuManualEnablementIssueCode> issues = new ArrayList<>();

        if (readiness.getStatus() != TurkuMvpReadinessStatus.READY_FOR_MANUAL_REVIEW
                && readiness.getStatus() != TurkuMvpReadinessStatus.PARTIALLY_READY) {
            issues.add(TurkuManualEnablementIssueCode.READINESS_NOT_READY);
        }
        if (!readiness.isCopyrightSafeIntentPresent()) {
            issues.add(TurkuManualEnablementIssueCode.COPYRIGHT_INTENT_MISSING);
        }
        if (!readiness.isAllScenesPlanned()) {
            issues.add(TurkuManualEnablementIssueCode.ASSET_PLAN_INCOMPLETE);
        }
        if (!readiness.isTimelineComplete()) {
            issues.add(TurkuManualEnablementIssueCode.TIMELINE_INCOMPLETE);
        }
        if (!readiness.isNoBlockedScenes()) {
            issues.add(TurkuManualEnablementIssueCode.BLOCKED_SCENES_PRESENT);
        }

        TurkuManualEnablementStatus status;
        List<String> steps;
        String summary;

        if (issues.isEmpty()) {
            status = TurkuManualEnablementStatus.ELIGIBLE_FOR_MANUAL_PREP;
            steps = MANUAL_STEPS_ELIGIBLE;
            summary = String.format("%s \u2014 Manuel \u00fcretim haz\u0131rl\u0131\u011f\u0131na uygun",
                    readiness.getSafeTurkuTitleLabel() != null ? readiness.getSafeTurkuTitleLabel() : "Proje");
        } else if (issues.contains(TurkuManualEnablementIssueCode.BLOCKED_SCENES_PRESENT)) {
            status = TurkuManualEnablementStatus.BLOCKED_BY_SAFETY;
            steps = MANUAL_STEPS_BLOCKED;
            summary = "G\u00fcvenlik engeli nedeniyle uygun de\u011fil";
        } else {
            status = TurkuManualEnablementStatus.MANUAL_REVIEW_REQUIRED;
            steps = MANUAL_STEPS_BLOCKED;
            summary = "Manuel inceleme gerekli \u2014 \u00f6n ko\u015fullar eksik";
        }

        return new TurkuManualEnablementResult(
                status, issues,
                readiness.getOpaqueRequestRef(),
                readiness.getSafeTurkuTitleLabel(),
                summary, SAFETY_PREREQUISITES, steps,
                readiness.isCopyrightSafeIntentPresent(),
                readiness.isAllScenesPlanned(),
                readiness.isTimelineComplete(),
                readiness.isNoBlockedScenes());
    }
}
