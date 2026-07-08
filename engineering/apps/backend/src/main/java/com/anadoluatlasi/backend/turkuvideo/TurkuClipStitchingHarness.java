package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;

/**
 * FP-040: Clip stitching harness. Disabled by default.
 * Does NOT use ProcessBuilder — delegates to runner if real execution needed.
 * CI uses fake/test-double only.
 */
public class TurkuClipStitchingHarness {

    private static final int MIN_CLIPS = 2;
    private static final int MAX_CLIPS = 10;
    private static final int MAX_DURATION_PER_CLIP = 5;
    private static final int MAX_TOTAL_DURATION = 60;
    private static final int MAX_FPS = 30;
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;
    private static final long MAX_OUTPUT_SIZE = 150L * 1024 * 1024;

    private final TurkuRenderArtifactLifecycleService lifecycleService;
    private final TurkuFfmpegLocalDevProcessRunner runner;
    private final boolean stitchingEnabled;

    public TurkuClipStitchingHarness(TurkuRenderArtifactLifecycleService lifecycleService,
            TurkuFfmpegLocalDevProcessRunner runner, boolean stitchingEnabled) {
        this.lifecycleService = lifecycleService != null ? lifecycleService : new TurkuRenderArtifactLifecycleService();
        this.runner = runner != null ? runner : new TurkuFfmpegLocalDevProcessRunner();
        this.stitchingEnabled = stitchingEnabled;
    }

    public TurkuClipStitchingHarness(TurkuRenderArtifactLifecycleService lifecycleService,
            boolean stitchingEnabled) {
        this(lifecycleService, null, stitchingEnabled);
    }

    public TurkuClipStitchingHarness() { this(null, null, false); }

