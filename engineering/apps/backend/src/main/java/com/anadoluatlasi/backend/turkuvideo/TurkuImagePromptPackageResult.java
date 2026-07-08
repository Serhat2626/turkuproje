package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * RPE-005: Image prompt package result — aggregates scene prompts + manual approval gate.
 */
public final class TurkuImagePromptPackageResult {
    private final TurkuImagePromptPackageStatus status;
    private final List<TurkuImagePromptPackageIssueCode> issues;
    private final String opaquePromptPackageRef;
    private final String opaqueExecutionPackageRef;
    private final String safeTurkuTitleLabel;
    private final List<TurkuSceneImagePrompt> scenePrompts;
    private final int totalScenes;
    private final int blockedScenes;
    private final boolean manualApprovalGranted;
    private final String approvalGateLabel;
    private final String validationSummary;

    public TurkuImagePromptPackageResult(
            TurkuImagePromptPackageStatus status, List<TurkuImagePromptPackageIssueCode> issues,
            String opaquePromptPackageRef, String opaqueExecutionPackageRef,
            String safeTurkuTitleLabel, List<TurkuSceneImagePrompt> scenePrompts,
            int totalScenes, int blockedScenes, boolean manualApprovalGranted,
            String approvalGateLabel, String validationSummary) {
        this.status = status;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.opaquePromptPackageRef = opaquePromptPackageRef;
        this.opaqueExecutionPackageRef = opaqueExecutionPackageRef;
        this.safeTurkuTitleLabel = safeTurkuTitleLabel;
        this.scenePrompts = scenePrompts == null ? List.of() : List.copyOf(scenePrompts);
        this.totalScenes = totalScenes;
        this.blockedScenes = blockedScenes;
        this.manualApprovalGranted = manualApprovalGranted;
        this.approvalGateLabel = approvalGateLabel;
        this.validationSummary = validationSummary;
    }

    public TurkuImagePromptPackageStatus getStatus() { return status; }
    public List<TurkuImagePromptPackageIssueCode> getIssues() { return issues; }
    public String getOpaquePromptPackageRef() { return opaquePromptPackageRef; }
    public String getOpaqueExecutionPackageRef() { return opaqueExecutionPackageRef; }
    public String getSafeTurkuTitleLabel() { return safeTurkuTitleLabel; }
    public List<TurkuSceneImagePrompt> getScenePrompts() { return scenePrompts; }
    public int getTotalScenes() { return totalScenes; }
    public int getBlockedScenes() { return blockedScenes; }
    public boolean isManualApprovalGranted() { return manualApprovalGranted; }
    public String getApprovalGateLabel() { return approvalGateLabel; }
    public String getValidationSummary() { return validationSummary; }
}
