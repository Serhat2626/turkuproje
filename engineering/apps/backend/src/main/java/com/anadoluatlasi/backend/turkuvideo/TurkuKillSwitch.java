package com.anadoluatlasi.backend.turkuvideo;

/**
 * LIVE-001: Kill switch — global execution blocker.
 * When active, all real provider execution is blocked regardless of other flags.
 */
public final class TurkuKillSwitch {
    private final boolean active;
    private final String reason;
    private final String activatedBy;

    public TurkuKillSwitch(boolean active, String reason, String activatedBy) {
        this.active = active;
        this.reason = reason;
        this.activatedBy = activatedBy;
    }

    /** Default: kill switch ON (blocks execution). */
    public static TurkuKillSwitch defaultActive() {
        return new TurkuKillSwitch(true, "Varsayilan: gercek saglayici yurutmesi devre disi", "SYSTEM");
    }

    public boolean isActive() { return active; }
    public String getReason() { return reason; }
    public String getActivatedBy() { return activatedBy; }
}
