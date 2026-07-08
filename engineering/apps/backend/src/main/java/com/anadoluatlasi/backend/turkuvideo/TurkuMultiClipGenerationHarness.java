package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;

/**
 * FP-039: Multi-clip generation harness. Disabled by default.
 * Does NOT use ProcessBuilder — delegates to single-clip harness.
 * Real execution only when ALL gates pass.
 */
public class TurkuMultiClipGenerationHarness {

    private final TurkuFfmpegClipGenerationHarness clipHarness;
    private final TurkuRenderArtifactLifecycleService lifecycleService;
    private final TurkuRenderBundleManifestService bundleService;
    private final boolean multiClipEnabled;

    public TurkuMultiClipGenerationHarness(TurkuFfmpegClipGenerationHarness clipHarness,
            TurkuRenderArtifactLifecycleService lifecycleService,
            TurkuRenderBundleManifestService bundleService,
            boolean multiClipEnabled) {
        this.clipHarness = clipHarness != null ? clipHarness : new TurkuFfmpegClipGenerationHarness();
        this.lifecycleService = lifecycleService != null ? lifecycleService : new TurkuRenderArtifactLifecycleService();
        this.bundleService = bundleService != null ? bundleService : new TurkuRenderBundleManifestService();
        this.multiClipEnabled = multiClipEnabled;
    }

    public TurkuMultiClipGenerationHarness() {
        this(null, null, null, false);
    }

    /**
     * Attempt multi-clip generation from validated job plan.
     */
    public TurkuMultiClipGenerationResult attempt(
            TurkuMultiSceneRenderJobPlan jobPlan,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        String jobRef = jobPlan != null ? jobPlan.getOpaqueJobRef() : null;

        // 1. Manual flag
        if (!multiClipEnabled)
            return TurkuMultiClipGenerationResult.gateDenied(jobRef,
                    TurkuMultiClipGenerationStatus.MANUAL_FLAG_NOT_ENABLED);

        // 2. Clip harness must be enabled
        if (!clipHarness.isClipGenerationEnabled())
            return TurkuMultiClipGenerationResult.gateDenied(jobRef,
                    TurkuMultiClipGenerationStatus.PROCESS_DISABLED);

        // 3. Binary policy
        if (binaryPolicy == null || !binaryPolicy.isAvailable())
            return TurkuMultiClipGenerationResult.gateDenied(jobRef,
                    TurkuMultiClipGenerationStatus.FFMPEG_UNAVAILABLE);

        // 4. Job plan must be ready
        if (jobPlan == null || jobPlan.getStatus() != TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER)
            return TurkuMultiClipGenerationResult.gateDenied(jobRef,
                    TurkuMultiClipGenerationStatus.JOB_NOT_READY);

        // 5. Generate clips per scene
        var clipRefs = new ArrayList<String>();
        var bundleArtifacts = new ArrayList<TurkuRenderBundleArtifactRef>();
        var issues = new ArrayList<TurkuMultiClipGenerationIssueCode>();
        int generated = 0, verified = 0, rejected = 0;
        int totalDuration = 0, maxW = 0, maxH = 0, maxFps = 0;
        long totalSize = 0;

        for (var sceneRef : jobPlan.getScenes()) {
            var req = TurkuMultiClipGenerationSceneRequest.fromSceneRef(sceneRef);
            if (!req.isValid()) {
                issues.add(TurkuMultiClipGenerationIssueCode.CLIP_GENERATION_FAILED);
                rejected++;
                continue;
            }

            // Build process request for single clip
            var processReq = new TurkuFfmpegProcessRequest(
                    req.getOpaqueSceneRef(), req.getFormat(), 30,
                    req.getDurationSeconds(), req.getFps(),
                    req.getWidth(), req.getHeight(), 50);
            var clipResult = clipHarness.attempt(processReq, binaryPolicy);

            if (clipResult.getStatus() == TurkuFfmpegClipGenerationStatus.VERIFICATION_PASSED) {
                generated++;
                // Verify via FP-035 lifecycle
                var lcReport = lifecycleService.verify(
                        clipResult.getOutputRef(), req.getFormat(),
                        req.getDurationSeconds(), req.getFps(),
                        req.getWidth(), req.getHeight(),
                        clipResult.getFixtureMetadata() != null ? clipResult.getFixtureMetadata().getSizeMB() * 1024L * 1024 : 0);

                if (lcReport.getVerificationStatus() == TurkuRenderArtifactVerificationStatus.PASSED) {
                    verified++;
                    clipRefs.add(clipResult.getOutputRef());
                    bundleArtifacts.add(TurkuRenderBundleArtifactRef.fromLifecycleReport(lcReport,
                            TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP));
                    totalDuration += req.getDurationSeconds();
                    maxW = Math.max(maxW, req.getWidth());
                    maxH = Math.max(maxH, req.getHeight());
                    maxFps = Math.max(maxFps, req.getFps());
                    totalSize += lcReport.getSizeBytes();
                } else {
                    rejected++;
                    issues.add(TurkuMultiClipGenerationIssueCode.CLIP_VERIFICATION_FAILED);
                }
            } else {
                rejected++;
                issues.add(TurkuMultiClipGenerationIssueCode.CLIP_GENERATION_FAILED);
            }
        }

        // 6. Determine status
        TurkuMultiClipGenerationStatus status;
        if (generated == 0 && rejected > 0)
            status = TurkuMultiClipGenerationStatus.FAILED_SAFE;
        else if (rejected > 0)
            status = TurkuMultiClipGenerationStatus.REJECTED_UNSAFE;
        else
            status = TurkuMultiClipGenerationStatus.GENERATED_CLIPS_VERIFIED;

        String runRef = "multi-clip-run:" + UUID.randomUUID();
        return new TurkuMultiClipGenerationResult(jobRef, runRef, status, issues,
                jobPlan.getSceneCount(), generated, verified, rejected,
                totalDuration, maxW, maxH, maxFps, totalSize, clipRefs, true);
    }

