package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-024 Task 04: FFmpeg execution result.
 * Safe metadata only — no file paths, no binary, no storage keys.
 */
public final class TurkuFfmpegExecutionResult {
    private final boolean success;
    private final TurkuFfmpegExecutionMode executionMode;
    private final TurkuFfmpegExecutionFailureCode failureCode;
    private final String safeTraceRef;
    private final String opaqueArtifactRef;
    private final TurkuFfmpegExecutionSafetyEnvelope safetyEnvelope;
    private final String safeMessage;
    private final long durationMs;

    private TurkuFfmpegExecutionResult(boolean success,
                                        TurkuFfmpegExecutionMode executionMode,
                                        TurkuFfmpegExecutionFailureCode failureCode,
                                        String safeTraceRef,
                                        String opaqueArtifactRef,
                                        TurkuFfmpegExecutionSafetyEnvelope safetyEnvelope,
                                        String safeMessage,
                                        long durationMs) {
        this.success = success;
        this.executionMode = executionMode;
        this.failureCode = failureCode;
        this.safeTraceRef = safeTraceRef;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.safetyEnvelope = safetyEnvelope;
        this.safeMessage = safeMessage;
        this.durationMs = durationMs;
    }

    public static TurkuFfmpegExecutionResult disabled() {
        return new TurkuFfmpegExecutionResult(false,
                TurkuFfmpegExecutionMode.DISABLED,
                TurkuFfmpegExecutionFailureCode.EXECUTION_DISABLED,
                "ffmpeg-disabled-trace:" + UUID.randomUUID(),
                null,
                new TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode.DISABLED),
                "FFmpeg execution disabled — using stub",
                0);
    }

    public static TurkuFfmpegExecutionResult configMissing() {
        return new TurkuFfmpegExecutionResult(false,
                TurkuFfmpegExecutionMode.CONFIG_MISSING,
                TurkuFfmpegExecutionFailureCode.CONFIG_MISSING,
                "ffmpeg-config-missing-trace:" + UUID.randomUUID(),
                null,
                new TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode.CONFIG_MISSING),
                "FFmpeg config missing",
                0);
    }

    public static TurkuFfmpegExecutionResult configInvalid(List<String> notes) {
        return new TurkuFfmpegExecutionResult(false,
                TurkuFfmpegExecutionMode.CONFIG_INVALID,
                TurkuFfmpegExecutionFailureCode.CONFIG_INVALID,
                "ffmpeg-config-invalid-trace:" + UUID.randomUUID(),
                null,
                new TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode.CONFIG_INVALID, notes),
                "FFmpeg config invalid",
                0);
    }

    public static TurkuFfmpegExecutionResult dryRunSuccess(List<String> notes, long durationMs) {
        return new TurkuFfmpegExecutionResult(true,
                TurkuFfmpegExecutionMode.DRY_RUN,
                null,
                "ffmpeg-dryrun-trace:" + UUID.randomUUID(),
                "internal-dryrun-artifact:" + UUID.randomUUID(),
                new TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode.DRY_RUN, notes),
                "Dry-run validation passed — no real execution",
                durationMs);
    }

    public static TurkuFfmpegExecutionResult dryRunFailed(TurkuFfmpegExecutionFailureCode code,
                                                           String message, List<String> notes) {
        return new TurkuFfmpegExecutionResult(false,
                TurkuFfmpegExecutionMode.DRY_RUN,
                code,
                "ffmpeg-dryrun-failed-trace:" + UUID.randomUUID(),
                null,
                new TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode.DRY_RUN, notes),
                message,
                0);
    }

    public static TurkuFfmpegExecutionResult simulatedSuccess(long durationMs) {
        return new TurkuFfmpegExecutionResult(true,
                TurkuFfmpegExecutionMode.SIMULATED_SUCCESS,
                null,
                "ffmpeg-simulated-trace:" + UUID.randomUUID(),
                "internal-simulated-artifact:" + UUID.randomUUID(),
                new TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode.SIMULATED_SUCCESS),
                "Simulated execution completed — no real process",
                durationMs);
    }

    public static TurkuFfmpegExecutionResult failedSafe(TurkuFfmpegExecutionFailureCode code,
                                                         String message) {
        return new TurkuFfmpegExecutionResult(false,
                TurkuFfmpegExecutionMode.FAILED_SAFE,
                code,
                "ffmpeg-failed-safe-trace:" + UUID.randomUUID(),
                null,
                new TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode.FAILED_SAFE),
                message,
                0);
    }

    public boolean isSuccess() { return success; }
    public TurkuFfmpegExecutionMode getExecutionMode() { return executionMode; }
    public TurkuFfmpegExecutionFailureCode getFailureCode() { return failureCode; }
    public String getSafeTraceRef() { return safeTraceRef; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public TurkuFfmpegExecutionSafetyEnvelope getSafetyEnvelope() { return safetyEnvelope; }
    public String getSafeMessage() { return safeMessage; }
    public long getDurationMs() { return durationMs; }
}
