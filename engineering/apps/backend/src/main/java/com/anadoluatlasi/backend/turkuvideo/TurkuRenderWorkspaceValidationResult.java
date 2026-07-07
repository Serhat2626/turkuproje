package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-024 Task 03: Workspace validation result.
 */
public final class TurkuRenderWorkspaceValidationResult {
    private final boolean valid;
    private final List<TurkuRenderWorkspaceIssue> issues;

    public TurkuRenderWorkspaceValidationResult(boolean valid,
                                                  List<TurkuRenderWorkspaceIssue> issues) {
        this.valid = valid;
        this.issues = issues != null ? List.copyOf(issues) : List.of();
    }

    public boolean isValid() { return valid; }
    public List<TurkuRenderWorkspaceIssue> getIssues() { return issues; }

    public static TurkuRenderWorkspaceValidationResult ok() {
        return new TurkuRenderWorkspaceValidationResult(true, List.of());
    }

    public static TurkuRenderWorkspaceValidationResult fail(List<TurkuRenderWorkspaceIssue> issues) {
        return new TurkuRenderWorkspaceValidationResult(false, issues);
    }
}
