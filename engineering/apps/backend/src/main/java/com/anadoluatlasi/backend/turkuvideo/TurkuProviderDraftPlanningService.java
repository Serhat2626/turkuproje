package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FP-020 Task 03: Provider-draft planning service.
 *
 * <p>Orchestrates provider-backed generation for story/scenario/storyboard.
 * Every call wraps with safety envelope, enforces policy, sanitizes output.
 * Uses TurkuGenerationProviderGateway (stub or real).</p>
 *
 * <p>Does NOT replace FP-019 prepare-* paths. generate-* is additive.</p>
 */
@Service
public class TurkuProviderDraftPlanningService {

    private final TurkuVideoProjectRepository repository;
    private final TurkuGenerationProviderGateway gateway;
    private final TurkuProviderSafetySanitizer sanitizer;
    private final TurkuProviderSafetyPolicy safetyPolicy;

    public TurkuProviderDraftPlanningService(
            TurkuVideoProjectRepository repository,
            TurkuGenerationProviderGateway gateway,
            TurkuProviderSafetySanitizer sanitizer,
            TurkuProviderSafetyPolicy safetyPolicy) {
        this.repository = repository;
        this.gateway = gateway;
        this.sanitizer = sanitizer;
        this.safetyPolicy = safetyPolicy;
    }

    public TurkuGenerationProviderGateway getGateway() {
        return gateway;
    }

    // ── Generate Story ──────────────────────────────────────

    public TurkuProviderPlanningGenerationResponse generateStory(
            UUID projectId, TurkuProviderGenerationRequest request) {

        TurkuVideoProject project = getProject(projectId);
        validateStage(project, TurkuVideoWorkflowStage.DRAFT_CREATED);

        TurkuProviderMode mode = safetyPolicy.resolveProviderMode(project);

        // Check for non-generatable modes
        TurkuProviderPlanningGenerationResponse modeBlock = checkModeBlocking(
                projectId, mode, "STORY", project.getWorkflowStage());
        if (modeBlock != null) return modeBlock;

        // Create safety envelope
        TurkuGenerationSafetyEnvelope envelope = TurkuGenerationSafetyEnvelope.createDefault(
                projectId, "STORY", mode);
        safetyPolicy.enforcePreGenerationPolicy(envelope);

        // Sanitize input
        sanitizeRequest(request);

        // Build gateway request
        TurkuAiGenerationRequest aiRequest = TurkuAiGenerationRequest.builder()
                .projectId(projectId)
                .generationStage("STORY")
                .context(buildNarrativeContext(project))
                .promptTemplateId("turku-story-v1")
                .safeParams(buildProjectSafeParams(project, request))
                .build();

        // Generate through gateway
        TurkuAiGenerationResult result = gateway.generateAi(aiRequest, envelope);

        // Sanitize and validate output
        sanitizer.sanitizeEnvelope(envelope);
        safetyPolicy.enforcePostGenerationPolicy(envelope);

        // Update project state
        project.advanceTo(TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED);
        project.setStoryPackage(buildStubStoryPackage(project, result));
        repository.save(project);

        return buildResponse(projectId, project.getWorkflowStage(), mode,
                "STORY", result, envelope);
    }

    // ── Generate Scenario ───────────────────────────────────

