package com.anadoluatlasi.backend.turkuvideo;

/**
 * MP-002: Single checklist item — label/instruction only.
 */
public final class TurkuProductionChecklistItem {
    private final int order;
    private final String label;
    private final String instruction;
    private final boolean prerequisiteMet;
    private final String blockedReason;

    public TurkuProductionChecklistItem(int order, String label, String instruction,
                                         boolean prerequisiteMet, String blockedReason) {
        this.order = order;
        this.label = label;
        this.instruction = instruction;
        this.prerequisiteMet = prerequisiteMet;
        this.blockedReason = blockedReason;
    }

    public int getOrder() { return order; }
    public String getLabel() { return label; }
    public String getInstruction() { return instruction; }
    public boolean isPrerequisiteMet() { return prerequisiteMet; }
    public String getBlockedReason() { return blockedReason; }
}
