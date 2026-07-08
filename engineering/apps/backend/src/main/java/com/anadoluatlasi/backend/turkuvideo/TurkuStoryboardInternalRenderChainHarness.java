package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;

/**
 * FP-047: Storyboard internal render chain harness.
 * Orchestrates: FP-046 adapter → FP-038 job → fake clips → fake stitch →
 * FP-035 lifecycle → FP-042 review → admin visibility.
 * No ProcessBuilder. No real FFmpeg. CI-safe.
 */
public class TurkuStoryboardInternalRenderChainHarness {

    private final TurkuStoryboardRenderPlanAdapterService adapterService;
    private final TurkuMultiSceneRenderOrchestrationService orchestrationService;
    private final TurkuClipStitchingHarness stitchingHarness;
    private final TurkuRenderArtifactLifecycleService lifecycleService;
    private final TurkuRenderReviewPackageService reviewService;

    public TurkuStoryboardInternalRenderChainHarness() {
        this.adapterService = new TurkuStoryboardRenderPlanAdapterService();
        this.orchestrationService = new TurkuMultiSceneRenderOrchestrationService();
        this.stitchingHarness = new TurkuClipStitchingHarness();
        this.lifecycleService = new TurkuRenderArtifactLifecycleService();
        this.reviewService = new TurkuRenderReviewPackageService();
    }

    /**
     * Execute full storyboard → admin visibility chain with fake metadata.
     */
    public TurkuStoryboardInternalRenderChainSummary execute(
            TurkuStoryboardInternalRenderChainFixture fixture) {

        String chainRef = "chain:" + UUID.randomUUID();
        var issues = new ArrayList<TurkuStoryboardInternalRenderChainIssueCode>();

        if (fixture == null || fixture.getSceneInputs().isEmpty()) {
            issues.add(TurkuStoryboardInternalRenderChainIssueCode.ADAPTER_FAILED);
            return fail(chainRef, null, TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE, issues);
        }

        // 1. FP-046: Storyboard adapter
        var adapterResult = adapterService.adapt(fixture.getOpaqueStoryboardRef(),
                fixture.getOpaqueStoryRef(), fixture.getSceneInputs());
        if (adapterResult.getStatus() != TurkuStoryboardRenderAdapterStatus.READY_FOR_INTERNAL_RENDER_CHAIN) {
            issues.add(TurkuStoryboardInternalRenderChainIssueCode.ADAPTER_FAILED);
            return fail(chainRef, fixture.getOpaqueStoryboardRef(),
                    TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE, issues);
        }

        // 2. FP-038: Multi-scene job
        var jobPlan = orchestrationService.buildJobPlan("job:" + UUID.randomUUID(),
                adapterResult.getScenePlans());
        if (jobPlan.getStatus() != TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER) {
            issues.add(TurkuStoryboardInternalRenderChainIssueCode.MULTI_SCENE_JOB_FAILED);
            return fail(chainRef, fixture.getOpaqueStoryboardRef(),
                    TurkuStoryboardInternalRenderChainStatus.FAILED_SAFE, issues);
        }

        // 3. Fake multi-clip generation
        int clipCount = Math.max(adapterResult.getSceneCount(), 2);
        var clipRefs = new ArrayList<TurkuClipStitchingInputClipRef>();
        for (int i = 0; i < clipCount; i++) {
            clipRefs.add(new TurkuClipStitchingInputClipRef(
                    "clip-" + UUID.randomUUID(), i + 1, 2, 24, 640, 360,
                    1024 * 1024, "mp4", true));
        }

        // 4. Fake stitching
        var stitchResult = stitchingHarness.fakeStitch(clipRefs);
        if (stitchResult.getStatus() != TurkuClipStitchingStatus.STITCHED_ARTIFACT_VERIFIED) {
            issues.add(TurkuStoryboardInternalRenderChainIssueCode.STITCHING_FAILED);
            return fail(chainRef, fixture.getOpaqueStoryboardRef(),
                    TurkuStoryboardInternalRenderChainStatus.FAILED_SAFE, issues);
        }

        // 5. FP-035: Lifecycle verification per clip
        for (var clip : clipRefs) {
            var report = lifecycleService.verify(clip.getOpaqueClipRef(), clip.getFormat(),
                    clip.getDurationSeconds(), clip.getFps(), clip.getWidth(), clip.getHeight(),
                    clip.getSizeBytes());
            if (report.getVerificationStatus() != TurkuRenderArtifactVerificationStatus.PASSED) {
                issues.add(TurkuStoryboardInternalRenderChainIssueCode.LIFECYCLE_VERIFICATION_FAILED);
                return fail(chainRef, fixture.getOpaqueStoryboardRef(),
                        TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE, issues);
            }
        }

        // 6. FP-042: Review package
        var artifactSummary = new TurkuRenderReviewArtifactSummary(
                stitchResult.getOpaqueOutputRef(),
                TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP,
                TurkuRenderArtifactVerificationStatus.PASSED,
                "mp4", stitchResult.getTotalDurationSeconds(), 24, 640, 360,
                stitchResult.getEstimatedOutputSizeBytes());
        String bundleRef = "bundle:" + UUID.randomUUID();
        var reviewPkg = reviewService.build(jobPlan.getOpaqueJobRef(),
                bundleRef, List.of(artifactSummary),
                adapterResult.getSceneCount(), clipCount);
        if (reviewPkg.getStatus() != TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY) {
            issues.add(TurkuStoryboardInternalRenderChainIssueCode.REVIEW_PACKAGE_FAILED);
            return fail(chainRef, fixture.getOpaqueStoryboardRef(),
                    TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE, issues);
        }

        // 7. Admin visibility — metadata-only
        return new TurkuStoryboardInternalRenderChainSummary(chainRef,
                fixture.getOpaqueStoryboardRef(), jobPlan.getOpaqueJobRef(),
                bundleRef, reviewPkg.getOpaqueReviewPackageRef(),
                TurkuStoryboardInternalRenderChainStatus.ADMIN_METADATA_VISIBLE,
                List.of(), adapterResult.getSceneCount(), clipCount, true,
                reviewPkg.getStatus(), true,
                stitchResult.getTotalDurationSeconds(), 640, 360, 24,
                stitchResult.getEstimatedOutputSizeBytes());
    }

    private TurkuStoryboardInternalRenderChainSummary fail(String ref, String sbRef,
            TurkuStoryboardInternalRenderChainStatus status,
            List<TurkuStoryboardInternalRenderChainIssueCode> issues) {
        return new TurkuStoryboardInternalRenderChainSummary(ref, sbRef,
                null, null, null, status, issues,
                0, 0, false, null, false,
                0, 0, 0, 0, 0);
    }
}
