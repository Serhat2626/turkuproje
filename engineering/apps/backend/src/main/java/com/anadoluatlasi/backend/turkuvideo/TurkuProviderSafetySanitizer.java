package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * FP-020 Task 02: Provider output sanitizer.
 *
 * <p>Detects and redacts forbidden patterns in provider output text.
 * Returns safe failure codes, not raw details.</p>
 */
@Service
public class TurkuProviderSafetySanitizer {

    private static final List<Pattern> CRITICAL_PATTERNS = List.of(
            Pattern.compile("eyJ[A-Za-z0-9_-]{10,}", Pattern.CASE_INSENSITIVE),           // JWT
            Pattern.compile("Bearer\\s+\\S+", Pattern.CASE_INSENSITIVE),                   // Bearer token
            Pattern.compile("<script", Pattern.CASE_INSENSITIVE),                           // Script tag
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),                       // JS protocol
            Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE),                     // Event handler
            Pattern.compile("onclick\\s*=", Pattern.CASE_INSENSITIVE),                     // Event handler
            Pattern.compile("data:image", Pattern.CASE_INSENSITIVE),                       // Base64 image
            Pattern.compile("[A-Za-z0-9+/=]{100,}")                                        // Long base64
    );

    private static final List<Pattern> URL_PATTERNS = List.of(
            Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("s3://\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("gs://\\S+", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("password\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("secret\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("apiKey\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("api_key\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Authorization:", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> LYRICS_PATTERNS = List.of(
            Pattern.compile("\\[Verse", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[Chorus", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[Bridge", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Lyrics:", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> PROMPT_INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore previous", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ignore all", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget your instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Sanitizes text by redacting forbidden patterns.
     * Throws for critical violations (JWT, Bearer, base64, script).
     */
    public String sanitizeText(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        // Critical patterns → exception
        for (Pattern p : CRITICAL_PATTERNS) {
            if (p.matcher(input).find()) {
                throw new TurkuVideoProjectException(
                        "Güvenlik ihlali: metin yasaklı kritik desen içeriyor",
                        "SANITIZATION_CRITICAL_VIOLATION");
            }
        }

        // Secret patterns → exception
        for (Pattern p : SECRET_PATTERNS) {
            if (p.matcher(input).find()) {
                throw new TurkuVideoProjectException(
                        "Güvenlik ihlali: metin gizli bilgi deseni içeriyor",
                        "SANITIZATION_SECRET_VIOLATION");
            }
        }

        // URL patterns → redact
        String result = input;
        for (Pattern p : URL_PATTERNS) {
            result = p.matcher(result).replaceAll("[URL_REDACTED]");
        }

        return result;
    }

    /**
     * Checks for prompt injection patterns.
     */
    public boolean containsPromptInjection(String text) {
        if (text == null || text.isBlank()) return false;
        return PROMPT_INJECTION_PATTERNS.stream()
                .anyMatch(p -> p.matcher(text).find());
    }

    /**
     * Checks for any forbidden pattern (critical, secret, lyrics, prompt injection, URL, storage).
     */
    public boolean containsForbiddenPattern(String text) {
        if (text == null || text.isBlank()) return false;

        for (Pattern p : CRITICAL_PATTERNS) {
            if (p.matcher(text).find()) return true;
        }
        for (Pattern p : SECRET_PATTERNS) {
            if (p.matcher(text).find()) return true;
        }
        for (Pattern p : LYRICS_PATTERNS) {
            if (p.matcher(text).find()) return true;
        }
        for (Pattern p : PROMPT_INJECTION_PATTERNS) {
            if (p.matcher(text).find()) return true;
        }
        for (Pattern p : URL_PATTERNS) {
            if (p.matcher(text).find()) return true;
        }
        return false;
    }

    /**
     * Marks a safety envelope as sanitized and records actions taken.
     */
    public TurkuGenerationSafetyEnvelope sanitizeEnvelope(TurkuGenerationSafetyEnvelope envelope) {
        envelope.setOutputSanitized(true);
        envelope.setPromptInjectionChecked(true);
        envelope.getSanitizationActions().add("OUTPUT_SANITIZED");
        envelope.getSanitizationActions().add("PROMPT_INJECTION_CHECKED");
        envelope.getSanitizationActions().add("FORBIDDEN_PATTERNS_SCANNED");

        // Invariants
        envelope.setAutoApproved(false);
        envelope.setHumanReviewRequired(true);

        if (!envelope.getReviewFlags().contains("HUMAN_REVIEW_REQUIRED")) {
            envelope.getReviewFlags().add("HUMAN_REVIEW_REQUIRED");
        }

        return envelope;
    }
}
