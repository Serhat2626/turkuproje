package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.Set;

/**
 * FP-024 Task 03: Workspace policy.
 * Defines allowed extensions, bounded limits, cleanup rules.
 */
public final class TurkuRenderWorkspacePolicy {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "webm", "png", "jpg", "jpeg");
    public static final int MAX_OUTPUT_SIZE_MB = 1000;
    public static final int MAX_FILES_PER_JOB = 50;

    private final Set<String> allowedExtensions;
    private final int maxOutputSizeMb;
    private final int maxFilesPerJob;
    private final boolean autoCleanupEnabled;

    public TurkuRenderWorkspacePolicy(Set<String> allowedExtensions, int maxOutputSizeMb,
                                       int maxFilesPerJob, boolean autoCleanupEnabled) {
        this.allowedExtensions = allowedExtensions != null ? Set.copyOf(allowedExtensions) : ALLOWED_EXTENSIONS;
        this.maxOutputSizeMb = Math.max(1, Math.min(maxOutputSizeMb, MAX_OUTPUT_SIZE_MB));
        this.maxFilesPerJob = Math.max(1, Math.min(maxFilesPerJob, MAX_FILES_PER_JOB));
        this.autoCleanupEnabled = autoCleanupEnabled;
    }

    public TurkuRenderWorkspacePolicy() {
        this(ALLOWED_EXTENSIONS, 500, 20, false);
    }

    public Set<String> getAllowedExtensions() { return allowedExtensions; }
    public int getMaxOutputSizeMb() { return maxOutputSizeMb; }
    public int getMaxFilesPerJob() { return maxFilesPerJob; }
    public boolean isAutoCleanupEnabled() { return autoCleanupEnabled; }

    public boolean isExtensionAllowed(String ext) {
        if (ext == null) return false;
        return allowedExtensions.contains(ext.toLowerCase().replaceFirst("^\\.", ""));
    }
}
