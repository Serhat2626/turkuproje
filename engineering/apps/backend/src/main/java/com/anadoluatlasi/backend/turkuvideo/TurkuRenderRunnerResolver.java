package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 03: Render runner resolver.
 * Selects between stub and FFmpeg gateways based on adapter mode.
 * Default is always STUB. FFmpeg path is disabled by default.
 */
public class TurkuRenderRunnerResolver {

    private final StubTurkuRenderRunnerGateway stubRunner;
    private final FfmpegTurkuRenderRunnerGateway ffmpegRunner;
    private final TurkuRenderAdapterMode activeMode;

    public TurkuRenderRunnerResolver(StubTurkuRenderRunnerGateway stubRunner,
                                      FfmpegTurkuRenderRunnerGateway ffmpegRunner,
                                      boolean ffmpegEnabled, boolean ffmpegConfigPresent) {
        this.stubRunner = stubRunner;
        this.ffmpegRunner = ffmpegRunner;
        TurkuRenderAdapterPolicy policy = new TurkuRenderAdapterPolicy();
        this.activeMode = policy.resolveMode(ffmpegEnabled, ffmpegConfigPresent);
    }

    public TurkuRenderRunnerResolver(StubTurkuRenderRunnerGateway stubRunner) {
        this(stubRunner, new FfmpegTurkuRenderRunnerGateway(), false, false);
    }

    public TurkuRenderRunnerGateway resolve() {
        if (activeMode == TurkuRenderAdapterMode.FFMPEG_READY) {
            return ffmpegRunner;
        }
        return stubRunner;
    }

    public TurkuRenderAdapterMode getActiveMode() {
        return activeMode;
    }

    public boolean isStubActive() {
        return activeMode == TurkuRenderAdapterMode.STUB
                || activeMode == TurkuRenderAdapterMode.FFMPEG_DISABLED
                || activeMode == TurkuRenderAdapterMode.FFMPEG_CONFIG_MISSING
                || activeMode == TurkuRenderAdapterMode.FFMPEG_FAILED_SAFE;
    }

    public boolean isFfmpegActive() {
        return activeMode == TurkuRenderAdapterMode.FFMPEG_READY;
    }

    public TurkuRenderAdapterResult resolveWithDiagnostics(TurkuRenderExecutionPlan plan) {
        TurkuRenderAdapterTraceReference trace = new TurkuRenderAdapterTraceReference(activeMode);
        TurkuRenderAdapterSafetyEnvelope envelope = new TurkuRenderAdapterSafetyEnvelope(activeMode);

        if (activeMode == TurkuRenderAdapterMode.FFMPEG_DISABLED) {
            return new TurkuRenderAdapterResult(
                    false, activeMode,
                    TurkuRenderAdapterFailureCode.RENDER_PROVIDER_DISABLED,
                    "FFmpeg adapter is disabled — using stub runner",
                    trace, envelope, 0);
        }
        if (activeMode == TurkuRenderAdapterMode.FFMPEG_CONFIG_MISSING) {
            return new TurkuRenderAdapterResult(
                    false, activeMode,
                    TurkuRenderAdapterFailureCode.RENDER_CONFIG_MISSING,
                    "FFmpeg config not present — using stub runner",
                    trace, envelope, 0);
        }
        if (activeMode == TurkuRenderAdapterMode.FFMPEG_FAILED_SAFE) {
            return new TurkuRenderAdapterResult(
                    false, activeMode,
                    TurkuRenderAdapterFailureCode.RENDER_FAILED_SAFE,
                    "FFmpeg adapter failed safe — using stub runner",
                    trace, envelope, 0);
        }

        // STUB or FFMPEG_READY — proceed with execution via resolved runner
        return new TurkuRenderAdapterResult(
                true, activeMode, null,
                "Adapter resolved: " + activeMode.name(),
                trace, envelope, 0);
    }
}