    public TurkuProviderPlanningGenerationResponse generateScenario(
            UUID projectId, TurkuProviderGenerationRequest request) {

        TurkuVideoProject project = getProject(projectId);
        validateStage(project, TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED);

        if (project.getStoryPackage() == null) {
            throw TurkuVideoProjectException.missingPrerequisite("Hikâye paketi hazırlanmalıdır");
        }

        TurkuProviderMode mode = safetyPolicy.resolveProviderMode(project);

        TurkuProviderPlanningGenerationResponse modeBlock = checkModeBlocking(
                projectId, mode, "SCENARIO", project.getWorkflowStage());
        if (modeBlock != null) return modeBlock;

        TurkuGenerationSafetyEnvelope envelope = TurkuGenerationSafetyEnvelope.createDefault(
                projectId, "SCENARIO", mode);
        safetyPolicy.enforcePreGenerationPolicy(envelope);

        sanitizeRequest(request);

        TurkuAiGenerationRequest aiRequest = TurkuAiGenerationRequest.builder()
                .projectId(projectId)
                .generationStage("SCENARIO")
                .context(buildNarrativeContext(project))
                .promptTemplateId("turku-scenario-v1")
                .safeParams(buildProjectSafeParams(project, request))
                .build();

        TurkuAiGenerationResult result = gateway.generateAi(aiRequest, envelope);

        sanitizer.sanitizeEnvelope(envelope);
        safetyPolicy.enforcePostGenerationPolicy(envelope);

        project.advanceTo(TurkuVideoWorkflowStage.SCENARIO_PREPARED);
        project.setScenarioPlan(buildStubScenarioPlan(project, result));
        repository.save(project);

        return buildResponse(projectId, project.getWorkflowStage(), mode,
                "SCENARIO", result, envelope);
    }

    // ── Generate Storyboard ─────────────────────────────────

    public TurkuProviderPlanningGenerationResponse generateStoryboard(
            UUID projectId, TurkuProviderGenerationRequest request) {

        TurkuVideoProject project = getProject(projectId);
        validateStage(project, TurkuVideoWorkflowStage.SCENARIO_PREPARED);

        if (project.getScenarioPlan() == null) {
            throw TurkuVideoProjectException.missingPrerequisite("Senaryo planı hazırlanmalıdır");
        }

        TurkuProviderMode mode = safetyPolicy.resolveProviderMode(project);

        TurkuProviderPlanningGenerationResponse modeBlock = checkModeBlocking(
                projectId, mode, "STORYBOARD", project.getWorkflowStage());
        if (modeBlock != null) return modeBlock;

        TurkuGenerationSafetyEnvelope envelope = TurkuGenerationSafetyEnvelope.createDefault(
                projectId, "STORYBOARD", mode);
        safetyPolicy.enforcePreGenerationPolicy(envelope);

        sanitizeRequest(request);

        TurkuAiGenerationRequest aiRequest = TurkuAiGenerationRequest.builder()
                .projectId(projectId)
                .generationStage("STORYBOARD")
                .context(buildNarrativeContext(project))
                .promptTemplateId("turku-storyboard-v1")
                .safeParams(buildProjectSafeParams(project, request))
                .build();

        TurkuAiGenerationResult result = gateway.generateAi(aiRequest, envelope);

        sanitizer.sanitizeEnvelope(envelope);
        safetyPolicy.enforcePostGenerationPolicy(envelope);

        project.advanceTo(TurkuVideoWorkflowStage.STORYBOARD_PREPARED);
        project.setStoryboardPlan(buildStubStoryboardPlan(project, result));
        repository.save(project);

        return buildResponse(projectId, project.getWorkflowStage(), mode,
                "STORYBOARD", result, envelope);
    }

    // ── Generate Planning Bundle ────────────────────────────

