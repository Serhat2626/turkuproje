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
        return TurkuStoryPackage.builder()
                .turkuName(project.getTurkuName())
                .regionContext(project.getRegion() != null ? project.getRegion() : "Anadolu")
                .originSummary("Sağlayıcı tarafından üretilen özet (inceleme gerekli)")
                .emotionalTheme(project.getMood() != null ? project.getMood() : "Geleneksel Anadolu duygusu")
                .culturalSignificance("Kültürel bağlam — insan incelemesi gerektirir")
                .narrativeAngle("Sağlayıcı perspektifi")
                .storySummary(result.getSafeOutputSummary())
                .sourceConfidence(result.getOutputConfidence())
                .needsHumanReview(true)
                .copyrightReviewRequired(true)
                .reviewFlags(result.getReviewFlags())
                .build();
    }

    private TurkuScenarioPlan buildStubScenarioPlan(TurkuVideoProject project,
                                                     TurkuAiGenerationResult result) {
        return TurkuScenarioPlan.builder()
                .title(project.getTurkuName() + " — Senaryo")
                .logline(result.getSafeOutputSummary())
                .narrationTone(project.getMood() != null ? project.getMood() : "Hüzünlü")
                .targetDurationHintSeconds(project.getTargetDurationMinutes() != null
                        ? project.getTargetDurationMinutes() * 60 : 480)
                .sceneCountHint(6)
                .scenarioBeats(List.of(
                        "Hook (inceleme gerekli)",
                        "Giriş (inceleme gerekli)",
                        "Ana hikâye (inceleme gerekli)",
                        "Duygusal zirve (inceleme gerekli)",
                        "Kapanış (inceleme gerekli)",
                        "CTA (inceleme gerekli)"))
                .reviewFlags(result.getReviewFlags())
                .build();
    }

    private TurkuStoryboardPlan buildStubStoryboardPlan(TurkuVideoProject project,
                                                         TurkuAiGenerationResult result) {
        List<TurkuStoryboardScene> scenes = List.of(
                new TurkuStoryboardScene(
                        1, "Sahne 1 (inceleme gerekli)",
                        "Sağlayıcı görsel 1",
                        "Sağlayıcı anlatım 1",
                        project.getMood() != null ? project.getMood() : "Hüzünlü",
                        30, "Geniş açı", "Fade in",
                        "Arka plan (telif incelemesi gerekli)")
        );

        return new TurkuStoryboardPlan(scenes, 30,
                result.getReviewFlags());
    }

    private void addMissing(List<String> target, List<String> source) {
        if (source == null) return;
        for (String s : source) {
            if (!target.contains(s)) target.add(s);
        }
    }
}
