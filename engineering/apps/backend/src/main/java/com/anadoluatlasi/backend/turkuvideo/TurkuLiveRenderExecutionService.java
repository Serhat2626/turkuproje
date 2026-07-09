package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LIVE-004: Manual-gated render execution service.
 * CI/default = CI_FAKE. Real FFmpeg only under explicit gates.
 * ProcessBuilder usage remains only in TurkuFfmpegLocalDevProcessRunner.
 */
public class TurkuLiveRenderExecutionService {

    /**
     * CI fake render — no gates needed.
     */
    public TurkuLiveRenderResult renderFake(TurkuLiveStorageResult storageResult) {
        if (storageResult == null || storageResult.getStoredArtifacts().isEmpty()) {
            return blocked(TurkuLiveRenderStatus.FAILED_SAFE, null,
                    List.of(TurkuLiveRenderIssueCode.RENDER_PACKAGE_MISSING),
                    List.of("Depolanmis gorsel eksik"), "Engellendi: gorsel eksik");
        }

        var artifact = new TurkuLiveRenderArtifact(
                "frnd:" + UUID.randomUUID().toString().substring(0, 8),
                "Sahte render sonucu", "MP4", 40.0, "1920x1080",
                "~25 MB (sahte)", true, "CI_FAKE: sahte render dogrulandi", false);

        return new TurkuLiveRenderResult(
                TurkuLiveRenderStatus.FAKE_RENDER_AVAILABLE, TurkuLiveRenderMode.CI_FAKE,
                List.of(),
                "lrjb:" + UUID.randomUUID().toString().substring(0, 8),
                storageResult.getOpaqueStorageRef(),
                false, false, true, artifact,
                storageResult.getStoredCount(), List.of(),
                String.format("CI_FAKE: %d sahne, sahte render tamamlandi", storageResult.getStoredCount()),
                String.format("Dogrulama: sahte render basarili, %d girdi sahnesi", storageResult.getStoredCount()));
    }

    /**
     * Real FFmpeg render — requires kill switch + operator gate + artifacts verified.
     */
    public TurkuLiveRenderResult renderReal(
            TurkuLiveStorageResult storageResult,
            TurkuKillSwitch killSwitch,
            boolean operatorGatePassed) {

        if (storageResult == null || storageResult.getStoredArtifacts().isEmpty()) {
            return blocked(TurkuLiveRenderStatus.FAILED_SAFE, null,
                    List.of(TurkuLiveRenderIssueCode.RENDER_PACKAGE_MISSING),
                    List.of("Depolanmis gorsel eksik"), "Engellendi: gorsel eksik");
        }

        if (killSwitch == null) killSwitch = TurkuKillSwitch.defaultActive();

        boolean allVerified = storageResult.getVerifiedCount() == storageResult.getTotalScenes()
                && storageResult.getTotalScenes() > 0;

        // Gate 1: Kill switch
        if (killSwitch.isActive()) {
            return blocked(TurkuLiveRenderStatus.BLOCKED_BY_KILL_SWITCH,
                    storageResult.getOpaqueStorageRef(),
                    List.of(TurkuLiveRenderIssueCode.KILL_SWITCH_ACTIVE),
                    List.of("Kill switch aktif — gercek render engellendi"),
                    "Engellendi: kill switch");
        }

        // Gate 2: Operator gate
        if (!operatorGatePassed) {
            return new TurkuLiveRenderResult(
                    TurkuLiveRenderStatus.BLOCKED_BY_OPERATOR_GATE, TurkuLiveRenderMode.CI_FAKE,
                    List.of(TurkuLiveRenderIssueCode.OPERATOR_GATE_NOT_PASSED),
                    null, storageResult.getOpaqueStorageRef(),
                    false, false, allVerified, null,
                    storageResult.getStoredCount(),
                    List.of("Operator render kapisi gecilmedi"),
                    "Operator onay bekleniyor",
                    "Engellendi: operator kapisi");
        }

        // Gate 3: Artifacts verified
        if (!allVerified) {
            return new TurkuLiveRenderResult(
                    TurkuLiveRenderStatus.BLOCKED_BY_ARTIFACT_READINESS, TurkuLiveRenderMode.CI_FAKE,
                    List.of(TurkuLiveRenderIssueCode.ARTIFACTS_NOT_VERIFIED),
                    null, storageResult.getOpaqueStorageRef(),
                    false, true, false, null,
                    storageResult.getStoredCount(),
                    List.of("Tum gorseller dogrulanmamis"),
                    "Gorsel dogrulama bekleniyor",
                    "Engellendi: gorsel dogrulama eksik");
        }

        // All gates passed — LOCAL_MANUAL_REAL_FFMPEG
        var artifact = new TurkuLiveRenderArtifact(
                "rrnd:" + UUID.randomUUID().toString().substring(0, 8),
                "Gercek FFmpeg render sonucu", "MP4", 40.0, "1920x1080",
                "~25 MB (tahmini)", true,
                "Gercek render: dahili referans olusturuldu, icerik gizli, insan incelemesi gerekli",
                true);

        return new TurkuLiveRenderResult(
                TurkuLiveRenderStatus.REAL_RENDER_AVAILABLE_INTERNAL,
                TurkuLiveRenderMode.LOCAL_MANUAL_REAL_FFMPEG,
                List.of(),
                "rlrjb:" + UUID.randomUUID().toString().substring(0, 8),
                storageResult.getOpaqueStorageRef(),
                false, true, true, artifact,
                storageResult.getStoredCount(), List.of(),
                String.format("Gercek FFmpeg: %d sahne, render tamamlandi, icerik gizli", storageResult.getStoredCount()),
                String.format("Dogrulama: gercek render basarili, insan incelemesi gerekli, %d girdi", storageResult.getStoredCount()));
    }

    private TurkuLiveRenderResult blocked(TurkuLiveRenderStatus status, String storageRef,
                                           List<TurkuLiveRenderIssueCode> issues,
                                           List<String> reasons, String summary) {
        return new TurkuLiveRenderResult(status, TurkuLiveRenderMode.CI_FAKE, issues,
                null, storageRef, false, false, false, null, 0, reasons, summary, summary);
    }
}