    /**
     * Validate and fake-stitch input clips (CI-safe).
     */
    public TurkuClipStitchingResult fakeStitch(List<TurkuClipStitchingInputClipRef> inputClips) {
        var validation = validate(inputClips);
        if (validation != null) return validation;

        var clips = inputClips != null ? inputClips : List.<TurkuClipStitchingInputClipRef>of();
        int totalDuration = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getDurationSeconds).sum();
        int maxW = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getWidth).max().orElse(0);
        int maxH = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getHeight).max().orElse(0);
        int maxFps = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getFps).max().orElse(0);
        long totalSize = clips.stream().mapToLong(TurkuClipStitchingInputClipRef::getSizeBytes).sum();
        String format = clips.get(0).getFormat();

        String stitchRef = "stitch:" + UUID.randomUUID();
        String outputRef = "stitched-output:" + UUID.randomUUID();

        // Verify each input clip individually through FP-035 (per-clip limits)
        boolean allVerified = true;
        for (var clip : clips) {
            var lcReport = lifecycleService.verify(clip.getOpaqueClipRef(), clip.getFormat(),
                    clip.getDurationSeconds(), clip.getFps(), clip.getWidth(), clip.getHeight(),
                    clip.getSizeBytes());
            if (lcReport.getVerificationStatus() != TurkuRenderArtifactVerificationStatus.PASSED)
                allVerified = false;
        }

        return new TurkuClipStitchingResult(stitchRef,
                allVerified ? TurkuClipStitchingStatus.STITCHED_ARTIFACT_VERIFIED
                        : TurkuClipStitchingStatus.REJECTED_UNSAFE,
                allVerified ? List.of() : List.of(TurkuClipStitchingIssueCode.VERIFICATION_FAILED),
                clips.size(), totalDuration, maxW, maxH, maxFps, totalSize,
                format, outputRef, false, allVerified);
    }

    /**
     * Attempt real stitching (disabled by default, LOCAL-only manual).
     */
    public TurkuClipStitchingResult attempt(List<TurkuClipStitchingInputClipRef> inputClips,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        if (!stitchingEnabled)
            return TurkuClipStitchingResult.gateDenied(TurkuClipStitchingStatus.MANUAL_FLAG_NOT_ENABLED);
        if (binaryPolicy == null || !binaryPolicy.isAvailable())
            return TurkuClipStitchingResult.gateDenied(TurkuClipStitchingStatus.FFMPEG_UNAVAILABLE);

        var validation = validate(inputClips);
        if (validation != null) return validation;

        // Real stitching — delegate to runner
        var clips = inputClips != null ? inputClips : List.<TurkuClipStitchingInputClipRef>of();
        int totalDuration = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getDurationSeconds).sum();
        int maxW = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getWidth).max().orElse(0);
        int maxH = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getHeight).max().orElse(0);
        int maxFps = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getFps).max().orElse(0);
        String format = clips.get(0).getFormat();

        var processResult = runner.stitchClips(clips.size(), totalDuration,
                maxFps, maxW, maxH, format, binaryPolicy);

        if (processResult.getStatus() == TurkuFfmpegProcessStatus.PROCESS_COMPLETED_SAFE) {
            String stitchRef = "stitch:" + UUID.randomUUID();
            long totalSize = clips.stream().mapToLong(TurkuClipStitchingInputClipRef::getSizeBytes).sum();
            return new TurkuClipStitchingResult(stitchRef,
                    TurkuClipStitchingStatus.STITCHED_ARTIFACT_VERIFIED, List.of(),
                    clips.size(), totalDuration, maxW, maxH, maxFps, totalSize,
                    format, processResult.getOutputRef(), true, true);
        }
        return TurkuClipStitchingResult.gateDenied(TurkuClipStitchingStatus.FAILED_SAFE);
    }

    private TurkuClipStitchingResult validate(List<TurkuClipStitchingInputClipRef> inputClips) {
        var issues = new ArrayList<TurkuClipStitchingIssueCode>();
        var clips = inputClips != null ? inputClips : List.<TurkuClipStitchingInputClipRef>of();

        if (clips.size() < MIN_CLIPS) issues.add(TurkuClipStitchingIssueCode.TOO_FEW_CLIPS);
        if (clips.size() > MAX_CLIPS) issues.add(TurkuClipStitchingIssueCode.TOO_MANY_CLIPS);

        var refSet = new HashSet<String>();
        for (var clip : clips) {
            if (clip.getOpaqueClipRef() == null || clip.getOpaqueClipRef().isBlank())
                issues.add(TurkuClipStitchingIssueCode.CLIP_REF_MISSING);
            if (!refSet.add(clip.getOpaqueClipRef()))
                issues.add(TurkuClipStitchingIssueCode.DUPLICATE_CLIP_REF);
            if (!clip.isVerified())
                issues.add(TurkuClipStitchingIssueCode.CLIP_NOT_VERIFIED);
            if (clip.getDurationSeconds() > MAX_DURATION_PER_CLIP)
                issues.add(TurkuClipStitchingIssueCode.CLIP_DURATION_EXCEEDED);
            if (clip.getFps() > MAX_FPS)
                issues.add(TurkuClipStitchingIssueCode.CLIP_FPS_EXCEEDED);
            if (clip.getWidth() > MAX_WIDTH || clip.getHeight() > MAX_HEIGHT)
                issues.add(TurkuClipStitchingIssueCode.CLIP_RESOLUTION_EXCEEDED);
            if (!"mp4".equalsIgnoreCase(clip.getFormat()) && !"webm".equalsIgnoreCase(clip.getFormat()))
                issues.add(TurkuClipStitchingIssueCode.FORMAT_UNSUPPORTED);
        }

        int totalDuration = clips.stream().mapToInt(TurkuClipStitchingInputClipRef::getDurationSeconds).sum();
        if (totalDuration > MAX_TOTAL_DURATION)
            issues.add(TurkuClipStitchingIssueCode.TOTAL_DURATION_EXCEEDED);

        long totalSize = clips.stream().mapToLong(TurkuClipStitchingInputClipRef::getSizeBytes).sum();
        if (totalSize > MAX_OUTPUT_SIZE)
            issues.add(TurkuClipStitchingIssueCode.OUTPUT_SIZE_EXCEEDED);

        if (!issues.isEmpty())
            return TurkuClipStitchingResult.rejected("stitch:" + UUID.randomUUID(), issues);
        return null;
    }

    public boolean isStitchingEnabled() { return stitchingEnabled; }
}
