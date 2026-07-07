package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 04: Per-scene image result.
 * Uses opaque safe internal reference — no base64, no public URL, no storage key.
 */
public final class TurkuSceneImageResult {

    private final int sceneNo;
    private final String safeInternalRef;
    private final boolean success;
    private final String safeErrorCode;

    public TurkuSceneImageResult(int sceneNo, String safeInternalRef,
                                  boolean success, String safeErrorCode) {
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