    public TurkuPlanningBundleGenerationResponse generatePlanningBundle(
            UUID projectId, TurkuProviderGenerationRequest request) {

        TurkuVideoProject project = getProject(projectId);
        validateStage(project, TurkuVideoWorkflowStage.DRAFT_CREATED);

        TurkuProviderMode mode = safetyPolicy.resolveProviderMode(project);

        // Check mode blocking for the full bundle
        if (mode == TurkuProviderMode.PROVIDER_DISABLED
                || mode == TurkuProviderMode.PROVIDER_FAILED_SAFE
                || mode == TurkuProviderMode.HUMAN_REVIEW_REQUIRED) {
            return TurkuPlanningBundleGenerationResponse.builder()
                    .projectId(projectId)
                    .currentStage(project.getWorkflowStage())
                    .providerMode(mode)
                    .safeOutputSummary("Sağlayıcı modu üretim için uygun değil: " + mode)
                    .safeFailureCode(mode == TurkuProviderMode.PROVIDER_DISABLED
                            ? "PROVIDER_DISABLED" : mode == TurkuProviderMode.PROVIDER_FAILED_SAFE
                            ? "PROVIDER_FAILED_SAFE" : "HUMAN_REVIEW_PENDING")
                    .humanReviewRequired(true)
                    .autoApproved(false)
                    .copyrightReviewRequired(true)
                    .storyGenerated(false)
                    .scenarioGenerated(false)
                    .storyboardGenerated(false)
                    .reviewFlags(List.of("HUMAN_REVIEW_REQUIRED", "COPYRIGHT_REVIEW_REQUIRED"))
                    .safeWarnings(List.of("Üretim engellendi: " + mode))
                    .build();
        }

        // Execute bundle in sequence: story → scenario → storyboard
        TurkuProviderPlanningGenerationResponse storyResult = generateStory(projectId, request);
        TurkuProviderPlanningGenerationResponse scenarioResult = generateScenario(projectId, request);
        TurkuProviderPlanningGenerationResponse storyboardResult = generateStoryboard(projectId, request);

        List<String> allFlags = new ArrayList<>();
        allFlags.addAll(storyResult.getReviewFlags());
        addMissing(allFlags, scenarioResult.getReviewFlags());
        addMissing(allFlags, storyboardResult.getReviewFlags());

        return TurkuPlanningBundleGenerationResponse.builder()
                .projectId(projectId)
                .currentStage(storyboardResult.getCurrentStage())
                .providerMode(mode)
                .safeOutputSummary("Planlama paketi tamamlandı: hikâye + senaryo + storyboard")
                .traceRef(storyboardResult.getTraceRef())
                .outputConfidence(TurkuSourceConfidence.LOW)
                .humanReviewRequired(true)
                .autoApproved(false)
                .copyrightReviewRequired(true)
                .storyGenerated(true)
                .scenarioGenerated(true)
                .storyboardGenerated(true)
                .reviewFlags(allFlags)
                .safeWarnings(storyResult.getSafeWarnings())
                .build();
    }

    // ── Private Helpers ─────────────────────────────────────

    private TurkuVideoProject getProject(UUID id) {
        return repository.findById(id)
                .orElseThrow(TurkuVideoProjectException::projectNotFound);
    }

    /**
     * Proje meta bilgilerini safeParams'a ekler — OpenAI gateway promptlarda kullanır.
     */
    private Map<String, String> buildProjectSafeParams(
            TurkuVideoProject project, TurkuProviderGenerationRequest request) {
        var params = new java.util.HashMap<String, String>();
        // Proje bilgileri
        if (project.getTurkuName() != null) params.put("turkuName", project.getTurkuName());
        if (project.getRegion() != null) params.put("region", project.getRegion());
        if (project.getMood() != null) params.put("mood", project.getMood());
        if (project.getSourceNotes() != null) params.put("sourceNotes", project.getSourceNotes());
        if (project.getNarrationStyle() != null) params.put("narrationStyle", project.getNarrationStyle());
        if (project.getVisualStyle() != null) params.put("visualStyle", project.getVisualStyle());
        if (project.getTargetDurationMinutes() != null)
            params.put("targetDurationMinutes", String.valueOf(project.getTargetDurationMinutes()));
        // Request'ten gelen ek parametreler (override)
        if (request != null && request.getSafeParams() != null) {
            params.putAll(request.getSafeParams());
        }
        return params;
    }

    private void validateStage(TurkuVideoProject project, TurkuVideoWorkflowStage expected) {
        if (project.getWorkflowStage() != expected) {
            throw TurkuVideoProjectException.invalidStageTransition(
                    project.getWorkflowStage(), expected);
        }
    }

