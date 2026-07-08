package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-037: Expected artifact within a scene render plan.
 * No content/prompt/lyrics/script body. Metadata-only.
 */
public final class TurkuSceneRenderArtifactExpectation {
    private final TurkuRenderBundleArtifactRole role;
    private final boolean required;

    public TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole role, boolean required) {
        this.role = role != null ? role : TurkuRenderBundleArtifactRole.UNKNOWN;
        this.required = required;
    }

    public TurkuRenderBundleArtifactRole getRole() { return role; }
    public boolean isRequired() { return required; }
}
