package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RPE-006: Maps prompt package to fake image generation execution.
 * CI/default = CI_FAKE. Real provider disabled.
 */
public class TurkuImageGenerationExecutionService {

    public TurkuImageGenerationExecutionResult execute(TurkuImagePromptPackageResult promptPkg) {
        if (promptPkg == null) {
            return new TurkuImageGenerationExecutionResult(
                    TurkuImageGenerationStatus.BLOCKED_BY_SAFETY,
                    List.of(TurkuImageGenerationIssueCode.PROMPT_PACKAGE_NOT_APPROVED),
                    null, null,
                    TurkuImageGenerationExecutionMode.DISABLED,
                    "Prompt paketi eksik — saglayici devre disi",
                    false, List.of(), 0, 0,
                    "Engellendi: prompt paketi eksik");
        }

        List<TurkuFakeImageResultMetadata> fakeResults = new ArrayList<>();
        for (var scene : promptPkg.getScenePrompts()) {
            if (!scene.isBlocked()) {
                fakeResults.add(new TurkuFakeImageResultMetadata(
                        scene.getSceneNo(),
                        "fimg:" + UUID.randomUUID().toString().substring(0, 8),
                        String.format("Sahne %d sahte gorsel", scene.getSceneNo()),
                        "PNG",
                        1920, 1080,
                        "CI_FAKE: deterministik sahte sonuc"));
            }
        }

        return new TurkuImageGenerationExecutionResult(
                TurkuImageGenerationStatus.FAKE_RESULT_AVAILABLE,
                List.of(TurkuImageGenerationIssueCode.PROVIDER_EXECUTION_DISABLED),
                "igen:" + UUID.randomUUID().toString().substring(0, 8),
                promptPkg.getOpaquePromptPackageRef(),
                TurkuImageGenerationExecutionMode.CI_FAKE,
                "Gercek saglayici devre disi — CI_FAKE modu aktif",
                false,
                fakeResults,
                promptPkg.getTotalScenes(),
                fakeResults.size(),
                String.format("Dogrulama: %d sahte gorsel uretildi, gercek saglayici devre disi", fakeResults.size()));
    }
}
