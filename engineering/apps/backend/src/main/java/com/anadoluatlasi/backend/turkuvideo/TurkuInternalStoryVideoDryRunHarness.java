package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;

/**
 * FP-044: Internal story-to-video dry run harness.
 * Orchestrates the full internal chain using fake/test-double metadata.
 * No ProcessBuilder. No real FFmpeg. CI-safe.
 */
public class TurkuInternalStoryVideoDryRunHarness {

    private final TurkuSceneRenderPlanService planService;
    private final TurkuMultiSceneRenderOrchestrationService orchestrationService;
    private final TurkuClipStitchingHarness stitchingHarness;
    private final TurkuRenderReviewPackageService reviewService;

    public TurkuInternalStoryVideoDryRunHarness() {
        this.planService = new TurkuSceneRenderPlanService();
        this.orchestrationService = new TurkuMultiSceneRenderOrchestrationService();
        this.stitchingHarness = new TurkuClipStitchingHarness();
        this.reviewService = new TurkuRenderReviewPackageService();
    }

    /**
     * Execute full E2E internal dry run with fake metadata.
     * Returns summary aggregating all stages.
     */
    public TurkuInternalStoryVideoDryRunSummary execute(int sceneCount) {
        String dryRunRef = "dry-run:" + UUID.randomUUID();
        var issues = new ArrayList<TurkuInternalStoryVideoDryRunIssueCode>();

        if (sceneCount < 1 || sceneCount > 20) {
            issues.add(TurkuInternalStoryVideoDryRunIssueCode.SCENE_PLAN_INVALID);
            return fail(dryRunRef, TurkuInternalStoryVideoDryRunStatus.REJECTED_UNSAFE, issues);
        }

        // 1. Scene plans (FP-037)
        var primaryExpectation = new TurkuSceneRenderArtifactExpectation(
                TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true);
        var plans = new ArrayList<TurkuSceneRenderPlan>();
        for (int i = 1; i <= sceneCount; i++) {
            var plan = planService.validate("scene:" + UUID.randomUUID(),
                    "plan:" + UUID.randomUUID(), i, 2, 24, 640, 360,
                    List.of(primaryExpectation));
            if (plan.getStatus() != TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY) {
                issues.add(TurkuInternalStoryVideoDryRunIssueCode.SCENE_PLAN_INVALID);
                return fail(dryRunRef, TurkuInternalStoryVideoDryRunStatus.REJECTED_UNSAFE, issues);
            }
            plans.add(plan);
        }

        // 2. Multi-scene job (FP-038)
        var jobPlan = orchestrationService.buildJobPlan("job:" + UUID.randomUUID(), plans);
        if (jobPlan.getStatus() != TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER) {
            issues.add(TurkuInternalStoryVideoDryRunIssueCode.MULTI_SCENE_JOB_FAILED);
            return fail(dryRunRef, TurkuInternalStoryVideoDryRunStatus.FAILED_SAFE, issues);
        }

        // 3. Fake multi-clip generation (FP-039 style)
        int clipCount = Math.max(sceneCount, 2); // stitching requires min 2
        var clipRefs = new ArrayList<TurkuClipStitchingInputClipRef>();
        for (int i = 0; i < clipCount; i++) {
            clipRefs.add(new TurkuClipStitchingInputClipRef(
                    "clip-" + UUID.randomUUID(), i + 1, 2, 24, 640, 360,
                    1024 * 1024, "mp4", true));
        }

        // 4. Fake stitching (FP-040)
        var stitchResult = stitchingHarness.fakeStitch(clipRefs);
        boolean stitched = stitchResult.getStatus() == TurkuClipStitchingStatus.STITCHED_ARTIFACT_VERIFIED;
        if (!stitched) {
            issues.add(TurkuInternalStoryVideoDryRunIssueCode.STITCHING_FAILED);
            return fail(dryRunRef, TurkuInternalStoryVideoDryRunStatus.FAILED_SAFE, issues);
        }

        // 5. Lifecycle verification (FP-035) — per clip
        var lifecycleService = new TurkuRenderArtifactLifecycleService();
        for (var clip : clipRefs) {
            var report = lifecycleService.verify(clip.getOpaqueClipRef(), clip.getFormat(),
                    clip.getDurationSeconds(), clip.getFps(), clip.getWidth(), clip.getHeight(),
                    clip.getSizeBytes());
            if (report.getVerificationStatus() != TurkuRenderArtifactVerificationStatus.PASSED) {
                issues.add(TurkuInternalStoryVideoDryRunIssueCode.LIFECYCLE_VERIFICATION_FAILED);
                return fail(dryRunRef, TurkuInternalStoryVideoDryRunStatus.REJECTED_UNSAFE, issues);
            }
        }

        // 6. Review package (FP-042)
        var artifactSummary = new TurkuRenderReviewArtifactSummary(
                stitchResult.getOpaqueOutputRef(),
                TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP,
                TurkuRenderArtifactVerificationStatus.PASSED,
                "mp4", stitchResult.getTotalDurationSeconds(), 24, 640, 360,
                stitchResult.getEstimatedOutputSizeBytes());
        var reviewPkg = reviewService.build(jobPlan.getOpaqueJobRef(),
                "bundle:" + UUID.randomUUID(), List.of(artifactSummary),
                sceneCount, sceneCount);
        if (reviewPkg.getStatus() != TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY) {
            issues.add(TurkuInternalStoryVideoDryRunIssueCode.REVIEW_PACKAGE_REJECTED);
            return fail(dryRunRef, TurkuInternalStoryVideoDryRunStatus.REJECTED_UNSAFE, issues);
        }

        // 7. Admin visibility — metadata-only
        return new TurkuInternalStoryVideoDryRunSummary(dryRunRef,
                jobPlan.getOpaqueJobRef(), reviewPkg.getOpaqueBundleRef(),
                reviewPkg.getOpaqueReviewPackageRef(),
                TurkuInternalStoryVideoDryRunStatus.ADMIN_METADATA_VISIBLE,
                List.of(), sceneCount, clipCount, true,
                reviewPkg.getStatus(), true,
                stitchResult.getTotalDurationSeconds(), 640, 360, 24,
                stitchResult.getEstimatedOutputSizeBytes());
    }

    private TurkuInternalStoryVideoDryRunSummary fail(String ref,
            TurkuInternalStoryVideoDryRunStatus status,
            List<TurkuInternalStoryVideoDryRunIssueCode> issues) {
        return new TurkuInternalStoryVideoDryRunSummary(ref,
                null, null, null, status, issues,
                0, 0, false, null, false,
                0, 0, 0, 0, 0);
    }
}
