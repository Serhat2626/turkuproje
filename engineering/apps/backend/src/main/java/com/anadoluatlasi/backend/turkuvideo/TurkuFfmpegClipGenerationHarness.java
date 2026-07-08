package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-034: Manual local video clip generation harness.
 * Disabled by default. Does NOT run in CI.
 * No ProcessBuilder in THIS class — delegates to runner.generateClip().
 */
public class TurkuFfmpegClipGenerationHarness {

    private final TurkuFfmpegLocalDevProcessRunner runner;
    private final boolean clipGenerationEnabled;

    public TurkuFfmpegClipGenerationHarness(TurkuFfmpegLocalDevProcessRunner runner,
            boolean clipGenerationEnabled) {
        this.runner = runner != null ? runner : new TurkuFfmpegLocalDevProcessRunner();
        this.clipGenerationEnabled = clipGenerationEnabled;
    }

    public TurkuFfmpegClipGenerationHarness() {
        this(new TurkuFfmpegLocalDevProcessRunner(), false);
    }

    /**
     * Attempt manual local video clip generation.
     */
    public TurkuFfmpegClipGenerationResult attempt(
            TurkuFfmpegProcessRequest request,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        // 1. Manual flag
        if (!clipGenerationEnabled)
            return TurkuFfmpegClipGenerationResult.notAttempted(
                    TurkuFfmpegClipGenerationStatus.MANUAL_FLAG_NOT_ENABLED);

        // 2. Request validation
        if (request == null || !request.isValid())
            return TurkuFfmpegClipGenerationResult.notAttempted(
                    TurkuFfmpegClipGenerationStatus.GATE_DENIED);

        // 3. Process enabled
        if (!runner.isProcessEnabled())
            return TurkuFfmpegClipGenerationResult.notAttempted(
                    TurkuFfmpegClipGenerationStatus.PROCESS_DISABLED);

        // 4. Binary policy
        if (binaryPolicy == null || !binaryPolicy.isAvailable())
            return TurkuFfmpegClipGenerationResult.notAttempted(
                    TurkuFfmpegClipGenerationStatus.FFMPEG_UNAVAILABLE);

        // 5. Delegate to runner for real clip generation
        var processResult = runner.generateClip(request, binaryPolicy);

        // 6. Map result
        return switch (processResult.getStatus()) {
            case PROCESS_COMPLETED_SAFE -> {
                var outputRef = processResult.getOutputRef();
                var metadata = new TurkuFfmpegFixtureMetadata(outputRef, request.getFormat(),
                        request.getDurationSeconds(), request.getFps(),
                        request.getWidth(), request.getHeight(), request.getMaxOutputSizeMB());
                if (metadata.isBoundsValid())
                    yield TurkuFfmpegClipGenerationResult.verified(outputRef, metadata,
                            processResult.getDurationMs());
                else
                    yield TurkuFfmpegClipGenerationResult.notAttempted(
                            TurkuFfmpegClipGenerationStatus.VERIFICATION_FAILED);
            }
            case GATE_DENIED -> TurkuFfmpegClipGenerationResult.notAttempted(
                    TurkuFfmpegClipGenerationStatus.GATE_DENIED);
            case PROCESS_SKIPPED -> TurkuFfmpegClipGenerationResult.notAttempted(
                    TurkuFfmpegClipGenerationStatus.PROCESS_DISABLED);
            case PROCESS_TIMED_OUT -> TurkuFfmpegClipGenerationResult.notAttempted(
                    TurkuFfmpegClipGenerationStatus.CLIP_GENERATION_TIMED_OUT);
            default -> TurkuFfmpegClipGenerationResult.failedSafe();
        };
    }

    public boolean isClipGenerationEnabled() { return clipGenerationEnabled; }
}
