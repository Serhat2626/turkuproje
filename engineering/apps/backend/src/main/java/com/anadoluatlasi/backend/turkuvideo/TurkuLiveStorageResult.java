package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * LIVE-003: Storage handoff + verification result.
 */
public final class TurkuLiveStorageResult {
    private final TurkuLiveStorageStatus status;
    private final List<TurkuLiveStorageIssueCode> issues;
    private final String opaqueStorageRef;
    private final String opaqueGenerationRef;
    private final List<TurkuLiveStoredArtifact> storedArtifacts;
    private final int totalScenes;
    private final int storedCount;
    private final int verifiedCount;
    private final List<String> blockedReasons;
    private final String validationSummary;

    public TurkuLiveStorageResult(
            TurkuLiveStorageStatus status, List<TurkuLiveStorageIssueCode> issues,
            String opaqueStorageRef, String opaqueGenerationRef,
            List<TurkuLiveStoredArtifact> storedArtifacts,
            int totalScenes, int storedCount, int verifiedCount,
            List<String> blockedReasons, String validationSummary) {
        this.status = status;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.opaqueStorageRef = opaqueStorageRef;
        this.opaqueGenerationRef = opaqueGenerationRef;
        this.storedArtifacts = storedArtifacts == null ? List.of() : List.copyOf(storedArtifacts);
        this.totalScenes = totalScenes;
        this.storedCount = storedCount;
        this.verifiedCount = verifiedCount;
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.validationSummary = validationSummary;
    }

    public TurkuLiveStorageStatus getStatus() { return status; }
    public List<TurkuLiveStorageIssueCode> getIssues() { return issues; }
    public String getOpaqueStorageRef() { return opaqueStorageRef; }
    public String getOpaqueGenerationRef() { return opaqueGenerationRef; }
    public List<TurkuLiveStoredArtifact> getStoredArtifacts() { return storedArtifacts; }
    public int getTotalScenes() { return totalScenes; }
    public int getStoredCount() { return storedCount; }
    public int getVerifiedCount() { return verifiedCount; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public String getValidationSummary() { return validationSummary; }
}
