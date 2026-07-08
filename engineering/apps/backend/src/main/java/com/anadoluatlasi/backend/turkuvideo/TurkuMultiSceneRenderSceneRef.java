package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-038: Safe scene reference within a multi-scene job.
 * Metadata-only — no content/path/command.
 */
public final class TurkuMultiSceneRenderSceneRef {
    private final String opaqueSceneRef;
    private final int sceneOrder;
    private final int expectedDurationSeconds;
    private final int expectedFps;
    private final int expectedWidth;
    private final int expectedHeight;
    private final TurkuSceneRenderPlanStatus planStatus;
    private final boolean hasPrimaryVideoExpectation;

    public TurkuMultiSceneRenderSceneRef(String opaqueSceneRef, int sceneOrder,
            int expectedDurationSeconds, int expectedFps, int expectedWidth, int expectedHeight,
            TurkuSceneRenderPlanStatus planStatus, boolean hasPrimaryVideoExpectation) {
        this.opaqueSceneRef = opaqueSceneRef;
        this.sceneOrder = sceneOrder;
        this.expectedDurationSeconds = expectedDurationSeconds;
        this.expectedFps = expectedFps;
        this.expectedWidth = expectedWidth;
        this.expectedHeight = expectedHeight;
        this.planStatus = planStatus != null ? planStatus : TurkuSceneRenderPlanStatus.FAILED_SAFE;
        this.hasPrimaryVideoExpectation = hasPrimaryVideoExpectation;
    }

    /**
     * Build from FP-037 scene render plan.
     */
    public static TurkuMultiSceneRenderSceneRef fromPlan(TurkuSceneRenderPlan plan) {
        if (plan == null) return new TurkuMultiSceneRenderSceneRef(
                null, 0, 0, 0, 0, 0, TurkuSceneRenderPlanStatus.FAILED_SAFE, false);
        boolean hasPrimary = plan.getArtifactExpectations().stream()
                .anyMatch(e -> e.getRole() == TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP);
        return new TurkuMultiSceneRenderSceneRef(
                plan.getOpaqueSceneRef(), plan.getSceneOrder(),
                plan.getExpectedDurationSeconds(), plan.getExpectedFps(),
                plan.getExpectedWidth(), plan.getExpectedHeight(),
                plan.getStatus(), hasPrimary);
    }

    public String getOpaqueSceneRef() { return opaqueSceneRef; }
    public int getSceneOrder() { return sceneOrder; }
    public int getExpectedDurationSeconds() { return expectedDurationSeconds; }
    public int getExpectedFps() { return expectedFps; }
    public int getExpectedWidth() { return expectedWidth; }
    public int getExpectedHeight() { return expectedHeight; }
    public TurkuSceneRenderPlanStatus getPlanStatus() { return planStatus; }
    public boolean isHasPrimaryVideoExpectation() { return hasPrimaryVideoExpectation; }

    public boolean isReady() {
        return planStatus == TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY;
    }
}
