package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-028 Task 02: Dev/test adapter. Calls gate before any result.
 * No ProcessBuilder in this class — boundary contract only.
 */
public class TurkuFfmpegDevTestAdapter {

    private final TurkuFfmpegExecutionGate gate;

    public TurkuFfmpegDevTestAdapter(TurkuFfmpegExecutionGate gate) {
        this.gate = gate != null ? gate : new TurkuFfmpegExecutionGate();
    }

    public TurkuFfmpegDevTestAdapter() {
        this(new TurkuFfmpegExecutionGate());
    }

    /**
     * Execute through gate. Returns safe boundary result + output ref if allowed.
     */
    public TurkuFfmpegDevTestAdapterResult execute(TurkuFfmpegBinaryPolicy binary,
            String jobRef, String queueRef, String reviewRef) {
        var gateResult = gate.evaluate(binary, jobRef, queueRef, reviewRef);

        if (gateResult.getStatus() == TurkuFfmpegBoundaryResultStatus.DENIED)
            return TurkuFfmpegDevTestAdapterResult.denied(gateResult);

        // Gate allowed — produce safe output ref (no real process in this class)
        var outputRef = TurkuFfmpegInternalOutputRef.create();
        return TurkuFfmpegDevTestAdapterResult.completed(gateResult, outputRef);
    }

    public TurkuFfmpegExecutionGate getGate() { return gate; }
}
