package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-047: Safe storyboard fixture input for the full render chain.
 * Metadata-only. No lyrics/script/prompt/user media.
 */
public final class TurkuStoryboardInternalRenderChainFixture {
    private final String opaqueStoryboardRef;
    private final String opaqueStoryRef;
    private final List<TurkuStoryboardSceneRenderInput> sceneInputs;

    public TurkuStoryboardInternalRenderChainFixture(String opaqueStoryboardRef,
            String opaqueStoryRef, List<TurkuStoryboardSceneRenderInput> sceneInputs) {
        this.opaqueStoryboardRef = opaqueStoryboardRef;
        this.opaqueStoryRef = opaqueStoryRef;
        this.sceneInputs = sceneInputs != null ? List.copyOf(sceneInputs) : List.of();
    }

    public String getOpaqueStoryboardRef() { return opaqueStoryboardRef; }
    public String getOpaqueStoryRef() { return opaqueStoryRef; }
    public List<TurkuStoryboardSceneRenderInput> getSceneInputs() { return sceneInputs; }
    public int getSceneCount() { return sceneInputs.size(); }

    /** Create a safe Türkü-themed test fixture. */
    public static TurkuStoryboardInternalRenderChainFixture turkuFixture(int sceneCount) {
        var inputs = new java.util.ArrayList<TurkuStoryboardSceneRenderInput>();
        for (int i = 1; i <= sceneCount; i++) {
            inputs.add(new TurkuStoryboardSceneRenderInput(
                    "scene-turku-" + i, i, 2, 24, 640, 360, "mp4"));
        }
        return new TurkuStoryboardInternalRenderChainFixture(
                "sb-turku-fixture", "story-turku-fixture", inputs);
    }
}
