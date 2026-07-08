package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RP-003: Deterministic fake/test-double scene asset planner.
 * Generates visual brief metadata from storyboard draft scenes.
 * No real AI/image provider call.
 */
public class TurkuSceneAssetPlanner {

    private static final String[] SHOT_TYPES = {
            "WIDE", "MEDIUM", "CLOSE_UP", "AERIAL", "PANNING"
    };
    private static final String[] FRAMINGS = {
            "Yatay manzara", "Dikey portre", "Simetrik merkez", "Kural \u00fc\u00e7l\u00fc", "\u00c7apraz kompozisyon"
    };
    private static final String[] ATMOSPHERES = {
            "S\u0131cak g\u00fcn \u0131\u015f\u0131\u011f\u0131", "So\u011fuk mavi tonlar", "Alt\u0131n saat", "Sis ve gizemli", "Canl\u0131 ve parlak"
    };
    private static final String[] STYLES = {
            "Osmanl\u0131 minyat\u00fcr ilham\u0131", "Anadolu halk sanat\u0131", "Sel\u00e7uklu geometrik desen",
            "Y\u00f6resel el dokumas\u0131 motif", "Do\u011fa ve ta\u015f mimarisi"
    };

    public TurkuSceneAssetPlanResult plan(TurkuStoryboardDraftResult draftResult) {
        if (draftResult == null) {
            return TurkuSceneAssetPlanResult.rejected(TurkuSceneAssetPlanIssueCode.DRAFT_MISSING);
        }
        if (draftResult.getStatus() != TurkuStoryboardDraftStatus.READY_FOR_ADMIN_PREVIEW) {
            return TurkuSceneAssetPlanResult.rejected(TurkuSceneAssetPlanIssueCode.DRAFT_NOT_READY);
        }
        if (draftResult.getScenes() == null || draftResult.getScenes().isEmpty()) {
            return TurkuSceneAssetPlanResult.rejected(TurkuSceneAssetPlanIssueCode.SCENE_MISSING);
        }

        List<TurkuSceneAssetPlanEntry> entries = new ArrayList<>();
        int blocked = 0;

        for (var scene : draftResult.getScenes()) {
            if (scene.getPurpose() == null || scene.getPurpose().isBlank()) {
                entries.add(new TurkuSceneAssetPlanEntry(
                        scene.getSceneNumber(), TurkuSceneAssetPlanStatus.BLOCKED_CONTENT_UNSAFE,
                        List.of(TurkuSceneAssetPlanIssueCode.SCENE_PURPOSE_EMPTY),
                        null, null, null, null, null, "BLOCKED", "Sahne amac\u0131 bo\u015f"));
                blocked++;
                continue;
            }
            if (scene.getVisualDirection() == null || scene.getVisualDirection().isBlank()) {
                entries.add(new TurkuSceneAssetPlanEntry(
                        scene.getSceneNumber(), TurkuSceneAssetPlanStatus.BLOCKED_CONTENT_UNSAFE,
                        List.of(TurkuSceneAssetPlanIssueCode.VISUAL_DIRECTION_EMPTY),
                        null, null, null, null, null, "BLOCKED", "G\u00f6rsel y\u00f6n bo\u015f"));
                blocked++;
                continue;
            }

            int idx = scene.getSceneNumber() - 1;
            String brief = String.format("Sahne %d: %s \u2014 %s, %s tarz\u0131nda",
                    scene.getSceneNumber(), scene.getPurpose(), scene.getMood(),
                    STYLES[idx % STYLES.length]);

            entries.add(new TurkuSceneAssetPlanEntry(
                    scene.getSceneNumber(), TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY,
                    List.of(), brief,
                    SHOT_TYPES[idx % SHOT_TYPES.length],
                    FRAMINGS[idx % FRAMINGS.length],
                    ATMOSPHERES[idx % ATMOSPHERES.length],
                    STYLES[idx % STYLES.length],
                    "SAFE", null));
        }

        TurkuSceneAssetPlanStatus overall = blocked > 0
                ? TurkuSceneAssetPlanStatus.VISUAL_BRIEF_GENERATED
                : TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY;

        return new TurkuSceneAssetPlanResult(
                overall, List.of(),
                "plan:" + UUID.randomUUID().toString().substring(0, 8),
                draftResult.getOpaqueDraftRef(),
                draftResult.getSafeTurkuTitleLabel(),
                "DETERMINISTIC_FAKE_TEST_DOUBLE",
                entries, entries.size(), blocked);
    }
}
