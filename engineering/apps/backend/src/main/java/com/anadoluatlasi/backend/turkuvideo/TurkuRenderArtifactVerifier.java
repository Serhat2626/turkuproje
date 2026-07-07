package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-024 Task 05: Artifact verifier.
 * Metadata-only. No filesystem access. No binary parsing.
 */
public class TurkuRenderArtifactVerifier {

    private final TurkuRenderArtifactVerificationPolicy policy;
    private final TurkuRenderWorkspaceValidator refValidator;

    public TurkuRenderArtifactVerifier(TurkuRenderArtifactVerificationPolicy policy) {
        this.policy = policy != null ? policy : new TurkuRenderArtifactVerificationPolicy();
        this.refValidator = new TurkuRenderWorkspaceValidator();
    }

    public TurkuRenderArtifactVerifier() {
        this(new TurkuRenderArtifactVerificationPolicy());
    }

    /**
     * Verify artifact metadata from execution result.
     * No filesystem access. No binary parsing.
     */
    public TurkuRenderArtifactVerificationReport verify(
            String opaqueArtifactRef, String rendererMode,
            String format, double durationSeconds,
            int sizeMb, String resolution, int fps) {

        List<TurkuRenderArtifactVerificationIssue> issues = new ArrayList<>();

        // 1. Validate ref
        if (opaqueArtifactRef == null || opaqueArtifactRef.isBlank()) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.REF_MISSING,
                    "artifactRef", "Artifact ref is missing"));
        } else {
            validateRefSafety(opaqueArtifactRef, "artifactRef", issues);
        }

        // 2. Validate format
        if (format == null || !policy.getAllowedFormats().contains(format.toLowerCase())) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.FORMAT_NOT_ALLOWED,
                    "format", "Format not allowed: " + format));
        }

        // 3. Validate duration
        if (durationSeconds < 0 || durationSeconds > policy.getMaxDurationSeconds()) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.DURATION_OUT_OF_RANGE,
                    "duration", "Duration out of range: " + durationSeconds));
        }

        // 4. Validate size
        if (sizeMb < 0 || sizeMb > policy.getMaxSizeMb()) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.SIZE_OUT_OF_RANGE,
                    "size", "Size out of range: " + sizeMb));
        }

        // 5. Validate resolution
        if (resolution != null) {
            validateResolution(resolution, issues);
        }

        // 6. Validate fps
        if (fps < 1 || fps > policy.getMaxFps()) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.FPS_OUT_OF_RANGE,
                    "fps", "FPS out of range: " + fps));
        }

        // Determine status
        TurkuRenderArtifactVerificationStatus status;
        if (issues.isEmpty()) {
            status = TurkuRenderArtifactVerificationStatus.VERIFIED;
        } else if (issues.stream().anyMatch(i ->
                i.getCode() == TurkuRenderArtifactVerificationIssue.Code.FORMAT_NOT_ALLOWED)) {
            status = TurkuRenderArtifactVerificationStatus.FORMAT_REJECTED;
        } else if (issues.stream().anyMatch(i ->
                i.getCode().name().startsWith("REF_UNSAFE"))) {
            status = TurkuRenderArtifactVerificationStatus.REF_UNSAFE;
        } else if (issues.stream().anyMatch(i ->
                i.getCode().name().contains("OUT_OF_RANGE"))) {
            status = TurkuRenderArtifactVerificationStatus.BOUNDS_EXCEEDED;
        } else {
            status = TurkuRenderArtifactVerificationStatus.FAILED_SAFE;
        }

        return new TurkuRenderArtifactVerificationReport(
                opaqueArtifactRef, status, rendererMode, format, issues);
    }

    /**
     * Verify artifact from execution result.
     */
    public TurkuRenderArtifactVerificationReport verifyFromExecution(
            TurkuFfmpegExecutionResult executionResult,
            TurkuRenderExecutionPlan plan) {
        if (executionResult == null) {
            return new TurkuRenderArtifactVerificationReport(
                    null, TurkuRenderArtifactVerificationStatus.METADATA_INCOMPLETE,
                    "unknown", null,
                    List.of(new TurkuRenderArtifactVerificationIssue(
                            TurkuRenderArtifactVerificationIssue.Code.METADATA_MISSING,
                            "executionResult", "Execution result is null")));
        }

        String format = plan != null ? plan.getTargetFormat() : "mp4";
        double duration = plan != null ? plan.getTotalDurationSeconds() : 0;
        String resolution = plan != null ? plan.getTargetResolution() : "1920x1080";
        int fps = plan != null ? plan.getTargetFps() : 30;

        return verify(
                executionResult.getOpaqueArtifactRef(),
                executionResult.getExecutionMode().name(),
                format, duration, 0, resolution, fps);
    }

    // ── Ref Safety Checks ──

    private void validateRefSafety(String ref, String field,
                                    List<TurkuRenderArtifactVerificationIssue> issues) {
        if (refValidator.isPathTraversal(ref) || refValidator.isAbsolutePath(ref)
                || refValidator.isWindowsPath(ref)) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.REF_UNSAFE_PATH,
                    field, "Unsafe path in ref"));
        }
        if (refValidator.isUrl(ref)) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.REF_UNSAFE_URL,
                    field, "URL in ref"));
        }
        if (refValidator.isStorageKey(ref)) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.REF_UNSAFE_STORAGE,
                    field, "Storage key in ref"));
        }
        if (refValidator.isBase64(ref)) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.REF_UNSAFE_BASE64,
                    field, "Base64 in ref"));
        }
        if (refValidator.isSecret(ref)) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.REF_UNSAFE_SECRET,
                    field, "Secret in ref"));
        }
        if (refValidator.isCommand(ref)) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.REF_UNSAFE_COMMAND,
                    field, "Command in ref"));
        }
        if (refValidator.isShellMeta(ref)) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.REF_UNSAFE_SHELL,
                    field, "Shell metacharacter in ref"));
        }
    }

    private void validateResolution(String resolution,
                                     List<TurkuRenderArtifactVerificationIssue> issues) {
        String[] parts = resolution.toLowerCase().split("x");
        if (parts.length != 2) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.RESOLUTION_OUT_OF_RANGE,
                    "resolution", "Invalid format"));
            return;
        }
        try {
            int w = Integer.parseInt(parts[0]);
            int h = Integer.parseInt(parts[1]);
            if (w < 1 || w > policy.getMaxResolutionW() || h < 1 || h > policy.getMaxResolutionH()) {
                issues.add(new TurkuRenderArtifactVerificationIssue(
                        TurkuRenderArtifactVerificationIssue.Code.RESOLUTION_OUT_OF_RANGE,
                        "resolution", "Exceeds max"));
            }
        } catch (NumberFormatException e) {
            issues.add(new TurkuRenderArtifactVerificationIssue(
                    TurkuRenderArtifactVerificationIssue.Code.RESOLUTION_OUT_OF_RANGE,
                    "resolution", "Non-numeric"));
        }
    }
}
