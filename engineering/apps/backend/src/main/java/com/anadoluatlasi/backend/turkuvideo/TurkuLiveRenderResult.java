package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * LIVE-004: Render execution result.
 */
public final class TurkuLiveRenderResult {
    private final TurkuLiveRenderStatus status;
    private final TurkuLiveRenderMode mode;
    private final List<TurkuLiveRenderIssueCode> issues;
    private final String opaqueRenderJobRef;
    private final String opaqueStorageRef;
    private final boolean killSwitchActive;
    private final boolean operatorGatePassed;
    private final boolean artifactsReady;
    private final TurkuLiveRenderArtifact renderArtifact;
    private final int inputSceneCount;
    private final List<String> blockedReasons;
    private final String sanitizedRenderSummary;
    private final String validationSummary;

    public TurkuLiveRenderResult(
            TurkuLiveRenderStatus status, TurkuLiveRenderMode mode,
            List<TurkuLiveRenderIssueCode> issues,
            String opaqueRenderJobRef, String opaqueStorageRef,
            boolean killSwitchActive, boolean operatorGatePassed, boolean artifactsReady,
            TurkuLiveRenderArtifact renderArtifact, int inputSceneCount,
            List<String> blockedReasons, String sanitizedRenderSummary, String validationSummary) {
        this.status = status;
        this.mode = mode;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.opaqueRenderJobRef = opaqueRenderJobRef;
        this.opaqueStorageRef = opaqueStorageRef;
        this.killSwitchActive = killSwitchActive;
        this.operatorGatePassed = operatorGatePassed;
        this.artifactsReady = artifactsReady;
        this.renderArtifact = renderArtifact;
        this.inputSceneCount = inputSceneCount;
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.sanitizedRenderSummary = sanitizedRenderSummary;
        this.validationSummary = validationSummary;
    }

    public TurkuLiveRenderStatus getStatus() { return status; }
    public TurkuLiveRenderMode getMode() { return mode; }
    public List<TurkuLiveRenderIssueCode> getIssues() { return issues; }
    public String getOpaqueRenderJobRef() { return opaqueRenderJobRef; }
    public String getOpaqueStorageRef() { return opaqueStorageRef; }
    public boolean isKillSwitchActive() { return killSwitchActive; }
    public boolean isOperatorGatePassed() { return operatorGatePassed; }
    public boolean isArtifactsReady() { return artifactsReady; }
    public TurkuLiveRenderArtifact getRenderArtifact() { return renderArtifact; }
    public int getInputSceneCount() { return inputSceneCount; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public String getSanitizedRenderSummary() { return sanitizedRenderSummary; }
    public String getValidationSummary() { return validationSummary; }
}
