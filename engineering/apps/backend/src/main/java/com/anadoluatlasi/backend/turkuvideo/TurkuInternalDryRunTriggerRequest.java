package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-049: Safe admin request for internal story-to-video dry-run trigger.
 * Metadata-only. No prompt/script/lyrics/user media/path/command.
 */
public final class TurkuInternalDryRunTriggerRequest {
    private String opaqueStoryboardRef;
    private String opaqueStoryRef;
    private List<TurkuStoryboardSceneRenderInput> sceneInputs;

    public TurkuInternalDryRunTriggerRequest() {}

    public TurkuInternalDryRunTriggerRequest(String opaqueStoryboardRef,
            String opaqueStoryRef, List<TurkuStoryboardSceneRenderInput> sceneInputs) {
        this.opaqueStoryboardRef = opaqueStoryboardRef;
        this.opaqueStoryRef = opaqueStoryRef;
        this.sceneInputs = sceneInputs;
    }

    public String getOpaqueStoryboardRef() { return opaqueStoryboardRef; }
    public void setOpaqueStoryboardRef(String v) { this.opaqueStoryboardRef = v; }
    public String getOpaqueStoryRef() { return opaqueStoryRef; }
    public void setOpaqueStoryRef(String v) { this.opaqueStoryRef = v; }
    public List<TurkuStoryboardSceneRenderInput> getSceneInputs() { return sceneInputs; }
    public void setSceneInputs(List<TurkuStoryboardSceneRenderInput> v) { this.sceneInputs = v; }
}
