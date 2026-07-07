package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-023 Task 03: FFmpeg render runner gateway.
 * <p>Disabled by default. Does NOT execute any OS process.
 * Does NOT call ProcessBuilder. Does NOT invoke FFmpeg.
 * Builds a command plan for validation purposes only,
 * then returns a safe disabled/sandbox result.</p>
 *
 * <p>When disabled (default), returns RENDER_PROVIDER_DISABLED result.
 * When config is missing, returns RENDER_CONFIG_MISSING result.
 * No real rendering occurs in FP-023.</p>
 */
public class FfmpegTurkuRenderRunnerGateway implements TurkuRenderRunnerGateway {

    private final TurkuFfmpegCommandBuilder commandBuilder;
    private final TurkuFfmpegExecutionLimits limits;
    private final TurkuRenderAdapterSanitizer sanitizer;
    private final boolean enabled;

    public FfmpegTurkuRenderRunnerGateway() {
        this(false);
    }

    public FfmpegTurkuRenderRunnerGateway(boolean enabled) {
        this.commandBuilder = new TurkuFfmpegCommandBuilder(new TurkuRenderAdapterPolicy());
        this.limits = new TurkuFfmpegExecutionLimits();
        this.sanitizer = new TurkuRenderAdapterSanitizer();
        this.enabled = enabled;
    }

    @Override
    public TurkuRenderExecutionResult execute(TurkuRenderExecutionPlan plan) {
        // Step 1: Check if enabled
        if (!enabled) {
            return disabledResult("FFmpeg adapter is disabled by default — no process execution");
        }

        // Step 2: Build command plan (validation only, no execution)
        TurkuFfmpegCommandPlan commandPlan = commandBuilder.buildPlan(plan, limits);

        // Step 3: If command plan validation failed, return safe failure
        if (!commandPlan.isValidated()) {
            return failedResult(
                    TurkuRenderFailureCode.SAFETY_VIOLATION,
                    sanitizer.sanitizeMessage("Command plan rejected: " + commandPlan.getValidationError()));
        }

        // Step 4: Sandbox mode — do NOT execute the command plan
        // In FP-023, even when enabled, we do not call ProcessBuilder.
        // Return a sandbox result with opaque artifact ref.
        return sandboxResult(plan, commandPlan);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public TurkuRenderAdapterMode getAdapterMode() {
        return enabled ? TurkuRenderAdapterMode.FFMPEG_READY : TurkuRenderAdapterMode.FFMPEG_DISABLED;
    }

    public TurkuFfmpegCommandPlan buildCommandPlanOnly(TurkuRenderExecutionPlan plan) {
        return commandBuilder.buildPlan(plan, limits);
    }

    public TurkuRenderAdapterResult getAdapterDiagnostics() {
        TurkuRenderAdapterMode mode = getAdapterMode();
        TurkuRenderAdapterTraceReference trace = new TurkuRenderAdapterTraceReference(mode);
        TurkuRenderAdapterSafetyEnvelope envelope = new TurkuRenderAdapterSafetyEnvelope(mode);
        if (!enabled) {
            return new TurkuRenderAdapterResult(
                    false, mode,
                    TurkuRenderAdapterFailureCode.RENDER_PROVIDER_DISABLED,
                    "FFmpeg adapter disabled",
                    trace, envelope, 0);
        }
        return new TurkuRenderAdapterResult(
                true, mode, null,
                "FFmpeg adapter ready (sandbox mode)",
                trace, envelope, 0);
    }

    private TurkuRenderExecutionResult disabledResult(String message) {
        return new TurkuRenderExecutionResult(
                false, 0L,
                TurkuRenderFailureCode.RENDERER_UNAVAILABLE,
                sanitizer.sanitizeMessage(message),
                null,
                "ffmpeg-disabled-trace:" + UUID.randomUUID());
    }

    private TurkuRenderExecutionResult failedResult(TurkuRenderFailureCode code, String message) {
        return new TurkuRenderExecutionResult(
                false, 0L, code,
                sanitizer.sanitizeMessage(message),
                null,
                "ffmpeg-failed-trace:" + UUID.randomUUID());
    }

    private TurkuRenderExecutionResult sandboxResult(TurkuRenderExecutionPlan plan,
                                                      TurkuFfmpegCommandPlan commandPlan) {
        // Sandbox: produce opaque artifact metadata without real rendering
        TurkuRenderArtifact artifact = new TurkuRenderArtifact(
                UUID.randomUUID(),
                "internal-ffmpeg-sandbox:" + UUID.randomUUID(),
                plan.getTargetFormat(),
                plan.getTotalDurationSeconds(),
                plan.getTargetResolution(),
                "sandbox: ~" + Math.round(plan.getTotalDurationSeconds() * 2) + "MB");

        return new TurkuRenderExecutionResult(
                true, 42L, null, null, artifact,
                "ffmpeg-sandbox-trace:" + UUID.randomUUID());
    }
}
