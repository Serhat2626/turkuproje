package com.anadoluatlasi.backend.turkuvideo;

import java.util.regex.Pattern;

/**
 * RP-001: Policy for türkü name storyboard requests.
 * Rejects lyrics, multiline, URLs, media, oversized inputs.
 */
public final class TurkuNameStoryboardRequestPolicy {
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MIN_SCENE_COUNT = 1;
    private static final int MAX_SCENE_COUNT = 30;
    private static final int MIN_DURATION_MS = 5_000;
    private static final int MAX_DURATION_MS = 600_000;
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final int MIN_DIM = 120;
    private static final int MAX_DIM = 3840;

    private static final Pattern URL_PATTERN = Pattern.compile("https?://|ftp://|www\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTILINE_PATTERN = Pattern.compile("\\n.*\\n");

    public boolean isTitleEmpty(String title) {
        return title == null || title.isBlank();
    }

    public boolean isTitleTooLong(String title) {
        return title != null && title.length() > MAX_TITLE_LENGTH;
    }

    public boolean containsLyrics(String title) {
        if (title == null) return false;
        // Heuristic: 3+ newlines likely lyrics
        long lines = title.chars().filter(c -> c == '\n').count();
        return lines >= 3;
    }

    public boolean containsMultiline(String title) {
        return title != null && MULTILINE_PATTERN.matcher(title).find();
    }

    public boolean containsUrl(String title) {
        return title != null && URL_PATTERN.matcher(title).find();
    }

    public boolean isSceneCountValid(int count) {
        return count >= MIN_SCENE_COUNT && count <= MAX_SCENE_COUNT;
    }

    public boolean isDurationValid(int ms) {
        return ms >= MIN_DURATION_MS && ms <= MAX_DURATION_MS;
    }

    public boolean isFpsValid(int fps) {
        return fps >= MIN_FPS && fps <= MAX_FPS;
    }

    public boolean isResolutionValid(int w, int h) {
        return w >= MIN_DIM && w <= MAX_DIM && h >= MIN_DIM && h <= MAX_DIM;
    }
}
