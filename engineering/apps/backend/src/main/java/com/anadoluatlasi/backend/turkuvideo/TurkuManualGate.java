package com.anadoluatlasi.backend.turkuvideo;

/**
 * RPE-004: Manual gate metadata — label/instruction only.
 */
public final class TurkuManualGate {
    private final int order;
    private final String gateLabel;
    private final String instruction;
    private final boolean passed;
    private final String blockedReason;

    public TurkuManualGate(int order, String gateLabel, String instruction, boolean passed, String blockedReason) {
        this.order = order;
        this.gateLabel = gateLabel;
        this.instruction = instruction;
        this.passed = passed;
        this.blockedReason = blockedReason;
    }

    public int getOrder() { return order; }
    public String getGateLabel() { return gateLabel; }
    public String getInstruction() { return instruction; }
    public boolean isPassed() { return passed; }
    public String getBlockedReason() { return blockedReason; }
}
