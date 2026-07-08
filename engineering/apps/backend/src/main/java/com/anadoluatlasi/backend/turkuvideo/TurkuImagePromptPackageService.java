package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RPE-005: Maps execution package to scene-level image prompt packages.
 * No real provider call. Safe summaries only. Manual approval gate.
 */
public class TurkuImagePromptPackageService {

    private static final List<String> DEFAULT_SAFETY_CONSTRAINTS = List.of(
            "Telif korumali icerik yok",
            "Gercek kisi benzerligi yok",
            "Siddet/nefret icerigi yok",
            "Cocuk istismari icerigi yok",
            "Tarihi hassasiyet kontrol edildi");

    public TurkuImagePromptPackageResult prepare(TurkuExecutionPackageResult execPkg) {
        if (execPkg == null) {
            return new TurkuImagePromptPackageResult(
                    TurkuImagePromptPackageStatus.BLOCKED_BY_SAFETY,
                    List.of(TurkuImagePromptPackageIssueCode.EXECUTION_PACKAGE_NOT_READY),
                    null, null, null, List.of(), 0, 0, false,
                    "Manuel onay kapisi: yurutme paketi eksik",
                    "Engellendi: yurutme paketi eksik");
        }

        // Generate deterministic fake scene prompts based on title
        String title = execPkg.getSafeTurkuTitleLabel() != null ? execPkg.getSafeTurkuTitleLabel() : "Proje";
        int sceneCount = 4; // deterministic fake count
        List<TurkuSceneImagePrompt> prompts = new ArrayList<>();

        for (int i = 1; i <= sceneCount; i++) {
            prompts.add(new TurkuSceneImagePrompt(
                    i,
                    String.format("Sahne %d: %s - guvenli gorsel ozeti", i, title),
                    "Gercek kisi, telif korumali sembol, siddet icerigi",
                    DEFAULT_SAFETY_CONSTRAINTS,
                    "16:9",
                    "CINEMATIC",
                    false,
                    null));
        }

        return new TurkuImagePromptPackageResult(
                TurkuImagePromptPackageStatus.PENDING_MANUAL_APPROVAL,
                List.of(TurkuImagePromptPackageIssueCode.OPERATOR_REVIEW_PENDING),
                "ipkg:" + UUID.randomUUID().toString().substring(0, 8),
                execPkg.getOpaquePackageRef(),
                title,
                prompts,
                sceneCount,
                0,
                false,
                "Manuel onay kapisi: operator incelemesi bekleniyor",
                String.format("Dogrulama: %d sahne hazir, 0 engelli, onay bekleniyor", sceneCount));
    }
}