    private TurkuProviderPlanningGenerationResponse checkModeBlocking(
            UUID projectId, TurkuProviderMode mode, String stage,
            TurkuVideoWorkflowStage currentStage) {

        if (mode == TurkuProviderMode.PROVIDER_DISABLED) {
            return buildBlockedResponse(projectId, currentStage, mode, stage,
                    "Sağlayıcı devre dışı bırakıldı", "PROVIDER_DISABLED");
        }
        if (mode == TurkuProviderMode.PROVIDER_FAILED_SAFE) {
            return buildBlockedResponse(projectId, currentStage, mode, stage,
                    "Sağlayıcı güvenli başarısızlık modunda", "PROVIDER_FAILED_SAFE");
        }
        if (mode == TurkuProviderMode.HUMAN_REVIEW_REQUIRED) {
            return buildBlockedResponse(projectId, currentStage, mode, stage,
                    "İnsan incelemesi bekleniyor", "HUMAN_REVIEW_PENDING");
        }
        return null;
    }

    private TurkuProviderPlanningGenerationResponse buildBlockedResponse(
            UUID projectId, TurkuVideoWorkflowStage stage, TurkuProviderMode mode,
            String genStage, String summary, String failureCode) {

        return TurkuProviderPlanningGenerationResponse.builder()
                .projectId(projectId)
                .currentStage(stage)
                .providerMode(mode)
                .generationStage(genStage)
                .safeOutputSummary(summary)
                .safeFailureCode(failureCode)
                .humanReviewRequired(true)
                .autoApproved(false)
                .copyrightReviewRequired(true)
                .reviewFlags(List.of("HUMAN_REVIEW_REQUIRED", "COPYRIGHT_REVIEW_REQUIRED"))
                .safeWarnings(List.of(summary))
                .build();
    }

    private void sanitizeRequest(TurkuProviderGenerationRequest request) {
        if (request == null) return;
        if (request.getNarrativeAngleHint() != null) {
            if (sanitizer.containsPromptInjection(request.getNarrativeAngleHint())) {
                throw new TurkuVideoProjectException(
                        "Prompt injection tespit edildi", "PROMPT_INJECTION_DETECTED");
            }
            if (sanitizer.containsForbiddenPattern(request.getNarrativeAngleHint())) {
                throw new TurkuVideoProjectException(
                        "Güvenlik ihlali: yasaklı desen tespit edildi", "UNSAFE_INPUT");
            }
        }
        if (request.getAdditionalContextHint() != null) {
            if (sanitizer.containsPromptInjection(request.getAdditionalContextHint())) {
                throw new TurkuVideoProjectException(
                        "Prompt injection tespit edildi", "PROMPT_INJECTION_DETECTED");
            }
            if (sanitizer.containsForbiddenPattern(request.getAdditionalContextHint())) {
                throw new TurkuVideoProjectException(
                        "Güvenlik ihlali: yasaklı desen tespit edildi", "UNSAFE_INPUT");
            }
        }
    }

    private TurkuNarrativeContext buildNarrativeContext(TurkuVideoProject project) {
        return new TurkuNarrativeContext(
                project.getMood() != null ? project.getMood() : "Geleneksel",
                "Duygusal yolculuk",
                project.getNarrationStyle() != null ? project.getNarrationStyle() : "Klasik anlatı",
                "Genel izleyici",
                project.getVisualStyle() != null ? project.getVisualStyle() : "Sinematik"
        );
    }

    private TurkuProviderPlanningGenerationResponse buildResponse(
            UUID projectId, TurkuVideoWorkflowStage stage, TurkuProviderMode mode,
            String genStage, TurkuAiGenerationResult result,
            TurkuGenerationSafetyEnvelope envelope) {

        return TurkuProviderPlanningGenerationResponse.builder()
                .projectId(projectId)
                .currentStage(stage)
                .providerMode(mode)
                .generationStage(genStage)
                .safeOutputSummary(result.getSafeOutputSummary())
                .traceRef(result.getTraceRef())
                .outputConfidence(result.getOutputConfidence())
                .humanReviewRequired(true)
                .autoApproved(false)
                .copyrightReviewRequired(true)
                .reviewFlags(result.getReviewFlags())
                .safeWarnings(result.getSafeWarnings())
                .build();
    }

