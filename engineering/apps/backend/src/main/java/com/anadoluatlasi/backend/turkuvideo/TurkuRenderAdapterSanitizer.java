package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 02: Render adapter sanitizer.
 * Sanitizes error messages, trace refs, and adapter output.
 */
public final class TurkuRenderAdapterSanitizer {

    private static final int MAX_MESSAGE_LENGTH = 200;

    public String sanitizeMessage(String rawMessage) {
        if (rawMessage == null) return "Render adapter error";
        String cleaned = rawMessage
                .replaceAll("(?i)(password|secret|token|apikey|authorization|bearer)\\S*", "[REDACTED]")
                .replaceAll("[A-Za-z]:\\\\[^\\s]+", "[PATH_REDACTED]")
                .replaceAll("/[a-zA-Z0-9/_.-]{5,}", "[PATH_REDACTED]")
                .replaceAll("https?://\\S+", "[URL_REDACTED]");
        if (cleaned.length() > MAX_MESSAGE_LENGTH) {
            cleaned = cleaned.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return cleaned;
    }

    public String sanitizeTraceRef(String traceRef) {
        if (traceRef == null) return "unknown-trace";
        // Trace refs should already be safe (prefix:uuid), but double-check
        if (traceRef.contains("/") || traceRef.contains("\\") || traceRef.contains("http")) {
            return "sanitized-trace:" + traceRef.hashCode();
        }
        return traceRef;
    }
}
