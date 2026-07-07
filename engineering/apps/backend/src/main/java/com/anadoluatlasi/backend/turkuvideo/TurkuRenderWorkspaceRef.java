package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * FP-024 Task 03: Opaque workspace reference.
 * No filesystem paths. No real directory access.
 */
public final class TurkuRenderWorkspaceRef {
    private final String workspaceId;
    private final String opaqueRef;

    public TurkuRenderWorkspaceRef(String workspaceId, String opaqueRef) {
        this.workspaceId = workspaceId;
        this.opaqueRef = opaqueRef;
    }

    public TurkuRenderWorkspaceRef() {
        this(UUID.randomUUID().toString(), "internal-workspace:" + UUID.randomUUID());
    }

    public String getWorkspaceId() { return workspaceId; }
    public String getOpaqueRef() { return opaqueRef; }
}
