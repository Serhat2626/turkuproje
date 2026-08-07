package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FP-019 Task 04: Per-scene image result.
 * Uses opaque safe internal reference — no base64, no public URL, no storage key.
 */
public final class TurkuSceneImageResult {

    private final int sceneNo;
    private final String safeInternalRef;
    private final boolean success;
    private final String safeErrorCode;

    @JsonCreator
    public TurkuSceneImageResult(@JsonProperty("sceneNo") int sceneNo,
                                  @JsonProperty("safeInternalRef") String safeInternalRef,
                                  @JsonProperty("success") boolean success,
                                  @JsonProperty("safeErrorCode") String safeErrorCode) {
        this.sceneNo = sceneNo;
        this.safeInternalRef = safeInternalRef;
        this.success = success;
        this.safeErrorCode = safeErrorCode;
    }

    public int getSceneNo() { return sceneNo; }
    public String getSafeInternalRef() { return safeInternalRef; }
    public boolean isSuccess() { return success; }
    public String getSafeErrorCode() { return safeErrorCode; }
}
