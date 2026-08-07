package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FP-021 Task 02: Narration segment — original summary text, NOT lyrics.
 * No copyrighted content. No TTS audio data.
 */
public final class TurkuNarrationSegment {

    private final int segmentNo;
    private final int sceneNo;
    private final String narrationText;
    private final double durationSeconds;
    private final String voiceStyle;
    private final String language;
    private final boolean ttsReady;

    @JsonCreator
    public TurkuNarrationSegment(
            @JsonProperty("segmentNo") int segmentNo,
            @JsonProperty("sceneNo") int sceneNo,
            @JsonProperty("narrationText") String narrationText,
            @JsonProperty("durationSeconds") double durationSeconds,
            @JsonProperty("voiceStyle") String voiceStyle,
            @JsonProperty("language") String language,
            @JsonProperty("ttsReady") boolean ttsReady) {
        this.segmentNo = segmentNo;
        this.sceneNo = sceneNo;
        this.narrationText = narrationText;
        this.durationSeconds = durationSeconds;
        this.voiceStyle = voiceStyle;
        this.language = language;
        this.ttsReady = ttsReady;
    }

    public int getSegmentNo() { return segmentNo; }
    public int getSceneNo() { return sceneNo; }
    public String getNarrationText() { return narrationText; }
    public double getDurationSeconds() { return durationSeconds; }
    public String getVoiceStyle() { return voiceStyle; }
    public String getLanguage() { return language; }
    public boolean isTtsReady() { return ttsReady; }
}
