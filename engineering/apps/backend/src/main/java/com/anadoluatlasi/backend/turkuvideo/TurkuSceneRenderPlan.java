package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-037: Scene render plan. Metadata-only, no content/paths/commands.
 */
public final class TurkuSceneRenderPlan {
    private final String opaqueSceneRef;
    private final String opaquePlanRef;
    private final int sceneOrder;
    private final int expectedDurationSeconds;
    private final int expectedFps;
    private final int expectedWidth;
    private final int expectedHeight;
    private final List<TurkuSceneRenderArtifactExpectation> artifactExpectations;
    private final TurkuSceneRenderPlanStatus status;
    private final List<TurkuSceneRenderPlanIssueCode> issueCodes;

    public TurkuSceneRenderPlan(String opaqueSceneRef, String opaquePlanRef,
            int sceneOrder, int expectedDurationSeconds, int expectedFps,
            int expectedWidth, int expectedHeight,
            List<TurkuSceneRenderArtifactExpectation> expectations,
            TurkuSceneRenderPlanStatus status,
            List<TurkuSceneRenderPlanIssueCode> issueCodes) {
        this.opaqueSceneRef = opaqueSceneRef;
        this.opaquePlanRef = opaquePlanRef;
        this.sceneOrder = sceneOrder;
        this.expectedDurationSeconds = expectedDurationSeconds;
        this.expectedFps = expectedFps;
        this.expectedWidth = expectedWidth;
        this.expectedHeight = expectedHeight;
        this.artifactExpectations = expectations != null ? List.copyOf(expectations) : List.of();
        this.status = status;
        this.issueCodes = issueCodes != null ? List.copyOf(issueCodes) : List.of();
    }

    public String getOpaqueSceneRef() { return opaqueSceneRef; }
    public String getOpaquePlanRef() { return opaquePlanRef; }
    public int getSceneOrder() { return sceneOrder; }
    public int getExpectedDurationSeconds() { return expectedDurationSeconds; }
    public int getExpectedFps() { return expectedFps; }
    public int getExpectedWidth() { return expectedWidth; }
    public int getExpectedHeight() { return expectedHeight; }
    public List<TurkuSceneRenderArtifactExpectation> getArtifactExpectations() { return artifactExpectations; }
    public TurkuSceneRenderPlanStatus getStatus() { return status; }
    public List<TurkuSceneRenderPlanIssueCode> getIssueCodes() { return issueCodes; }

    @Override
    public String toString() {
        return "SceneRenderPlan{scene=" + opaqueSceneRef
                + ", plan=" + opaquePlanRef
                + ", order=" + sceneOrder
                + ", status=" + status
                + ", issues=" + issueCodes + "}";
    }
}
