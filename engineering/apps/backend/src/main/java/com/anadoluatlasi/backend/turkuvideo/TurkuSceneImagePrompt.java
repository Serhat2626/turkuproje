package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-005: Safe image prompt entry per scene.
 * Contains summarized prompt text only — no lyrics, full script, raw payload, base64.
 */
public final class TurkuSceneImagePrompt {
    private final int sceneNo;
    private final String safePromptSummary;
    private final String safeNegativeConstraints;
    private final List<String> safetyConstraintLabels;
    private final String aspectRatio;
    private final String styleHint;
    private final boolean blocked;
    private final String blockedReason;

    public TurkuSceneImagePrompt(int sceneNo, String safePromptSummary, String safeNegativeConstraints,
                                  List<String> safetyConstraintLabels, String aspectRatio, String styleHint,
                                  boolean blocked, String blockedReason) {
        this.sceneNo = sceneNo;
        this.safePromptSummary = safePromptSummary;
        this.safeNegativeConstraints = safeNegativeConstraints;
        this.safetyConstraintLabels = safetyConstraintLabels == null ? List.of() : List.copyOf(safetyConstraintLabels);
        this.aspectRatio = aspectRatio;
        this.styleHint = styleHint;
        this.blocked = blocked;
        this.blockedReason = blockedReason;
    }

    public int getSceneNo() { return sceneNo; }
    public String getSafePromptSummary() { return safePromptSummary; }
    public String getSafeNegativeConstraints() { return safeNegativeConstraints; }
    public List<String> getSafetyConstraintLabels() { return safetyConstraintLabels; }
    public String getAspectRatio() { return aspectRatio; }
    public String getStyleHint() { return styleHint; }
    public boolean isBlocked() { return blocked; }
    public String getBlockedReason() { return blockedReason; }
}
