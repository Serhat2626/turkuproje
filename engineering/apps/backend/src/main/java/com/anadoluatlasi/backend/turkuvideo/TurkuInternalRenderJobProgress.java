package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-025 Task 02: Safe progress snapshot.
 * Bounded 0–100. Predefined stage labels only.
 */
public final class TurkuInternalRenderJobProgress {

    public enum StageLabel {
        HAZIRLANIYOR("Hazırlanıyor"),
        YAPILANDIRMA_DOGRULANIYOR("Yapılandırma doğrulanıyor"),
        CALISMA_ALANI_HAZIRLANIYOR("Çalışma alanı hazırlanıyor"),
        KOMUT_PLANI_OLUSTURULUYOR("Komut planı oluşturuluyor"),
        DAHILI_RENDER_CALISIYOR("Dahili render çalışıyor"),
        ARTIFACT_DOGRULANIYOR("Artifact doğrulanıyor"),
        TAMAMLANDI("Tamamlandı");

        private final String label;
        StageLabel(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final String jobRef;
    private final TurkuInternalRenderJobStatus status;
    private final int progressPercent;
    private final StageLabel currentStage;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    public TurkuInternalRenderJobProgress(String jobRef, TurkuInternalRenderJobStatus status,
                                            int progressPercent, StageLabel currentStage) {
        this.jobRef = jobRef;
        this.status = status;
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.currentStage = currentStage;
        this.humanReviewRequired = true;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT", "NO_DOWNLOAD");
    }

    public String getJobRef() { return jobRef; }
    public TurkuInternalRenderJobStatus getStatus() { return status; }
    public int getProgressPercent() { return progressPercent; }
    public StageLabel getCurrentStage() { return currentStage; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }

    /**
     * Validate raw progress value. Returns clamped value.
     */
    public static int clampProgress(int raw) {
        return Math.max(0, Math.min(100, raw));
    }

    /**
     * Check if raw progress is within valid bounds.
     */
    public static boolean isValidProgress(int raw) {
        return raw >= 0 && raw <= 100;
    }
}
