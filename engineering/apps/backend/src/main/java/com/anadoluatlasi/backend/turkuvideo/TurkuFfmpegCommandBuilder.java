package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-023 Task 02: Secure FFmpeg command builder.
 * <p>Produces safe, pre-validated command plans. No shell execution.
 * No user-controlled args. No command interpolation.
 * No secrets. No absolute file paths in public output.
 * All limits enforced at build time.</p>
 */
public final class TurkuFfmpegCommandBuilder {

    private static final String EXECUTABLE_TOKEN = "ffmpeg";
    private static final List<String> ALLOWED_FORMATS = List.of("mp4", "webm");
    private static final List<String> ALLOWED_CODECS = List.of("libx264", "libvpx-vp9");
    private static final List<String> ALLOWED_PRESETS = List.of("ultrafast", "fast", "medium");

    private final TurkuRenderAdapterPolicy policy;

    public TurkuFfmpegCommandBuilder(TurkuRenderAdapterPolicy policy) {
        this.policy = policy;
    }

    public TurkuFfmpegCommandPlan buildPlan(TurkuRenderExecutionPlan executionPlan,
                                              TurkuFfmpegExecutionLimits limits) {
        // Validate execution plan
        if (executionPlan == null) {
            return rejected(limits, "Execution plan is null");
        }

        // Validate format
        String format = executionPlan.getTargetFormat();
        if (format == null || !ALLOWED_FORMATS.contains(format.toLowerCase())) {
            return rejected(limits, "Unsupported format: " + format);
        }

        // Validate resolution bounds
        String resolution = executionPlan.getTargetResolution();
        if (!isValidResolution(resolution, limits)) {
            return rejected(limits, "Resolution exceeds limits: " + resolution);
        }

        // Validate FPS
        int fps = executionPlan.getTargetFps();
        if (fps < 1 || fps > limits.getMaxFps()) {
            return rejected(limits, "FPS out of bounds: " + fps);
        }

        // Validate duration
        double duration = executionPlan.getTotalDurationSeconds();
        if (duration < 0.1 || duration > limits.getMaxDurationSeconds()) {
            return rejected(limits, "Duration out of bounds: " + duration);
        }

        // Validate render mode
        String renderMode = executionPlan.getRenderMode();
        if (renderMode != null && policy.isShellMetacharacter(renderMode)) {
            return rejected(limits, "Render mode contains forbidden characters");
        }

        // Build safe input/output specs
        String inputRef = "internal-input:" + UUID.randomUUID();
        String outputRef = "internal-output:" + UUID.randomUUID();

        TurkuFfmpegInputSpec inputSpec = new TurkuFfmpegInputSpec(
                inputRef, executionPlan.getSceneCount(), duration, format);
        TurkuFfmpegOutputSpec outputSpec = new TurkuFfmpegOutputSpec(
                outputRef, format, resolution, fps, limits.getMaxDurationSeconds());

        // Build safe argument list
        List<String> safeArgs = new ArrayList<>();
        safeArgs.add("-i");
        safeArgs.add(inputRef);
        safeArgs.add("-c:v");
        safeArgs.add(selectCodec(format));
        safeArgs.add("-preset");
        safeArgs.add("medium");
        safeArgs.add("-s");
        safeArgs.add(resolution);
        safeArgs.add("-r");
        safeArgs.add(String.valueOf(fps));
        safeArgs.add("-t");
        safeArgs.add(String.valueOf((int) Math.ceil(duration)));
        safeArgs.add("-y");
        safeArgs.add(outputRef);

        // Validate all args for shell metacharacters
        for (String arg : safeArgs) {
            if (policy.isShellMetacharacter(arg)) {
                return rejected(limits, "Argument contains forbidden characters");
            }
            if (policy.isForbiddenInput(arg)) {
                return rejected(limits, "Argument contains forbidden input pattern");
            }
        }

        return new TurkuFfmpegCommandPlan(
                EXECUTABLE_TOKEN, safeArgs, limits, inputSpec, outputSpec, true, null);
    }

    public boolean validateArg(String arg) {
        if (arg == null) return false;
        return !policy.isShellMetacharacter(arg) && !policy.isForbiddenInput(arg);
    }

    private TurkuFfmpegCommandPlan rejected(TurkuFfmpegExecutionLimits limits, String error) {
        return new TurkuFfmpegCommandPlan(
                EXECUTABLE_TOKEN, List.of(), limits, null, null, false, error);
    }

    private String selectCodec(String format) {
        if ("webm".equalsIgnoreCase(format)) return "libvpx-vp9";
        return "libx264";
    }

    private boolean isValidResolution(String resolution, TurkuFfmpegExecutionLimits limits) {
        if (resolution == null) return false;
        String[] parts = resolution.split("x");
        if (parts.length != 2) return false;
        try {
            int w = Integer.parseInt(parts[0]);
            int h = Integer.parseInt(parts[1]);
            return w >= 1 && w <= limits.getMaxWidth()
                    && h >= 1 && h <= limits.getMaxHeight();
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
