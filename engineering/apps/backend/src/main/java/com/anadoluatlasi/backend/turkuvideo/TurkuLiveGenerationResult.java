package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * LIVE-002: Live image generation execution result.
 */
public final class TurkuLiveGenerationResult {
    private final TurkuLiveGenerationStatus status;
    private final TurkuLiveGenerationMode mode;
    private final String opaqueGenerationRef;
    private final String opaquePromptPackageRef;
    private final boolean killSwitchActive;
    private final boolean operatorGatePassed;
    private final boolean providerReady;
    private final List<TurkuLiveImageResultMetadata> results;
    private final int totalScenes;
    private final int generatedCount;
    private final List<String> blockedReasons;
    private final String sanitizedProviderSummary;
    private final String validationSummary;

    public TurkuLiveGenerationResult(
            TurkuLiveGenerationStatus status, TurkuLiveGenerationMode mode,
            String opaqueGenerationRef, String opaquePromptPackageRef,
            boolean killSwitchActive, boolean operatorGatePassed, boolean providerReady,
            List<TurkuLiveImageResultMetadata> results, int totalScenes, int generatedCount,
            List<String> blockedReasons, String sanitizedProviderSummary, String validationSummary) {
        this.status = status;
        this.mode = mode;
        this.opaqueGenerationRef = opaqueGenerationRef;
        this.opaquePromptPackageRef = opaquePromptPackageRef;
        this.killSwitchActive = killSwitchActive;
        this.operatorGatePassed = operatorGatePassed;
        this.providerReady = providerReady;
        this.results = results == null ? List.of() : List.copyOf(results);
        this.totalScenes = totalScenes;
        this.generatedCount = generatedCount;
        this.blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
        this.sanitizedProviderSummary = sanitizedProviderSummary;
        this.validationSummary = validationSummary;
    }

    public TurkuLiveGenerationStatus getStatus() { return status; }
    public TurkuLiveGenerationMode getMode() { return mode; }
    public String getOpaqueGenerationRef() { return opaqueGenerationRef; }
    public String getOpaquePromptPackageRef() { return opaquePromptPackageRef; }
    public boolean isKillSwitchActive() { return killSwitchActive; }
    public boolean isOperatorGatePassed() { return operatorGatePassed; }
    public boolean isProviderReady() { return providerReady; }
    public List<TurkuLiveImageResultMetadata> getResults() { return results; }
    public int getTotalScenes() { return totalScenes; }
    public int getGeneratedCount() { return generatedCount; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public String getSanitizedProviderSummary() { return sanitizedProviderSummary; }
    public String getValidationSummary() { return validationSummary; }
}
