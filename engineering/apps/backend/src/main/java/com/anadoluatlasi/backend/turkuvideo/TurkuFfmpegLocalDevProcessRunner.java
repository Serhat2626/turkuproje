package com.anadoluatlasi.backend.turkuvideo;

import java.util.concurrent.TimeUnit;

/**
 * FP-030 Task 01: ONLY class allowed to use ProcessBuilder.
 * Gated: must call FP-028 gate first.
 * Denied for STAGING/PROD/UNKNOWN.
 * Tests use fake runner (processEnabled=false by default).
 */
public class TurkuFfmpegLocalDevProcessRunner {

    private final TurkuFfmpegExecutionGate gate;
    private final boolean processEnabled;

    /**
     * @param gate FP-028 execution gate
     * @param processEnabled true only for real LOCAL/DEV/TEST manual runs.
     *                       Default false — CI never enables this.
     */
    public TurkuFfmpegLocalDevProcessRunner(TurkuFfmpegExecutionGate gate, boolean processEnabled) {
        this.gate = gate != null ? gate : new TurkuFfmpegExecutionGate();
        this.processEnabled = processEnabled;
    }

    public TurkuFfmpegLocalDevProcessRunner(TurkuFfmpegExecutionGate gate) {
        this(gate, false);
    }

    public TurkuFfmpegLocalDevProcessRunner() {
        this(new TurkuFfmpegExecutionGate(), false);
    }

    /**
     * Execute FFmpeg process behind gate.
     */
    public TurkuFfmpegProcessResult execute(TurkuFfmpegProcessRequest request,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        // 1. Validate request
        if (request == null || !request.isValid())
            return TurkuFfmpegProcessResult.invalid(TurkuFfmpegProcessReasonCode.FORMAT_NOT_ALLOWED);

        // 2. Gate check
        var gateResult = gate.evaluate(binaryPolicy,
                request.getJobRef(), null, null);
        if (gateResult.getStatus() == TurkuFfmpegBoundaryResultStatus.DENIED) {
            var reason = mapGateReason(gateResult.getReasonCode());
            return TurkuFfmpegProcessResult.denied(reason);
        }

        // 3. If not real mode or process not enabled → skip
        if (!processEnabled || gateResult.getMode() != TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY)
            return TurkuFfmpegProcessResult.skipped();

        // 4. Real process execution — LOCAL/DEV/TEST only
        return executeProcess(request);
    }

    /**
     * Real process execution — version probe. Isolated in this one method.
     * stdout/stderr are consumed but NOT exposed in result.
     */
    private TurkuFfmpegProcessResult executeProcess(TurkuFfmpegProcessRequest request) {
        long startMs = System.currentTimeMillis();
        try {
            // Build process — internal only, paths not exposed
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Consume output (not exposed)
            process.getInputStream().readAllBytes();

            boolean finished = process.waitFor(request.getTimeoutSeconds(), TimeUnit.SECONDS);
            long durationMs = System.currentTimeMillis() - startMs;

            if (!finished) {
                process.destroyForcibly();
                return TurkuFfmpegProcessResult.timedOut(durationMs);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0)
                return TurkuFfmpegProcessResult.failed();

            var outputRef = TurkuFfmpegInternalOutputRef.create().toOpaqueRef();
            return TurkuFfmpegProcessResult.completed(outputRef, durationMs);
        } catch (Exception e) {
            return TurkuFfmpegProcessResult.failed();
        }
    }

    /**
     * FP-034: Real video clip generation using synthetic testsrc input.
     * Uses ProcessBuilder — output file is temp, path NOT exposed.
     * Returns only opaque ref + safe metadata.
     */
    public TurkuFfmpegProcessResult generateClip(TurkuFfmpegProcessRequest request,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        // 1. Validate
        if (request == null || !request.isValid())
            return TurkuFfmpegProcessResult.invalid(TurkuFfmpegProcessReasonCode.FORMAT_NOT_ALLOWED);

        // 2. Gate check
        var gateResult = gate.evaluate(binaryPolicy, request.getJobRef(), null, null);
        if (gateResult.getStatus() == TurkuFfmpegBoundaryResultStatus.DENIED) {
            return TurkuFfmpegProcessResult.denied(mapGateReason(gateResult.getReasonCode()));
        }

        // 3. Not enabled → skip
        if (!processEnabled || gateResult.getMode() != TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY)
            return TurkuFfmpegProcessResult.skipped();

        // 4. Real clip generation
        return executeClipGeneration(request);
    }

    /**
     * Real clip generation. Synthetic testsrc input — no copyrighted media.
     * Output path is temp, consumed internally, NOT exposed.
     */
    private TurkuFfmpegProcessResult executeClipGeneration(TurkuFfmpegProcessRequest request) {
        long startMs = System.currentTimeMillis();
        java.io.File tempFile = null;
        try {
            // Create temp output — path never exposed to DTO/API/UI
            tempFile = java.io.File.createTempFile("turku_internal_clip_", "." + request.getFormat());
            tempFile.deleteOnExit();

            // Build synthetic testsrc command — no copyrighted media
            String testSrc = String.format("testsrc=duration=%d:size=%dx%d:rate=%d",
                    request.getDurationSeconds(), request.getWidth(), request.getHeight(), request.getFps());

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-f", "lavfi", "-i", testSrc,
                    "-f", "lavfi", "-i", "anullsrc=r=44100:cl=mono",
                    "-t", String.valueOf(request.getDurationSeconds()),
                    "-c:v", "libx264", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-shortest",
                    tempFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Consume output (not exposed)
            process.getInputStream().readAllBytes();

            boolean finished = process.waitFor(request.getTimeoutSeconds(), TimeUnit.SECONDS);
            long durationMs = System.currentTimeMillis() - startMs;

            if (!finished) {
                process.destroyForcibly();
                return TurkuFfmpegProcessResult.timedOut(durationMs);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0)
                return TurkuFfmpegProcessResult.failed();

            // Verify file exists and has content
            long fileSize = tempFile.length();
            if (fileSize <= 0)
                return TurkuFfmpegProcessResult.failed();

            // Return opaque ref only — file path NOT exposed
            var outputRef = TurkuFfmpegInternalOutputRef.create().toOpaqueRef();
            return TurkuFfmpegProcessResult.completed(outputRef, durationMs);
        } catch (Exception e) {
            return TurkuFfmpegProcessResult.failed();
        }
    }

    private TurkuFfmpegProcessReasonCode mapGateReason(TurkuFfmpegBoundaryReasonCode gateReason) {
        return switch (gateReason) {
            case MODE_DISABLED -> TurkuFfmpegProcessReasonCode.GATE_DENIED_MODE;
            case ENVIRONMENT_DENIED -> TurkuFfmpegProcessReasonCode.GATE_DENIED_ENVIRONMENT;
            case INTERNAL_FLAG_NOT_SET -> TurkuFfmpegProcessReasonCode.GATE_DENIED_FLAG;
            case BINARY_UNAVAILABLE -> TurkuFfmpegProcessReasonCode.BINARY_UNAVAILABLE;
            default -> TurkuFfmpegProcessReasonCode.GATE_DENIED_MODE;
        };
    }

    public boolean isProcessEnabled() { return processEnabled; }
}
