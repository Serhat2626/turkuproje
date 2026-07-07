package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-027 Task 02: Opaque queue item reference.
 * Format: internal-render-queue-item:{uuid}
 */
public final class TurkuInternalRenderQueueItemRef {

    private static final String PREFIX = "internal-render-queue-item:";
    private final UUID itemId;

    private TurkuInternalRenderQueueItemRef(UUID itemId) {
        this.itemId = itemId;
    }

    public static TurkuInternalRenderQueueItemRef create() {
        return new TurkuInternalRenderQueueItemRef(UUID.randomUUID());
    }

    public static TurkuInternalRenderQueueItemRef of(UUID itemId) {
        if (itemId == null) throw new IllegalArgumentException("Queue item ID cannot be null");
        return new TurkuInternalRenderQueueItemRef(itemId);
    }

    public static TurkuInternalRenderQueueItemRef parse(String ref) {
        if (ref == null || ref.isBlank()) return null;
        if (!ref.startsWith(PREFIX)) return null;
        try {
            return new TurkuInternalRenderQueueItemRef(UUID.fromString(ref.substring(PREFIX.length())));
        } catch (IllegalArgumentException e) { return null; }
    }

    public static boolean isValid(String ref) { return parse(ref) != null; }
    public UUID getItemId() { return itemId; }
    public String toOpaqueRef() { return PREFIX + itemId; }
    @Override public String toString() { return toOpaqueRef(); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return itemId.equals(((TurkuInternalRenderQueueItemRef) o).itemId);
    }
    @Override public int hashCode() { return itemId.hashCode(); }
}
