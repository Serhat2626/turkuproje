package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-035: Artifact lifecycle verification service for LOCAL-only generated renders.
 * Accepts metadata from FP-034 clip generation, verifies bounded limits,
 * produces safe lifecycle report with issue codes only.
 * No ProcessBuilder. No path/command/stdout/stderr exposure.
 */
public class TurkuRenderArtifactLifecycleService {

    // FP-035 bounded limits for manual LOCAL clips
    private static final int MAX_DURATION_SECONDS = 5;
    private static final int MAX_FPS = 30;
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;
    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024; // 50MB

    /**
     * Verify artifact metadata against bounded limits.
     * Returns safe report with issue codes only.
     */
    public TurkuRenderArtifactLifecycleReport verify(
            String opaqueRef, String formatStr, int durationSeconds, int fps,
            int width, int height, long sizeBytes) {

        var format = TurkuRenderArtifactFormat.fromString(formatStr);
        var issues = new ArrayList<TurkuRenderArtifactIssueCode>();

        if (opaqueRef == null || opaqueRef.isBlank())
            issues.add(TurkuRenderArtifactIssueCode.REF_MISSING);
        if (!format.isAllowed())
            issues.add(TurkuRenderArtifactIssueCode.FORMAT_NOT_ALLOWED);
        if (durationSeconds > MAX_DURATION_SECONDS || durationSeconds < 1)
            issues.add(TurkuRenderArtifactIssueCode.DURATION_EXCEEDS_LIMIT);
        if (fps > MAX_FPS || fps < 1)
            issues.add(TurkuRenderArtifactIssueCode.FPS_EXCEEDS_LIMIT);
        if (width > MAX_WIDTH || width < 1)
            issues.add(TurkuRenderArtifactIssueCode.WIDTH_EXCEEDS_LIMIT);
        if (height > MAX_HEIGHT || height < 1)
            issues.add(TurkuRenderArtifactIssueCode.HEIGHT_EXCEEDS_LIMIT);
        if (sizeBytes > MAX_SIZE_BYTES || sizeBytes < 0)
            issues.add(TurkuRenderArtifactIssueCode.SIZE_EXCEEDS_LIMIT);

        var status = issues.isEmpty()
                ? TurkuRenderArtifactVerificationStatus.PASSED
                : (issues.size() == 1 ? mapSingleIssue(issues.get(0))
                        : TurkuRenderArtifactVerificationStatus.REJECTED_MULTIPLE);

        return new TurkuRenderArtifactLifecycleReport(opaqueRef, format,
                durationSeconds, fps, width, height, sizeBytes, status, issues);
    }

    /**
     * Verify from FP-034 fixture metadata.
     */
    public TurkuRenderArtifactLifecycleReport verifyFromFixture(TurkuFfmpegFixtureMetadata metadata) {
        if (metadata == null)
            return new TurkuRenderArtifactLifecycleReport(null, TurkuRenderArtifactFormat.UNKNOWN,
                    0, 0, 0, 0, 0, TurkuRenderArtifactVerificationStatus.FAILED_SAFE,
                    List.of(TurkuRenderArtifactIssueCode.METADATA_MISSING));

        return verify(metadata.getOutputRef(), metadata.getFormat(),
                metadata.getDurationSeconds(), metadata.getFps(),
                metadata.getWidth(), metadata.getHeight(),
                metadata.getSizeMB() * 1024 * 1024);
    }

    /**
     * Register cleanup (metadata-only, no path exposure).
     */
    public TurkuRenderArtifactLifecycleStatus registerCleanup(String opaqueRef) {
        if (opaqueRef == null || opaqueRef.isBlank())
            return TurkuRenderArtifactLifecycleStatus.FAILED_SAFE;
        return TurkuRenderArtifactLifecycleStatus.CLEANUP_SCHEDULED_OR_REGISTERED;
    }

    /**
     * Confirm cleanup (best-effort, metadata-only).
     */
    public TurkuRenderArtifactLifecycleStatus confirmCleanup(String opaqueRef) {
        if (opaqueRef == null || opaqueRef.isBlank())
            return TurkuRenderArtifactLifecycleStatus.FAILED_SAFE;
        return TurkuRenderArtifactLifecycleStatus.CLEANUP_CONFIRMED_OR_BEST_EFFORT;
    }

    private TurkuRenderArtifactVerificationStatus mapSingleIssue(TurkuRenderArtifactIssueCode issue) {
        return switch (issue) {
            case FORMAT_NOT_ALLOWED -> TurkuRenderArtifactVerificationStatus.REJECTED_FORMAT;
            case DURATION_EXCEEDS_LIMIT -> TurkuRenderArtifactVerificationStatus.REJECTED_DURATION;
            case FPS_EXCEEDS_LIMIT -> TurkuRenderArtifactVerificationStatus.REJECTED_FPS;
            case WIDTH_EXCEEDS_LIMIT, HEIGHT_EXCEEDS_LIMIT ->
                    TurkuRenderArtifactVerificationStatus.REJECTED_RESOLUTION;
            case SIZE_EXCEEDS_LIMIT -> TurkuRenderArtifactVerificationStatus.REJECTED_SIZE;
            default -> TurkuRenderArtifactVerificationStatus.FAILED_SAFE;
        };
    }
}
