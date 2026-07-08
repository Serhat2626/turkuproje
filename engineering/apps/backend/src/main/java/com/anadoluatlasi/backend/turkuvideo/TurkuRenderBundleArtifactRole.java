package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-036: Artifact role within a render bundle.
 * No user media or copyrighted content roles.
 */
public enum TurkuRenderBundleArtifactRole {
    PRIMARY_VIDEO_CLIP,
    POSTER_FRAME,
    AUDIO_STUB,
    VERIFICATION_REPORT,
    INTERNAL_METADATA,
    UNKNOWN;

    public boolean isAllowed() {
        return this != UNKNOWN;
    }
}
