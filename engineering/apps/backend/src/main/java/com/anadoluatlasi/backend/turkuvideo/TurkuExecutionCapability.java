package com.anadoluatlasi.backend.turkuvideo;

/**
 * RPE-001: Disabled execution capability flag.
 * All capabilities disabled by default in RPE-001.
 */
public final class TurkuExecutionCapability {
    private final String capabilityLabel;
    private final boolean enabled;
    private final String disabledReason;

    public TurkuExecutionCapability(String capabilityLabel, boolean enabled, String disabledReason) {
        this.capabilityLabel = capabilityLabel;
        this.enabled = enabled;
        this.disabledReason = disabledReason;
    }

    public String getCapabilityLabel() { return capabilityLabel; }
    public boolean isEnabled() { return enabled; }
    public String getDisabledReason() { return disabledReason; }
}
