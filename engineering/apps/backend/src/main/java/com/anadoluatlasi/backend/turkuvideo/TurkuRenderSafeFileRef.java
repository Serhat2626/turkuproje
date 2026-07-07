package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-024 Task 03: Opaque safe file reference.
 * No filesystem paths. No real file access.
 */
public final class TurkuRenderSafeFileRef {
    private final String fileId;
    private final String opaqueRef;
    private final String extension;
    private final String purpose;

    public TurkuRenderSafeFileRef(String fileId, String opaqueRef,
                                   String extension, String purpose) {
        this.fileId = fileId;
        this.opaqueRef = opaqueRef;
        this.extension = extension;
        this.purpose = purpose;
    }

    public TurkuRenderSafeFileRef(String extension, String purpose) {
        this(UUID.randomUUID().toString(),
                "internal-file:" + UUID.randomUUID(),
                extension, purpose);
    }

    public String getFileId() { return fileId; }
    public String getOpaqueRef() { return opaqueRef; }
    public String getExtension() { return extension; }
    public String getPurpose() { return purpose; }
}
