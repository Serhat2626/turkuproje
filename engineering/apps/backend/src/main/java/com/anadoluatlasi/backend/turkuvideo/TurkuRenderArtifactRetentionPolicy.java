package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 04: Render artifact retention policy.
 * Metadata-only. No real file deletion.
 */
public final class TurkuRenderArtifactRetentionPolicy {
    public static final int DEFAULT_RETENTION_HOURS = 24;
    public static final int MAX_RETENTION_HOURS = 168;
    public static final int MIN_RETENTION_HOURS = 1;

    private final int retentionHours;
    private final boolean autoCleanupEnabled;
    private final String policyLabel;

    public TurkuRenderArtifactRetentionPolicy() {
        this(DEFAULT_RETENTION_HOURS, false, "default-24h");
    }

    public TurkuRenderArtifactRetentionPolicy(int retentionHours, boolean autoCleanupEnabled,
                                                String policyLabel) {
        this.retentionHours = Math.min(Math.max(retentionHours, MIN_RETENTION_HOURS), MAX_RETENTION_HOURS);
        this.autoCleanupEnabled = autoCleanupEnabled;
        this.policyLabel = policyLabel != null ? policyLabel : "default";
    }

    public int getRetentionHours() { return retentionHours; }
    public boolean isAutoCleanupEnabled() { return autoCleanupEnabled; }
    public String getPolicyLabel() { return policyLabel; }
}
