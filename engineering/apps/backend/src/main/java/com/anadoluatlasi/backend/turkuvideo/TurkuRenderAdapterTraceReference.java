package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-023 Task 02: Opaque trace reference for render adapter execution.
 * No file path, no URL, no secrets.
 */
public final class TurkuRenderAdapterTraceReference {
    private final String traceRef;
    private final TurkuRenderAdapterMode adapterMode;
    private final long timestampMs;

    public TurkuRenderAdapterTraceReference(TurkuRenderAdapterMode adapterMode) {
        this.traceRef = adapterMode.name().toLowerCase() + "-trace:" + UUID.randomUUID();
        this.adapterMode = adapterMode;
        this.timestampMs = System.currentTimeMillis();
    }

    public String getTraceRef() { return traceRef; }
    public TurkuRenderAdapterMode getAdapterMode() { return adapterMode; }
    public long getTimestampMs() { return timestampMs; }
}
