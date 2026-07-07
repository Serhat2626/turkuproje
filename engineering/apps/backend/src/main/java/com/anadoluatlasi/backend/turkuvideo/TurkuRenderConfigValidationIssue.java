package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-024 Task 02: Config validation issue.
 * Safe issue representation — no raw paths/secrets.
 */
public final class TurkuRenderConfigValidationIssue {
    private final TurkuRenderConfigFailureCode code;
    private final String field;
    private final String safeMessage;

    public TurkuRenderConfigValidationIssue(TurkuRenderConfigFailureCode code,
                                             String field, String safeMessage) {
        this.code = code;
        this.field = field;
        this.safeMessage = safeMessage;
    }

    public TurkuRenderConfigFailureCode getCode() { return code; }
    public String getField() { return field; }
    public String getSafeMessage() { return safeMessage; }
}
