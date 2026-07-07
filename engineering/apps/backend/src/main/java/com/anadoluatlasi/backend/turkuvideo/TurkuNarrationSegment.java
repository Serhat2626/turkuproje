package com.anadoluatlasi.backend.turkuvideo;

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

    public TurkuNarrationSegment(int segmentNo, int sceneNo, String narrationText,
                                  double durationSeconds, String voiceStyle,
                                  String language, boolean ttsReady) {
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
