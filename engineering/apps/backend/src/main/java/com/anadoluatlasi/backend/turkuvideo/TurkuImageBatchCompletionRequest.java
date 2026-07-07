package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 04: Request for image batch completion/partial marking.
 */
public final class TurkuImageBatchCompletionRequest {
    private boolean partial;
    private String safeCompletionNote;

    public TurkuImageBatchCompletionRequest() {}
    public boolean isPartial() { return partial; }
    public void setPartial(boolean v) { this.partial = v; }
    public String getSafeCompletionNote() { return safeCompletionNote; }
    public void setSafeCompletionNote(String v) { this.safeCompletionNote = v; }
}
