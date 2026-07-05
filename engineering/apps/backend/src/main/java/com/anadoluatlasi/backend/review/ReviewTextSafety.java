package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 02: Simple safety helper for review notes and rejection reasons.
 *
 * <p>Trims, normalizes, and validates review text to prevent unsafe content.</p>
 */
public final class ReviewTextSafety {

    /** Maximum allowed length for a review note or rejection reason. */
    public static final int MAX_NOTE_LENGTH = 2000;

    /** Maximum allowed length for a safe reason code. */
    public static final int MAX_REASON_CODE_LENGTH = 100;

    private ReviewTextSafety() {
        // Utility class
    }

    /**
     * Sanitize a review note/rejection reason.
     *
     * @param text raw text input
     * @return sanitized text, or null if input is null
     */
    public static String sanitize(String text) {
        if (text == null) return null;
        // Trim whitespace
        String trimmed = text.strip();
        // Remove control characters except newline/tab
        String cleaned = trimmed.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        // Enforce max length
        if (cleaned.length() > MAX_NOTE_LENGTH) {
            cleaned = cleaned.substring(0, MAX_NOTE_LENGTH);
        }
        return cleaned;
    }

    /**
     * Sanitize a safe reason code.
     *
     * @param code raw reason code
     * @return sanitized code, or null if input is null
     */
    public static String sanitizeReasonCode(String code) {
        if (code == null) return null;
        String trimmed = code.strip();
        // Reason codes should be simple — remove non-alphanumeric except underscore/hyphen
        String cleaned = trimmed.replaceAll("[^a-zA-Z0-9_\\-]", "");
        if (cleaned.length() > MAX_REASON_CODE_LENGTH) {
            cleaned = cleaned.substring(0, MAX_REASON_CODE_LENGTH);
        }
        return cleaned;
    }

    /**
     * Validate that a required reason is not blank.
     *
     * @param text the text to validate
     * @param fieldName the field name for error message
     * @throws ReviewTransitionViolationException if text is blank
     */
    public static void requireNonBlank(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new ReviewTransitionViolationException(
                    "Review " + fieldName + " must not be blank");
        }
    }
}