    /**
     * Fake/test-double multi-clip generation (CI-safe).
     */
    public TurkuMultiClipGenerationResult fakeGenerate(TurkuMultiSceneRenderJobPlan jobPlan) {
        if (jobPlan == null || jobPlan.getStatus() != TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER)
            return TurkuMultiClipGenerationResult.gateDenied(
                    jobPlan != null ? jobPlan.getOpaqueJobRef() : null,
                    TurkuMultiClipGenerationStatus.JOB_NOT_READY);

        var clipRefs = new ArrayList<String>();
        var issues = new ArrayList<TurkuMultiClipGenerationIssueCode>();
        int totalDuration = 0, maxW = 0, maxH = 0, maxFps = 0;
        long totalSize = 0;

        for (var sceneRef : jobPlan.getScenes()) {
            String fakeRef = "fake-clip:" + UUID.randomUUID();
            clipRefs.add(fakeRef);
            totalDuration += sceneRef.getExpectedDurationSeconds();
            maxW = Math.max(maxW, sceneRef.getExpectedWidth());
            maxH = Math.max(maxH, sceneRef.getExpectedHeight());
            maxFps = Math.max(maxFps, sceneRef.getExpectedFps());
            totalSize += 1024L * 1024; // 1MB fake
        }

        int count = jobPlan.getSceneCount();
        return new TurkuMultiClipGenerationResult(jobPlan.getOpaqueJobRef(),
                "fake-run:" + UUID.randomUUID(),
                TurkuMultiClipGenerationStatus.GENERATED_CLIPS_VERIFIED, issues,
                count, count, count, 0,
                totalDuration, maxW, maxH, maxFps, totalSize, clipRefs, false);
    }

    public boolean isMultiClipEnabled() { return multiClipEnabled; }
}
