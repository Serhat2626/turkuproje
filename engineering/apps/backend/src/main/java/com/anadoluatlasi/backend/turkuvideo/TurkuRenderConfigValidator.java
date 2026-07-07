package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FP-024 Task 02: Render config validator.
 * Fail-closed by default. No filesystem access. No process execution.
 */
public class TurkuRenderConfigValidator {

    private static final Set<String> ALLOWED_FORMATS = Set.of("mp4", "webm");
    private static final Set<String> ALLOWED_CODECS = Set.of("h264", "vp9", "aac", "opus");

    private static final List<String> SHELL_METACHARACTERS = List.of(
            ";", "&&", "||", "$(", "`", "|", ">", "<", "&", "\n", "\r");

    private static final List<String> TRAVERSAL_PATTERNS = List.of(
            "..", "/../", "\\..\\", "%2e%2e", "%2E%2E");

    private static final List<String> URL_PATTERNS = List.of(
            "http://", "https://", "ftp://", "file://",
            "s3://", "gs://", "az://");

    private static final List<String> SECRET_PATTERNS = List.of(
            "password=", "secret=", "token=", "apiKey=",
            "Bearer ", "Authorization:", "jwt=");

    private static final List<String> STORAGE_PATTERNS = List.of(
            "storageKey", "bucketKey", "objectKey", "signedUrl",
            "publicUrl", "cdnUrl");

    private static final List<String> BASE64_PATTERNS = List.of(
            "base64", "data:image", "data:video", "data:audio");

    // Bounds
    private static final int MIN_TIMEOUT = 10;
    private static final int MAX_TIMEOUT = 300;
    private static final int MIN_DURATION = 1;
    private static final int MAX_DURATION = 600;
    private static final int MIN_SIZE_MB = 1;
    private static final int MAX_SIZE_MB = 1000;
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final int MAX_RESOLUTION_W = 3840;
    private static final int MAX_RESOLUTION_H = 2160;

    public TurkuRenderConfigValidationResult validate(TurkuRenderSecureConfig config) {
        if (config == null) {
            return TurkuRenderConfigValidationResult.missing();
        }

        if (!config.isEnabled()) {
            return TurkuRenderConfigValidationResult.disabled();
        }

        List<TurkuRenderConfigValidationIssue> issues = new ArrayList<>();

        // Validate executable token
        validateToken(config.getExecutableToken(), "executableToken", issues);

        // Validate workspace/temp tokens
        validateToken(config.getWorkspaceToken(), "workspaceToken", issues);
        validateToken(config.getTempToken(), "tempToken", issues);

        // Validate bounds
        validateBound(config.getMaxTimeoutSeconds(), MIN_TIMEOUT, MAX_TIMEOUT,
                "maxTimeoutSeconds", TurkuRenderConfigFailureCode.TIMEOUT_OUT_OF_RANGE, issues);
        validateBound(config.getMaxDurationSeconds(), MIN_DURATION, MAX_DURATION,
                "maxDurationSeconds", TurkuRenderConfigFailureCode.DURATION_OUT_OF_RANGE, issues);
        validateBound(config.getMaxOutputSizeMb(), MIN_SIZE_MB, MAX_SIZE_MB,
                "maxOutputSizeMb", TurkuRenderConfigFailureCode.SIZE_OUT_OF_RANGE, issues);
        validateBound(config.getMaxFps(), MIN_FPS, MAX_FPS,
                "maxFps", TurkuRenderConfigFailureCode.FPS_OUT_OF_RANGE, issues);

        // Validate resolution
        validateResolution(config.getMaxResolution(), issues);

        // Validate formats
        if (config.getAllowedFormats() != null) {
            for (String fmt : config.getAllowedFormats()) {
                if (!ALLOWED_FORMATS.contains(fmt)) {
                    issues.add(new TurkuRenderConfigValidationIssue(
                            TurkuRenderConfigFailureCode.FORMAT_NOT_ALLOWED,
                            "allowedFormats", "Format not in allowlist: " + fmt));
                }
            }
        }

        if (issues.isEmpty()) {
            return new TurkuRenderConfigValidationResult(true,
                    TurkuRenderConfigMode.FFMPEG_CONFIG_VALID, issues);
        } else {
            return new TurkuRenderConfigValidationResult(false,
                    TurkuRenderConfigMode.FFMPEG_CONFIG_INVALID, issues);
        }
    }

