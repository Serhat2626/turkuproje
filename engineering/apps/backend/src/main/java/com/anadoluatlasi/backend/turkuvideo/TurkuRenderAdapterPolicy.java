package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-023 Task 02: Render adapter policy.
 * Enforces adapter selection rules and safety gates.
 */
public final class TurkuRenderAdapterPolicy {

    private static final List<String> FORBIDDEN_ARG_PATTERNS = List.of(
            ";", "&&", "||", "|>", "$(", "`", "\\n", "\\r",
            ">", ">>", "<", "2>", "&>",
            "rm ", "del ", "format ", "mkfs",
            "curl ", "wget ", "nc ",
            "http://", "https://", "ftp://",
            "file://", "s3://", "gs://", "az://");

    private static final List<String> FORBIDDEN_INPUT_PATTERNS = List.of(
            "publicUrl", "signedUrl", "cdnUrl", "storageKey",
            "base64", "data:image", "data:video", "data:audio",
            "Bearer ", "Authorization:", "token=", "apiKey=",
            "password=", "secret=");

    public boolean isShellMetacharacter(String arg) {
        if (arg == null) return false;
        return FORBIDDEN_ARG_PATTERNS.stream().anyMatch(arg::contains);
    }

    public boolean isForbiddenInput(String input) {
        if (input == null) return false;
        return FORBIDDEN_INPUT_PATTERNS.stream().anyMatch(input::contains);
    }

    public TurkuRenderAdapterMode resolveMode(boolean ffmpegEnabled, boolean configPresent) {
        if (!ffmpegEnabled) return TurkuRenderAdapterMode.STUB;
        if (!configPresent) return TurkuRenderAdapterMode.FFMPEG_CONFIG_MISSING;
        return TurkuRenderAdapterMode.FFMPEG_READY;
    }

    public List<String> getForbiddenArgPatterns() {
        return FORBIDDEN_ARG_PATTERNS;
    }

    public List<String> getForbiddenInputPatterns() {
        return FORBIDDEN_INPUT_PATTERNS;
    }
}
