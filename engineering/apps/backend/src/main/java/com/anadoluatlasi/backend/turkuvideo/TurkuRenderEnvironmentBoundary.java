package com.anadoluatlasi.backend.turkuvideo;

/**
 * RPE-003: Render environment boundary placeholder.
 * Metadata only — no real environment, path, or command.
 */
public final class TurkuRenderEnvironmentBoundary {
    private final String environmentLabel;
    private final boolean environmentConfigured;
    private final String configurationStatusLabel;

    public TurkuRenderEnvironmentBoundary(String environmentLabel, boolean environmentConfigured, String configurationStatusLabel) {
        this.environmentLabel = environmentLabel;
        this.environmentConfigured = environmentConfigured;
        this.configurationStatusLabel = configurationStatusLabel;
    }

    public String getEnvironmentLabel() { return environmentLabel; }
    public boolean isEnvironmentConfigured() { return environmentConfigured; }
    public String getConfigurationStatusLabel() { return configurationStatusLabel; }
}
