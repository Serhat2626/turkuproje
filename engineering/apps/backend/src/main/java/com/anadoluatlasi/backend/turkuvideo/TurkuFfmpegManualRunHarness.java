package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-033: Manual local FFmpeg run harness.
 * Disabled by default. Does NOT run in CI.
 * Requires explicit manual flags to reach real execution.
 * No ProcessBuilder in THIS class — delegates to runner.
 */
public class TurkuFfmpegManualRunHarness {

    private final TurkuFfmpegLocalDevProcessRunner runner;
    private final boolean manualRunEnabled;

    public TurkuFfmpegManualRunHarness(TurkuFfmpegLocalDevProcessRunner runner, boolean manualRunEnabled) {
        this.runner = runner != null ? runner : new TurkuFfmpegLocalDevProcessRunner();
        this.manualRunEnabled = manualRunEnabled;
    }

    public TurkuFfmpegManualRunHarness() {
        this(new TurkuFfmpegLocalDevProcessRunner(), false);
    }

    /**
     * Attempt manual local FFmpeg run.
     */
    public TurkuFfmpegManualRunEvidenceResult attempt(
            TurkuFfmpegProcessRequest request,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        // 1. Manual flag check
        if (!manualRunEnabled)
            return TurkuFfmpegManualRunEvidenceResult.notAttempted(
                    TurkuFfmpegManualRunEvidenceStatus.MANUAL_FLAG_NOT_ENABLED);

        // 2. Request validation
        if (request == null || !request.isValid())
            return TurkuFfmpegManualRunEvidenceResult.notAttempted(
                    TurkuFfmpegManualRunEvidenceStatus.GATE_DENIED);

        // 3. Process enabled check
        if (!runner.isProcessEnabled())
            return TurkuFfmpegManualRunEvidenceResult.notAttempted(
                    TurkuFfmpegManualRunEvidenceStatus.PROCESS_DISABLED);

        // 4. Binary policy check
        if (binaryPolicy == null || !binaryPolicy.isAvailable())
            return TurkuFfmpegManualRunEvidenceResult.notAttempted(
                    TurkuFfmpegManualRunEvidenceStatus.FFMPEG_UNAVAILABLE);

        // 5. Execute via runner (gate inside)
        var processResult = runner.execute(request, binaryPolicy);

        // 6. Map result
        return switch (processResult.getStatus()) {
            case PROCESS_COMPLETED_SAFE -> {
                var outputRef = processResult.getOutputRef();
                var metadata = new TurkuFfmpegFixtureMetadata(outputRef, request.getFormat(),
                        request.getDurationSeconds(), request.getFps(),
                        request.getWidth(), request.getHeight(), request.getMaxOutputSizeMB());
                yield TurkuFfmpegManualRunEvidenceResult.succeeded(outputRef, metadata,
                        processResult.getDurationMs());
            }
            case GATE_DENIED -> TurkuFfmpegManualRunEvidenceResult.notAttempted(
                    TurkuFfmpegManualRunEvidenceStatus.GATE_DENIED);
            case PROCESS_SKIPPED -> TurkuFfmpegManualRunEvidenceResult.notAttempted(
                    TurkuFfmpegManualRunEvidenceStatus.PROCESS_DISABLED);
            default -> TurkuFfmpegManualRunEvidenceResult.failedSafe();
        };
    }

    public boolean isManualRunEnabled() { return manualRunEnabled; }
}