    private TurkuStoryPackage buildStubStoryPackage(TurkuVideoProject project,
                                                     TurkuAiGenerationResult result) {
        String fullText = result.getSafeOutputSummary();
        return TurkuStoryPackage.builder()
                .turkuName(project.getTurkuName())
                .regionContext(project.getRegion() != null ? project.getRegion() : "Anadolu")
                .originSummary(fullText) // Gerçek GPT-4o çıktısı
                .emotionalTheme(project.getMood() != null ? project.getMood() : "Geleneksel Anadolu duygusu")
                .culturalSignificance("Kültürel bağlam — insan incelemesi gerektirir")
                .narrativeAngle("Sağlayıcı perspektifi")
                .storySummary(fullText) // Tam hikâye metni
                .sourceConfidence(result.getOutputConfidence())
                .needsHumanReview(true)
                .copyrightReviewRequired(true)
                .reviewFlags(result.getReviewFlags())
                .build();
    }

    private TurkuScenarioPlan buildStubScenarioPlan(TurkuVideoProject project,
                                                     TurkuAiGenerationResult result) {
        String fullText = result.getSafeOutputSummary();

        // Beat'leri GPT-4o çıktısından parse et
        List<String> beats = parseBeatsFromText(fullText);

        return TurkuScenarioPlan.builder()
                .title(project.getTurkuName() + " — Senaryo")
                .logline(fullText)
                .narrationTone(project.getMood() != null ? project.getMood() : "Hüzünlü")
                .targetDurationHintSeconds(project.getTargetDurationMinutes() != null
                        ? project.getTargetDurationMinutes() * 60 : 480)
                .sceneCountHint(beats.size())
                .scenarioBeats(beats)
                .reviewFlags(result.getReviewFlags())
                .build();
    }

    private TurkuStoryboardPlan buildStubStoryboardPlan(TurkuVideoProject project,
                                                         TurkuAiGenerationResult result) {
        String fullText = result.getSafeOutputSummary();

        // GPT-4o çıktısını sahnelere parse et
        List<TurkuStoryboardScene> scenes = parseScenesFromText(fullText, project);

        int totalDuration = scenes.stream()
                .mapToInt(TurkuStoryboardScene::getEstimatedDurationSeconds)
                .sum();

        return new TurkuStoryboardPlan(scenes, totalDuration, result.getReviewFlags());
    }

    // ── GPT-4o çıktısı parse yardımcıları ──

