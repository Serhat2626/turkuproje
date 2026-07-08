package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RP-002: Deterministic fake/test-double storyboard draft orchestrator.
 * Generates scene list from RP-001 request result. No real AI call.
 */
public class TurkuStoryboardDraftOrchestrator {

    private static final String[] PURPOSES = {
            "Giri\u015f \u2014 Anadolu co\u011frafyas\u0131 tan\u0131t\u0131m\u0131",
            "Tarihsel ba\u011flam \u2014 d\u00f6nemin atmosferi",
            "Duygusal y\u00fckselme \u2014 halk\u0131n hikayesi",
            "G\u00f6rsel doruk \u2014 do\u011fa/k\u00fclt\u00fcr sentezi",
            "Kapan\u0131\u015f \u2014 miras\u0131n bug\u00fcne ta\u015f\u0131nmas\u0131"
    };

    private static final String[] MOODS = {
            "huzurlu", "nostaljik", "dramatik", "umut dolu", "co\u015fkulu"
    };

    private static final String[] VISUALS = {
            "G\u00fcne\u015f do\u011farken Anadolu ovalar\u0131",
            "Tarihi ta\u015f yollar ve han kal\u0131nt\u0131lar\u0131",
            "Halk\u0131n g\u00fcnl\u00fck ya\u015fam\u0131 \u2014 pazar, tarla",
            "Da\u011f siluetiyle g\u00f6l manzaras\u0131",
            "G\u00fcn bat\u0131m\u0131nda k\u00f6y silueti"
    };

    public TurkuStoryboardDraftResult orchestrate(TurkuNameStoryboardRequestResult requestResult) {
        if (requestResult == null) {
            return TurkuStoryboardDraftResult.rejected(TurkuStoryboardDraftIssueCode.REQUEST_MISSING);
        }
        if (requestResult.getStatus() != TurkuNameStoryboardRequestStatus.READY_FOR_STORYBOARD_GENERATION_BOUNDARY) {
            return TurkuStoryboardDraftResult.rejected(TurkuStoryboardDraftIssueCode.REQUEST_NOT_READY);
        }
        if (requestResult.getSafeTurkuTitleLabel() == null || requestResult.getSafeTurkuTitleLabel().isBlank()) {
            return TurkuStoryboardDraftResult.rejected(TurkuStoryboardDraftIssueCode.TITLE_MISSING);
        }
        if (requestResult.getCopyrightSafeIntentSummary() == null) {
            return TurkuStoryboardDraftResult.rejected(TurkuStoryboardDraftIssueCode.COPYRIGHT_INTENT_MISSING);
        }

        int sceneCount = requestResult.getTargetSceneCount();
        if (sceneCount < 1 || sceneCount > 30) {
            return TurkuStoryboardDraftResult.rejected(TurkuStoryboardDraftIssueCode.SCENE_COUNT_INVALID);
        }

        int totalDuration = requestResult.getTargetDurationMs();
        if (totalDuration < 5_000) {
            return TurkuStoryboardDraftResult.rejected(TurkuStoryboardDraftIssueCode.DURATION_INVALID);
        }

        int perSceneDuration = totalDuration / sceneCount;
        List<TurkuStoryboardDraftScene> scenes = new ArrayList<>();
        for (int i = 0; i < sceneCount; i++) {
            scenes.add(new TurkuStoryboardDraftScene(
                    i + 1,
                    PURPOSES[i % PURPOSES.length],
                    MOODS[i % MOODS.length],
                    VISUALS[i % VISUALS.length],
                    perSceneDuration));
        }

        return new TurkuStoryboardDraftResult(
                TurkuStoryboardDraftStatus.READY_FOR_ADMIN_PREVIEW,
                List.of(),
                "draft:" + UUID.randomUUID().toString().substring(0, 8),
                requestResult.getOpaqueStoryboardRequestRef(),
                requestResult.getSafeTurkuTitleLabel(),
                requestResult.getCopyrightSafeIntentSummary(),
                "DETERMINISTIC_FAKE_TEST_DOUBLE",
                scenes, sceneCount, totalDuration,
                requestResult.getTargetFormat(),
                requestResult.getTargetWidth(), requestResult.getTargetHeight(),
                requestResult.getTargetFps());
    }
}
