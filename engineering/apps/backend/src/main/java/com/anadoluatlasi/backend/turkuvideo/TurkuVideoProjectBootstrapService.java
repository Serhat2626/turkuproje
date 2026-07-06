package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * FP-019 Task 02: Bootstrap service for Türkü video projects.
 *
 * <p>Validates input, sanitizes display values, creates draft projects
 * with safe defaults (needsHumanReview=true, copyrightReviewRequired=true,
 * sourceConfidence=UNKNOWN, stage=DRAFT_CREATED).</p>
 *
 * <p>No AI generation. No publish/export. No public delivery.</p>
 */
@Service
public class TurkuVideoProjectBootstrapService {

    // Güvenli olmayan HTML / script / credential kalıpları
    private static final Pattern UNSAFE_HTML = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_SCRIPT = Pattern.compile(
            "(javascript:|on\\w+=|<script|</script)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDENTIAL_PATTERN = Pattern.compile(
            "(password|secret|token|bearer|authorization|apikey|eyJ[A-Za-z0-9])",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_TURKU_NAME = 200;
    private static final int MAX_REGION = 100;
    private static final int MAX_SOURCE_NOTES = 500;
    private static final int MAX_MOOD = 200;
    private static final int MAX_NARRATION_STYLE = 100;
    private static final int MAX_VISUAL_STYLE = 100;
    private static final int MIN_DURATION = 1;
    private static final int MAX_DURATION = 30;

    private final TurkuVideoProjectRepository repository;

    public TurkuVideoProjectBootstrapService(TurkuVideoProjectRepository repository) {
        this.repository = repository;
    }

    /**
     * Bootstrap a new Türkü video project from the given request.
     *
     * @param request bootstrap input
     * @param actorReference safe, sanitized actor reference from resolver
     * @return created project
     */
    public TurkuVideoProject bootstrap(TurkuVideoBootstrapRequest request, String actorReference) {
        // ── Validate required field ──
        if (request.getTurkuName() == null || request.getTurkuName().isBlank()) {
            throw TurkuVideoProjectException.turkuNameRequired();
        }

        String turkuName = sanitize(request.getTurkuName());
        validateLength(turkuName, "turkuName", MAX_TURKU_NAME);

        // ── Validate optional fields ──
        String region = sanitizeOptional(request.getRegion(), "region", MAX_REGION);
        String sourceNotes = sanitizeOptional(request.getSourceNotes(), "sourceNotes", MAX_SOURCE_NOTES);
        String mood = sanitizeOptional(request.getMood(), "mood", MAX_MOOD);
        String narrationStyle = sanitizeOptional(request.getNarrationStyle(), "narrationStyle", MAX_NARRATION_STYLE);
        String visualStyle = sanitizeOptional(request.getVisualStyle(), "visualStyle", MAX_VISUAL_STYLE);

        // ── Validate duration ──
        Integer duration = request.getTargetDurationMinutes();
        if (duration != null && (duration < MIN_DURATION || duration > MAX_DURATION)) {
            throw TurkuVideoProjectException.invalidDuration();
        }

        // ── Determine initial source confidence ──
        TurkuSourceConfidence confidence = TurkuSourceConfidence.UNKNOWN;
        if (sourceNotes != null && !sourceNotes.isBlank()) {
            confidence = TurkuSourceConfidence.LOW; // kaynak notu var ama doğrulanmadı
        }

        // ── Build project with safe defaults ──
        TurkuVideoProject project = TurkuVideoProject.builder()
                .turkuName(turkuName)
                .region(region)
                .sourceNotes(sourceNotes)
                .mood(mood)
                .targetDurationMinutes(duration)
                .narrationStyle(narrationStyle)
                .visualStyle(visualStyle)
                .workflowStage(TurkuVideoWorkflowStage.DRAFT_CREATED)
                .sourceConfidence(confidence)
                .needsHumanReview(true)
                .copyrightReviewRequired(true)
                .createdByActorReference(actorReference)
                .build();

        return repository.save(project);
    }

    public TurkuVideoProject getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(TurkuVideoProjectException::projectNotFound);
    }

    // ── Sanitization / Validation ──

    private String sanitize(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        if (UNSAFE_SCRIPT.matcher(trimmed).find()) {
            throw TurkuVideoProjectException.unsafeInput("script/event handler");
        }
        if (CREDENTIAL_PATTERN.matcher(trimmed).find()) {
            throw TurkuVideoProjectException.unsafeInput("credential pattern");
        }
        // HTML etiketlerini kaldır
        return UNSAFE_HTML.matcher(trimmed).replaceAll("");
    }

    private String sanitizeOptional(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String sanitized = sanitize(value);
        validateLength(sanitized, fieldName, maxLength);
        return sanitized;
    }

    private void validateLength(String value, String fieldName, int max) {
        if (value != null && value.length() > max) {
            if ("turkuName".equals(fieldName)) {
                throw TurkuVideoProjectException.turkuNameTooLong();
            }
            throw TurkuVideoProjectException.fieldTooLong(fieldName, max);
        }
    }
}
