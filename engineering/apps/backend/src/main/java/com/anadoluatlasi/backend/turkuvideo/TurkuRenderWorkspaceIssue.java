package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-024 Task 03: Workspace validation issue.
 */
public final class TurkuRenderWorkspaceIssue {

    public enum Code {
        PATH_TRAVERSAL,
        ABSOLUTE_PATH,
        RELATIVE_PATH,
        WINDOWS_PATH,
        UNIX_PATH,
        URL_DETECTED,
        STORAGE_KEY_DETECTED,
        BASE64_DETECTED,
        SECRET_DETECTED,
        COMMAND_DETECTED,
        EXTENSION_NOT_ALLOWED,
        REF_EMPTY,
        REF_UNSAFE
    }

    private final Code code;
    private final String field;
    private final String safeMessage;

    public TurkuRenderWorkspaceIssue(Code code, String field, String safeMessage) {
        this.code = code;
        this.field = field;
        this.safeMessage = safeMessage;
    }

    public Code getCode() { return code; }
    public String getField() { return field; }
    public String getSafeMessage() { return safeMessage; }
}
