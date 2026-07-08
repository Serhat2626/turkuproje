package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MP-002: Generates checklist + gated work order preview from enablement candidate.
 * Labels/instructions only — no executable actions.
 */
public class TurkuWorkOrderPreviewService {

    private static final String[][] CHECKLIST_DEFS = {
            {"G\u00f6rsel sa\u011flay\u0131c\u0131 yap\u0131land\u0131rmas\u0131", "Harici g\u00f6rsel \u00fcretim sa\u011flay\u0131c\u0131s\u0131n\u0131 manuel olarak yap\u0131land\u0131r\u0131n"},
            {"G\u00f6rsel \u00f6zetleri inceleme", "T\u00fcm sahne g\u00f6rsel \u00f6zetlerini inceleyin ve uygun oldu\u011funu onaylay\u0131n"},
            {"Sahne varl\u0131klar\u0131 onay\u0131", "Her sahne i\u00e7in varl\u0131k plan\u0131n\u0131 onaylay\u0131n"},
            {"Render ortam\u0131 yap\u0131land\u0131rmas\u0131", "Yerel FFmpeg ortam\u0131n\u0131 manuel olarak yap\u0131land\u0131r\u0131n"},
            {"Zaman \u00e7izelgesi do\u011frulama", "Animatik zaman \u00e7izelgesini son kez kontrol edin"},
            {"Son kalite kontrol\u00fc", "T\u00fcm \u00e7\u0131kt\u0131lar\u0131 g\u00f6zden ge\u00e7irin ve onaylay\u0131n"},
    };

    private static final List<String> WORK_ORDER_STEPS_ELIGIBLE = List.of(
            "Kontrol listesini tamamlay\u0131n",
            "Sa\u011flay\u0131c\u0131 yap\u0131land\u0131rmas\u0131n\u0131 do\u011frulay\u0131n",
            "Render ortam\u0131n\u0131 haz\u0131rlay\u0131n",
            "Manuel \u00fcretim s\u00fcrecini ba\u015flat\u0131n"
    );

    private static final List<String> WORK_ORDER_STEPS_BLOCKED = List.of(
            "Engelleri \u00e7\u00f6z\u00fcn",
            "Kontrol listesini yeniden de\u011ferlendirin"
    );

    public TurkuWorkOrderPreviewResult generate(TurkuManualEnablementResult enablement) {
        if (enablement == null) {
            return new TurkuWorkOrderPreviewResult(
                    TurkuWorkOrderPreviewStatus.BLOCKED_BY_SAFETY,
                    List.of(TurkuWorkOrderPreviewIssueCode.ENABLEMENT_MISSING),
                    null, null, null, "Etkinle\u015ftirme adayı eksik",
                    List.of(), 0, 0, 0, WORK_ORDER_STEPS_BLOCKED);
        }

        if (enablement.getStatus() != TurkuManualEnablementStatus.ELIGIBLE_FOR_MANUAL_PREP) {
            return new TurkuWorkOrderPreviewResult(
                    TurkuWorkOrderPreviewStatus.BLOCKED_BY_SAFETY,
                    List.of(TurkuWorkOrderPreviewIssueCode.ENABLEMENT_NOT_ELIGIBLE),
                    null, enablement.getOpaqueReadinessRef(),
                    enablement.getSafeTurkuTitleLabel(),
                    "Etkinle\u015ftirme aday\u0131 uygun de\u011fil",
                    List.of(), 0, 0, 0, WORK_ORDER_STEPS_BLOCKED);
        }

        List<TurkuProductionChecklistItem> items = new ArrayList<>();
        boolean[] prereqs = {
                enablement.isCopyrightSafeIntentPresent(),
                enablement.isAllScenesPlanned(),
                enablement.isAllScenesPlanned(),
                enablement.isTimelineComplete(),
                enablement.isTimelineComplete(),
                enablement.isNoBlockedScenes()
        };

        int completed = 0, blocked = 0;
        for (int i = 0; i < CHECKLIST_DEFS.length; i++) {
            boolean met = prereqs[i];
            String reason = met ? null : "\u00d6n ko\u015ful kar\u015f\u0131lanmad\u0131";
            items.add(new TurkuProductionChecklistItem(
                    i + 1, CHECKLIST_DEFS[i][0], CHECKLIST_DEFS[i][1], met, reason));
            if (met) completed++;
            else blocked++;
        }

        TurkuWorkOrderPreviewStatus status;
        List<TurkuWorkOrderPreviewIssueCode> issues = new ArrayList<>();
        List<String> steps;

        if (blocked == 0) {
            status = TurkuWorkOrderPreviewStatus.ELIGIBLE_FOR_MANUAL_PREPARATION;
            steps = WORK_ORDER_STEPS_ELIGIBLE;
        } else {
            status = TurkuWorkOrderPreviewStatus.WAITING_FOR_MANUAL_REVIEW;
            issues.add(TurkuWorkOrderPreviewIssueCode.CHECKLIST_INCOMPLETE);
            steps = WORK_ORDER_STEPS_BLOCKED;
        }

        return new TurkuWorkOrderPreviewResult(
                status, issues,
                "wo:" + UUID.randomUUID().toString().substring(0, 8),
                enablement.getOpaqueReadinessRef(),
                enablement.getSafeTurkuTitleLabel(),
                String.format("%s \u2014 %d/%d kontrol tamamland\u0131",
                        enablement.getSafeTurkuTitleLabel() != null ? enablement.getSafeTurkuTitleLabel() : "Proje",
                        completed, items.size()),
                items, items.size(), completed, blocked, steps);
    }
}
