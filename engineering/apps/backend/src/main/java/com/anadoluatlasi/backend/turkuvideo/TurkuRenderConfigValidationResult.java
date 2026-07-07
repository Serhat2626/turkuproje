package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-024 Task 02: Config validation result.
 */
public final class TurkuRenderConfigValidationResult {
    private final boolean valid;
    private final TurkuRenderConfigMode resolvedMode;
    private final List<TurkuRenderConfigValidationIssue> issues;

    public TurkuRenderConfigValidationResult(boolean valid,
                                              TurkuRenderConfigMode resolvedMode,
                                              List<TurkuRenderConfigValidationIssue> issues) {
        this.valid = valid;
        this.resolvedMode = resolvedMode;
        this.issues = issues != null ? List.copyOf(issues) : List.of();
    }

    public boolean isValid() { return valid; }
    public TurkuRenderConfigMode getResolvedMode() { return resolvedMode; }
    public List<TurkuRenderConfigValidationIssue> getIssues() { return issues; }

    public static TurkuRenderConfigValidationResult missing() {
        return new TurkuRenderConfigValidationResult(false,
                TurkuRenderConfigMode.FFMPEG_CONFIG_MISSING,
                List.of(new TurkuRenderConfigValidationIssue(
                        TurkuRenderConfigFailureCode.CONFIG_MISSING,
                        "config", "Render config not present — using stub")));
    }

    public static TurkuRenderConfigValidationResult disabled() {
        return new TurkuRenderConfigValidationResult(false,
                TurkuRenderConfigMode.FFMPEG_DISABLED,
                List.of(new TurkuRenderConfigValidationIssue(
                        TurkuRenderConfigFailureCode.ENABLED_FLAG_MISSING,
                        "enabled", "FFmpeg disabled — using stub")));
    }

    public static TurkuRenderConfigValidationResult stubOnly() {
        return new TurkuRenderConfigValidationResult(true,
                TurkuRenderConfigMode.STUB_ONLY, List.of());
    }
}
