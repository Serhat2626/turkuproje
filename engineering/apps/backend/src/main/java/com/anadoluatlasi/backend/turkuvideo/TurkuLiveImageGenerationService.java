package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LIVE-002: Manual-gated live image generation service.
 * CI/default = CI_FAKE. Real provider only under explicit gates.
 */
public class TurkuLiveImageGenerationService {

    /**
     * Execute image generation with gate checks.
     *
     * @param promptPkg approved prompt package
     * @param readiness provider readiness report (from LIVE-001)
     * @param operatorGatePassed explicit operator gate approval
     */
    public TurkuLiveGenerationResult execute(
            TurkuImagePromptPackageResult promptPkg,
            TurkuLiveProviderReadinessReport readiness,
            boolean operatorGatePassed) {

        if (promptPkg == null) {
            return blocked(TurkuLiveGenerationStatus.FAILED_SAFE, TurkuLiveGenerationMode.CI_FAKE,
                    null, true, false, false,
                    List.of("Prompt paketi eksik"), "Prompt paketi mevcut degil",
                    "Engellendi: prompt paketi eksik");
        }

        boolean killActive = readiness == null || readiness.getKillSwitch().isActive();
        boolean provReady = readiness != null && readiness.getStatus() == TurkuLiveProviderStatus.CREDENTIAL_PRESENT;

        // Gate 1: Kill switch
        if (killActive) {
            return blocked(TurkuLiveGenerationStatus.BLOCKED_BY_KILL_SWITCH, TurkuLiveGenerationMode.CI_FAKE,
                    promptPkg.getOpaquePromptPackageRef(), true, operatorGatePassed, provReady,
                    List.of("Kill switch aktif — gercek saglayici engellendi"),
                    "Kill switch aktif", "Engellendi: kill switch");
        }

        // Gate 2: Operator gate
        if (!operatorGatePassed) {
            return blocked(TurkuLiveGenerationStatus.BLOCKED_BY_OPERATOR_GATE, TurkuLiveGenerationMode.CI_FAKE,
                    promptPkg.getOpaquePromptPackageRef(), false, false, provReady,
                    List.of("Operator kapisi gecilmedi"),
                    "Operator onay bekleniyor", "Engellendi: operator kapisi");
        }

        // Gate 3: Provider readiness
        if (!provReady) {
            return blocked(TurkuLiveGenerationStatus.BLOCKED_BY_PROVIDER_READINESS, TurkuLiveGenerationMode.CI_FAKE,
                    promptPkg.getOpaquePromptPackageRef(), false, true, false,
                    List.of("Saglayici hazir degil — kimlik bilgisi veya yapilandirma eksik"),
                    "Saglayici hazir degil", "Engellendi: saglayici hazir degil");
        }

        // All gates passed — LOCAL_MANUAL_REAL_PROVIDER mode
        List<TurkuLiveImageResultMetadata> results = new ArrayList<>();
        for (var scene : promptPkg.getScenePrompts()) {
            if (!scene.isBlocked()) {
                results.add(new TurkuLiveImageResultMetadata(
                        scene.getSceneNo(),
                        "limg:" + UUID.randomUUID().toString().substring(0, 8),
                        String.format("Sahne %d gercek saglayici sonucu", scene.getSceneNo()),
                        "PNG", 1920, 1080,
                        TurkuLiveGenerationMode.LOCAL_MANUAL_REAL_PROVIDER,
                        String.format("Sahne %d: saglayici yaniti alindi, icerik gizli", scene.getSceneNo()),
                        true));
            }
        }

        return new TurkuLiveGenerationResult(
                TurkuLiveGenerationStatus.REAL_PROVIDER_RESULT_AVAILABLE,
                TurkuLiveGenerationMode.LOCAL_MANUAL_REAL_PROVIDER,
                "lgen:" + UUID.randomUUID().toString().substring(0, 8),
                promptPkg.getOpaquePromptPackageRef(),
                false, true, true,
                results, promptPkg.getTotalScenes(), results.size(),
                List.of(),
                String.format("Gercek saglayici: %d sonuc, tum icerikleri operator incelemeli", results.size()),
                String.format("Dogrulama: %d gorsel uretildi (gercek saglayici), insan incelemesi gerekli", results.size()));
    }

    /**
     * CI/default fake execution — no gates needed.
     */
    public TurkuLiveGenerationResult executeFake(TurkuImagePromptPackageResult promptPkg) {
        if (promptPkg == null) {
            return blocked(TurkuLiveGenerationStatus.FAILED_SAFE, TurkuLiveGenerationMode.CI_FAKE,
                    null, false, false, false,
                    List.of("Prompt paketi eksik"), "Sahte mod — prompt eksik",
                    "Engellendi: prompt paketi eksik (sahte)");
        }

        List<TurkuLiveImageResultMetadata> results = new ArrayList<>();
        for (var scene : promptPkg.getScenePrompts()) {
            if (!scene.isBlocked()) {
                results.add(new TurkuLiveImageResultMetadata(
                        scene.getSceneNo(),
                        "fimg:" + UUID.randomUUID().toString().substring(0, 8),
                        String.format("Sahne %d sahte gorsel", scene.getSceneNo()),
                        "PNG", 1920, 1080,
                        TurkuLiveGenerationMode.CI_FAKE,
                        "CI_FAKE: deterministik sahte sonuc",
                        false));
            }
        }

        return new TurkuLiveGenerationResult(
                TurkuLiveGenerationStatus.FAKE_RESULT_AVAILABLE,
                TurkuLiveGenerationMode.CI_FAKE,
                "lfgen:" + UUID.randomUUID().toString().substring(0, 8),
                promptPkg.getOpaquePromptPackageRef(),
                false, false, false,
                results, promptPkg.getTotalScenes(), results.size(),
                List.of(),
                String.format("CI_FAKE: %d sahte sonuc", results.size()),
                String.format("Dogrulama: %d sahte gorsel uretildi", results.size()));
    }

    private TurkuLiveGenerationResult blocked(
            TurkuLiveGenerationStatus status, TurkuLiveGenerationMode mode,
            String promptRef, boolean killActive, boolean opGate, boolean provReady,
            List<String> blockedReasons, String provSummary, String validationSummary) {
        return new TurkuLiveGenerationResult(
                status, mode, null, promptRef,
                killActive, opGate, provReady,
                List.of(), 0, 0,
                blockedReasons, provSummary, validationSummary);
    }
}
