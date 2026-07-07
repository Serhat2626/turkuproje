package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 04: Request for image batch preparation.
 */
public final class TurkuImageBatchPreparationRequest {
    private String providerHint;

    public TurkuImageBatchPreparationRequest() {}
    public String getProviderHint() { return providerHint; }
    public void setProviderHint(String v) { this.providerHint = v; }
}