    /**
     * GPT-4o çıktısından beat'leri çıkarır.
     * "### Beat N" veya "## Beat N" formatında bölümleri arar.
     */
    private List<String> parseBeatsFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of("Veri yok");
        }
        List<String> beats = new java.util.ArrayList<>();
        String[] sections = text.split("(?m)^#{2,3}\\s+Beat\\s+\\d+");
        for (int i = 1; i < sections.length; i++) { // i=0 öncesi boş
            String section = sections[i].trim();
            // İlk satırı beat başlığı olarak al
            String firstLine = section.split("\\n")[0].trim();
            if (firstLine.startsWith(":")) firstLine = firstLine.substring(1).trim();
            beats.add(!firstLine.isBlank() ? firstLine : "Beat " + i);
        }
        return beats.isEmpty() ? List.of("Tek bölüm") : beats;
    }

    /**
     * GPT-4o storyboard çıktısını sahne nesnelerine dönüştürür.
     * "### Beat N" veya "### Sahne N" bölümlerini arar ve
     * **Seslendirme:**, **Görsel:**, **Müzik:**, **Duygu:** gibi alanları parse eder.
     */
    private List<TurkuStoryboardScene> parseScenesFromText(String text, TurkuVideoProject project) {
        if (text == null || text.isBlank()) {
            return List.of(new TurkuStoryboardScene(
                    1, "Sahne 1", "Genel görünüm", "Anlatım metni yok",
                    project.getMood() != null ? project.getMood() : "Hüzünlü",
                    30, "Geniş açı", "Fade in", "Arka plan müziği"));
        }

        List<TurkuStoryboardScene> scenes = new java.util.ArrayList<>();

        // "### Beat N" veya "### Sahne N" ile böl
        String[] sections = text.split("(?m)^#{2,3}\\s+(?:Beat|Sahne)\\s+\\d+");
        if (sections.length <= 1) {
            // Alternatif: satır başındaki "Beat N:" veya numaralı bölümler
            sections = text.split("(?m)^\\*{0,2}\\s*Beat\\s+\\d+");
        }

        for (int i = 1; i < sections.length && i <= 35; i++) {
            String section = sections[i];

            String title = extractField(section, "^[:\\s]*([^\\n]+)");
            String narration = extractField(section, "\\*{0,2}Seslendirme:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            if (narration == null) {
                narration = extractField(section, "\\*{0,2}Narration:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            }
            String visual = extractField(section, "\\*{0,2}Görsel:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            String music = extractField(section, "\\*{0,2}Müzik:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            String mood = extractField(section, "\\*{0,2}Duygu:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            String duration = extractField(section, "\\*{0,2}Süre:?\\*{0,2}\\s*(.+?)(?:\\n|$)");

            // Süreyi saniyeye çevir (ör: "0:00 - 0:30" → 30)
            int durationSec = parseDurationToSeconds(duration, 30);

            // Narration: tırnakları temizle
            if (narration != null) {
                narration = narration.replaceAll("^\"|\"$", "").trim();
            }

            scenes.add(new TurkuStoryboardScene(
                    i,
                    title != null ? title.replaceAll("^[:\\s]+", "").trim() : "Sahne " + i,
                    visual != null ? visual : "Genel Anadolu manzarası",
                    narration != null ? narration : "Anlatım metni",
                    mood != null ? mood : (project.getMood() != null ? project.getMood() : "Hüzünlü"),
                    durationSec,
                    "Orta plan",
                    i == 1 ? "Fade in" : "Çapraz geçiş",
                    music != null ? music : "Bağlama ve keman"
            ));
        }

        if (scenes.isEmpty()) {
            // Parse başarısız — tüm metni tek sahne olarak kaydet
            scenes.add(new TurkuStoryboardScene(
                    1, "Tam İçerik",
                    "Belgesel görünümü",
                    text.length() > 500 ? text.substring(0, 500) : text,
                    project.getMood() != null ? project.getMood() : "Hüzünlü",
                    60, "Geniş açı", "Fade in", "Arka plan müziği"));
        }

        return scenes;
    }

    private String extractField(String section, String regex) {
        var matcher = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.MULTILINE)
                .matcher(section);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private int parseDurationToSeconds(String durationStr, int defaultSec) {
        if (durationStr == null || durationStr.isBlank()) return defaultSec;
        try {
            // "0:30 - 2:00" formatı → süreyi hesapla
            var matcher = java.util.regex.Pattern.compile("(\\d+):(\\d+)\\s*[-–]\\s*(\\d+):(\\d+)")
                    .matcher(durationStr);
            if (matcher.find()) {
                int startSec = Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
                int endSec = Integer.parseInt(matcher.group(3)) * 60 + Integer.parseInt(matcher.group(4));
                return Math.max(endSec - startSec, 10);
            }
        } catch (Exception e) {
            // ignore parse errors
        }
        return defaultSec;
    }

    private void addMissing(List<String> target, List<String> source) {
        if (source == null) return;
        for (String s : source) {
            if (!target.contains(s)) target.add(s);
        }
    }
}
