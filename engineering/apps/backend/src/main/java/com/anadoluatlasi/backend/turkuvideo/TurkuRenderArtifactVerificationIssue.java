package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-024 Task 05: Artifact verification issue.
 */
public final class TurkuRenderArtifactVerificationIssue {

    public enum Code {
        FORMAT_NOT_ALLOWED,
        DURATION_OUT_OF_RANGE,
        SIZE_OUT_OF_RANGE,
        RESOLUTION_OUT_OF_RANGE,
        FPS_OUT_OF_RANGE,
        REF_MISSING,
        REF_UNSAFE_PATH,
        REF_UNSAFE_URL,
        REF_UNSAFE_STORAGE,
        REF_UNSAFE_BASE64,
        REF_UNSAFE_SECRET,
        REF_UNSAFE_COMMAND,
        REF_UNSAFE_SHELL,
        METADATA_MISSING,
        RENDERER_MODE_UNKNOWN,
        GENERAL_FAILURE
    }

    private final Code code;
    private final String field;
    private final String safeMessage;

    public TurkuRenderArtifactVerificationIssue(Code code, String field, String safeMessage) {
        this.code = code;
        this.field = field;
        this.safeMessage = safeMessage;
    }

    public Code getCode() { return code; }
    public String getField() { return field; }
    public String getSafeMessage() { return safeMessage; }
}
