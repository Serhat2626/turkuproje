package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-023 Task 02: FFmpeg command plan.
 * Safe, pre-validated argument list. No shell string.
 * This is a representation only — no process execution.
 */
public final class TurkuFfmpegCommandPlan {
    private final String executableToken;
    private final List<String> safeArgs;
    private final TurkuFfmpegExecutionLimits limits;
    private final TurkuFfmpegInputSpec inputSpec;
    private final TurkuFfmpegOutputSpec outputSpec;
    private final boolean validated;
    private final String validationError;

    public TurkuFfmpegCommandPlan(String executableToken, List<String> safeArgs,
                                   TurkuFfmpegExecutionLimits limits,
                                   TurkuFfmpegInputSpec inputSpec,
                                   TurkuFfmpegOutputSpec outputSpec,
                                   boolean validated, String validationError) {
        this.executableToken = executableToken;
        this.safeArgs = safeArgs != null ? List.copyOf(safeArgs) : List.of();
        this.limits = limits;
        this.inputSpec = inputSpec;
        this.outputSpec = outputSpec;
        this.validated = validated;
        this.validationError = validationError;
    }

    public String getExecutableToken() { return executableToken; }
    public List<String> getSafeArgs() { return safeArgs; }
    public TurkuFfmpegExecutionLimits getLimits() { return limits; }
    public TurkuFfmpegInputSpec getInputSpec() { return inputSpec; }
    public TurkuFfmpegOutputSpec getOutputSpec() { return outputSpec; }
    public boolean isValidated() { return validated; }
    public String getValidationError() { return validationError; }
}