    public boolean containsShellMetacharacters(String value) {
        if (value == null) return false;
        for (String meta : SHELL_METACHARACTERS) {
            if (value.contains(meta)) return true;
        }
        return false;
    }

    public boolean containsPathTraversal(String value) {
        if (value == null) return false;
        for (String pattern : TRAVERSAL_PATTERNS) {
            if (value.contains(pattern)) return true;
        }
        return false;
    }

    public boolean containsUrl(String value) {
        if (value == null) return false;
        for (String pattern : URL_PATTERNS) {
            if (value.contains(pattern)) return true;
        }
        return false;
    }

    public boolean containsSecret(String value) {
        if (value == null) return false;
        for (String pattern : SECRET_PATTERNS) {
            if (value.contains(pattern)) return true;
        }
        return false;
    }

    public boolean containsStorageKey(String value) {
        if (value == null) return false;
        for (String pattern : STORAGE_PATTERNS) {
            if (value.contains(pattern)) return true;
        }
        return false;
    }

    public boolean containsBase64(String value) {
        if (value == null) return false;
        for (String pattern : BASE64_PATTERNS) {
            if (value.contains(pattern)) return true;
        }
        return false;
    }

    private void validateToken(String token, String field,
                                List<TurkuRenderConfigValidationIssue> issues) {
        if (token == null || token.isBlank()) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    field.contains("executable")
                            ? TurkuRenderConfigFailureCode.EXECUTABLE_MISSING
                            : TurkuRenderConfigFailureCode.WORKSPACE_MISSING,
                    field, "Missing: " + field));
            return;
        }
        if (containsShellMetacharacters(token)) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.SHELL_METACHARACTER_DETECTED,
                    field, "Shell metacharacter in: " + field));
        }
        if (containsPathTraversal(token)) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.PATH_TRAVERSAL_DETECTED,
                    field, "Path traversal in: " + field));
        }
        if (containsUrl(token)) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.URL_DETECTED,
                    field, "URL pattern in: " + field));
        }
        if (containsSecret(token)) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.SECRET_DETECTED,
                    field, "Secret pattern in: " + field));
        }
        if (containsStorageKey(token)) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.STORAGE_KEY_DETECTED,
                    field, "Storage key pattern in: " + field));
        }
        if (containsBase64(token)) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.GENERAL_VALIDATION_FAILURE,
                    field, "Base64/data pattern in: " + field));
        }
    }

    private void validateBound(int value, int min, int max, String field,
                                TurkuRenderConfigFailureCode code,
                                List<TurkuRenderConfigValidationIssue> issues) {
        if (value < min || value > max) {
            issues.add(new TurkuRenderConfigValidationIssue(code, field,
                    field + " out of range [" + min + "-" + max + "]: " + value));
        }
    }

    private void validateResolution(String resolution, List<TurkuRenderConfigValidationIssue> issues) {
        if (resolution == null || resolution.isBlank()) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.RESOLUTION_OUT_OF_RANGE,
                    "maxResolution", "Resolution missing"));
            return;
        }
        String[] parts = resolution.toLowerCase().split("x");
        if (parts.length != 2) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.RESOLUTION_OUT_OF_RANGE,
                    "maxResolution", "Invalid resolution format"));
            return;
        }
        try {
            int w = Integer.parseInt(parts[0]);
            int h = Integer.parseInt(parts[1]);
            if (w < 1 || w > MAX_RESOLUTION_W || h < 1 || h > MAX_RESOLUTION_H) {
                issues.add(new TurkuRenderConfigValidationIssue(
                        TurkuRenderConfigFailureCode.RESOLUTION_OUT_OF_RANGE,
                        "maxResolution", "Resolution exceeds max " + MAX_RESOLUTION_W + "x" + MAX_RESOLUTION_H));
            }
        } catch (NumberFormatException e) {
            issues.add(new TurkuRenderConfigValidationIssue(
                    TurkuRenderConfigFailureCode.RESOLUTION_OUT_OF_RANGE,
                    "maxResolution", "Non-numeric resolution"));
        }
    }
}
