package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-046: Adapter result — storyboard metadata → scene render plans.
 * Metadata-only. No public delivery.
 */
public final class TurkuStoryboardRenderPlanAdapterResult {
    private final String opaqueAdapterRef;
    private final String opaqueStoryboardRef;
    private final String opaqueStoryRef;
    private final TurkuStoryboardRenderAdapterStatus status;
    private final List<TurkuStoryboardRenderAdapterIssueCode> issueCodes;
    private final List<TurkuSceneRenderPlan> scenePlans;
    private final int sceneCount;
    private final boolean internalOnly;

    public TurkuStoryboardRenderPlanAdapterResult(String opaqueAdapterRef,
            String opaqueStoryboardRef, String opaqueStoryRef,
            TurkuStoryboardRenderAdapterStatus status,
            List<TurkuStoryboardRenderAdapterIssueCode> issueCodes,
            List<TurkuSceneRenderPlan> scenePlans, int sceneCount) {
        this.opaqueAdapterRef = opaqueAdapterRef;
        this.opaqueStoryboardRef = opaqueStoryboardRef;
        this.opaqueStoryRef = opaqueStoryRef;
        this.status = status;
        this.issueCodes = issueCodes != null ? List.copyOf(issueCodes) : List.of();
        this.scenePlans = scenePlans != null ? List.copyOf(scenePlans) : List.of();
        this.sceneCount = sceneCount;
        this.internalOnly = true;
    }

    public String getOpaqueAdapterRef() { return opaqueAdapterRef; }
    public String getOpaqueStoryboardRef() { return opaqueStoryboardRef; }
    public String getOpaqueStoryRef() { return opaqueStoryRef; }
    public TurkuStoryboardRenderAdapterStatus getStatus() { return status; }
    public List<TurkuStoryboardRenderAdapterIssueCode> getIssueCodes() { return issueCodes; }
    public List<TurkuSceneRenderPlan> getScenePlans() { return scenePlans; }
    public int getSceneCount() { return sceneCount; }
    public boolean isInternalOnly() { return internalOnly; }

    @Override
    public String toString() {
        return "AdapterResult{ref=" + opaqueAdapterRef + ", status=" + status
                + ", scenes=" + sceneCount + ", internalOnly=" + internalOnly + "}";
    }
}
