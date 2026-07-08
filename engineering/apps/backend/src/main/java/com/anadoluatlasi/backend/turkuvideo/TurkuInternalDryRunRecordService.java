package com.anadoluatlasi.backend.turkuvideo;

import java.time.Instant;
import java.util.*;

/**
 * FP-051: Service that creates safe audit-only dry-run records from FP-049 responses.
 * In-memory only; no persistence/migration required.
 */
public class TurkuInternalDryRunRecordService {

    private final List<TurkuInternalDryRunRecord> records = new ArrayList<>();

    /**
     * Create a dry-run record from a safe FP-049 trigger response.
     */
    public TurkuInternalDryRunRecord createFromDryRunResponse(
            TurkuInternalDryRunTriggerResponse response) {
        if (response == null) {
            return createRejectedRecord("trigger:null", List.of(TurkuInternalDryRunRecordIssueCode.TRIGGER_REJECTED));
        }

        TurkuInternalDryRunRecordStatus status = mapStatus(response);
        List<TurkuInternalDryRunRecordIssueCode> issues = mapIssues(response);

        var now = Instant.now();
        var record = new TurkuInternalDryRunRecord(
                UUID.randomUUID(),
                "dryrec:" + UUID.randomUUID().toString().substring(0, 8),
                response.getOpaqueChainRef() != null ? response.getOpaqueChainRef() : "unknown",
                response.getOpaqueStoryboardRef() != null ? response.getOpaqueStoryboardRef() : "unknown",
                "review:" + UUID.randomUUID().toString().substring(0, 8),
                status, issues,
                response.getSceneCount(),
                response.getGeneratedClipCount(),
                response.getSceneCount(), // inputClipCount = sceneCount for dry-run
                response.isStitchedArtifactPresent(),
                response.getTotalDurationSeconds(),
                response.getMaxFps(),
                response.getMaxWidth(),
                response.getMaxHeight(),
                response.getTotalSizeBytes(),
                now, now);
        records.add(record);
        return record;
    }

    /**
     * Create a rejected record for invalid/unsafe triggers.
     */
    public TurkuInternalDryRunRecord createRejectedRecord(
            String opaqueRef, List<TurkuInternalDryRunRecordIssueCode> issues) {
        var now = Instant.now();
        var record = new TurkuInternalDryRunRecord(
                UUID.randomUUID(),
                "dryrec:" + UUID.randomUUID().toString().substring(0, 8),
                opaqueRef, "unknown", "none",
                TurkuInternalDryRunRecordStatus.REJECTED_UNSAFE,
                issues, 0, 0, 0, false, 0, 0, 0, 0, 0, now, now);
        records.add(record);
        return record;
    }

    /**
     * Get safe summary for admin history view.
     */
    public TurkuInternalDryRunRecordSummary getSummary(TurkuInternalDryRunRecord record) {
        return new TurkuInternalDryRunRecordSummary(record);
    }

    /**
     * Get all records as safe summaries for admin history.
     */
    public List<TurkuInternalDryRunRecordSummary> getHistory() {
        return records.stream()
                .map(TurkuInternalDryRunRecordSummary::new)
                .toList();
    }

    public int getRecordCount() { return records.size(); }

    private TurkuInternalDryRunRecordStatus mapStatus(TurkuInternalDryRunTriggerResponse r) {
        if (r.getStatus() == null) return TurkuInternalDryRunRecordStatus.FAILED_SAFE;
        return switch (r.getStatus()) {
            case ADMIN_METADATA_VISIBLE -> TurkuInternalDryRunRecordStatus.READY_FOR_ADMIN_HISTORY_ONLY;
            case REJECTED_UNSAFE -> TurkuInternalDryRunRecordStatus.REJECTED_UNSAFE;
            default -> TurkuInternalDryRunRecordStatus.METADATA_CAPTURED;
        };
    }

    private List<TurkuInternalDryRunRecordIssueCode> mapIssues(TurkuInternalDryRunTriggerResponse r) {
        if (r.getStatus() == TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE)
            return List.of(TurkuInternalDryRunRecordIssueCode.TRIGGER_REJECTED);
        if (r.getIssueCodes() != null && !r.getIssueCodes().isEmpty())
            return List.of(TurkuInternalDryRunRecordIssueCode.DRY_RUN_CHAIN_FAILED);
        return List.of();
    }
}
