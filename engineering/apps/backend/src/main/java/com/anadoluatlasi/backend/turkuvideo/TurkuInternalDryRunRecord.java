package com.anadoluatlasi.backend.turkuvideo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * FP-051: Safe internal dry-run record. Metadata-only, no forbidden fields.
 * No filePath/stdout/stderr/rawLog/storageKey/publicUrl/token/prompt/lyrics.
 */
public final class TurkuInternalDryRunRecord {
    private final UUID recordId;
    private final String opaqueRecordRef;
    private final String opaqueDryRunRef;
    private final String opaqueStoryboardRef;
    private final String opaqueReviewPackageRef;
    private final TurkuInternalDryRunRecordStatus status;
    private final List<TurkuInternalDryRunRecordIssueCode> issueCodes;
    private final int sceneCount;
    private final int generatedClipCount;
    private final int inputClipCount;
    private final boolean stitchedArtifactPresent;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TurkuInternalDryRunRecord(UUID recordId, String opaqueRecordRef,
            String opaqueDryRunRef, String opaqueStoryboardRef,
            String opaqueReviewPackageRef,
            TurkuInternalDryRunRecordStatus status,
            List<TurkuInternalDryRunRecordIssueCode> issueCodes,
            int sceneCount, int generatedClipCount, int inputClipCount,
            boolean stitchedArtifactPresent,
            int durationSeconds, int fps, int width, int height, long sizeBytes,
            Instant createdAt, Instant updatedAt) {
        this.recordId = recordId;
        this.opaqueRecordRef = opaqueRecordRef;
        this.opaqueDryRunRef = opaqueDryRunRef;
        this.opaqueStoryboardRef = opaqueStoryboardRef;
        this.opaqueReviewPackageRef = opaqueReviewPackageRef;
        this.status = status;
        this.issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
        this.sceneCount = sceneCount;
        this.generatedClipCount = generatedClipCount;
        this.inputClipCount = inputClipCount;
        this.stitchedArtifactPresent = stitchedArtifactPresent;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getRecordId() { return recordId; }
    public String getOpaqueRecordRef() { return opaqueRecordRef; }
    public String getOpaqueDryRunRef() { return opaqueDryRunRef; }
    public String getOpaqueStoryboardRef() { return opaqueStoryboardRef; }
    public String getOpaqueReviewPackageRef() { return opaqueReviewPackageRef; }
    public TurkuInternalDryRunRecordStatus getStatus() { return status; }
    public List<TurkuInternalDryRunRecordIssueCode> getIssueCodes() { return issueCodes; }
    public int getSceneCount() { return sceneCount; }
    public int getGeneratedClipCount() { return generatedClipCount; }
    public int getInputClipCount() { return inputClipCount; }
    public boolean isStitchedArtifactPresent() { return stitchedArtifactPresent; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
