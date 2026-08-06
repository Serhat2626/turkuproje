package com.anadoluatlasi.backend.turkuvideo;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        // Hikâye yeniden üretilebilir — DRAFT_CREATED veya sonrası kabul edilir
        validateStageAtLeast(project, TurkuVideoWorkflowStage.DRAFT_CREATED);

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

    // ── 3-Stage Story Pipeline ─────────────────────────────────

    /**
     * 3 aşamalı hikâye üretim pipeline'ı:
     * 1. RESEARCH — Kaynaklı araştırma
     * 2. STORY_DRAFT — Araştırmaya dayalı hikâye taslağı
     * 3. STORY_EDIT — Doğrulama editörü + nihai hikâye
     *
     * <p>Her aşama arasında DB'ye kaydedilir. Hata durumunda kaldığı yerden devam eder.</p>
     */
    public TurkuProviderPlanningGenerationResponse generateStoryPipeline(
            UUID projectId, TurkuProviderGenerationRequest request) {

        TurkuVideoProject project = getProject(projectId);

        // Başlangıç veya kaldığı yerden devam (resume desteği)
        TurkuVideoWorkflowStage stage = project.getWorkflowStage();
        // Pipeline yeniden çalıştırılabilir — DRAFT_CREATED veya sonrası kabul edilir
        validateStageAtLeast(project, TurkuVideoWorkflowStage.DRAFT_CREATED);

        TurkuProviderMode mode = safetyPolicy.resolveProviderMode(project);
        TurkuProviderPlanningGenerationResponse modeBlock = checkModeBlocking(
                projectId, mode, "STORY_PIPELINE", project.getWorkflowStage());
        if (modeBlock != null) return modeBlock;

        sanitizeRequest(request);
        var safeParams = buildProjectSafeParams(project, request);

        TurkuStoryPackage currentPackage = project.getStoryPackage();

        // ═══ AŞAMA 1: ARAŞTIRMA ═══
        if (stage == TurkuVideoWorkflowStage.DRAFT_CREATED) {
            log.info("📚 Aşama 1/3: Araştırma başlıyor — projectId={}", projectId);

            TurkuGenerationSafetyEnvelope env1 = TurkuGenerationSafetyEnvelope.createDefault(
                    projectId, "RESEARCH", mode);
            safetyPolicy.enforcePreGenerationPolicy(env1);

            TurkuAiGenerationRequest researchReq = TurkuAiGenerationRequest.builder()
                    .projectId(projectId)
                    .generationStage("RESEARCH")
                    .context(buildNarrativeContext(project))
                    .promptTemplateId("turku-research-v1")
                    .safeParams(safeParams)
                    .build();

            TurkuAiGenerationResult researchResult = gateway.generateAi(researchReq, env1);
            sanitizer.sanitizeEnvelope(env1);
            safetyPolicy.enforcePostGenerationPolicy(env1);

            String researchReport = researchResult.getSafeOutputSummary();
            log.info("📚 Araştırma tamamlandı — {} karakter", researchReport.length());

            // İlk package oluştur veya güncelle
            currentPackage = TurkuStoryPackage.builder()
                    .turkuName(project.getTurkuName())
                    .regionContext(project.getRegion() != null ? project.getRegion() : "Anadolu")
                    .emotionalTheme(project.getMood() != null ? project.getMood() : "Geleneksel Anadolu duygusu")
                    .researchReport(researchReport)
                    .sourceConfidence(researchResult.getOutputConfidence())
                    .needsHumanReview(true)
                    .copyrightReviewRequired(true)
                    .reviewFlags(researchResult.getReviewFlags())
                    .build();

            project.setStoryPackage(currentPackage);
            project.advanceTo(TurkuVideoWorkflowStage.RESEARCH_COMPLETED);
            repository.save(project);

            // Params'a araştırmayı ekle
            safeParams.put("researchReport", researchReport);
            stage = TurkuVideoWorkflowStage.RESEARCH_COMPLETED;
        } else if (currentPackage != null && currentPackage.getResearchReport() != null) {
            safeParams.put("researchReport", currentPackage.getResearchReport());
        }

        // ═══ AŞAMA 2: HİKÂYE TASLAĞI ═══
        if (stage == TurkuVideoWorkflowStage.RESEARCH_COMPLETED) {
            log.info("📝 Aşama 2/3: Hikâye taslağı başlıyor — projectId={}", projectId);

            TurkuGenerationSafetyEnvelope env2 = TurkuGenerationSafetyEnvelope.createDefault(
                    projectId, "STORY_DRAFT", mode);
            safetyPolicy.enforcePreGenerationPolicy(env2);

            TurkuAiGenerationRequest draftReq = TurkuAiGenerationRequest.builder()
                    .projectId(projectId)
                    .generationStage("STORY_DRAFT")
                    .context(buildNarrativeContext(project))
                    .promptTemplateId("turku-story-draft-v1")
                    .safeParams(safeParams)
                    .build();

            TurkuAiGenerationResult draftResult = gateway.generateAi(draftReq, env2);
            sanitizer.sanitizeEnvelope(env2);
            safetyPolicy.enforcePostGenerationPolicy(env2);

            String storyDraft = draftResult.getSafeOutputSummary();
            log.info("📝 Hikâye taslağı tamamlandı — {} karakter", storyDraft.length());

            // Package güncelle
            currentPackage = TurkuStoryPackage.builder()
                    .turkuName(currentPackage.getTurkuName())
                    .regionContext(currentPackage.getRegionContext())
                    .emotionalTheme(currentPackage.getEmotionalTheme())
                    .researchReport(currentPackage.getResearchReport())
                    .storyDraft(storyDraft)
                    .sourceConfidence(draftResult.getOutputConfidence())
                    .needsHumanReview(true)
                    .copyrightReviewRequired(true)
                    .reviewFlags(draftResult.getReviewFlags())
                    .build();

            project.setStoryPackage(currentPackage);
            project.advanceTo(TurkuVideoWorkflowStage.STORY_DRAFT_COMPLETED);
            repository.save(project);

            safeParams.put("storyDraft", storyDraft);
            stage = TurkuVideoWorkflowStage.STORY_DRAFT_COMPLETED;
        } else if (currentPackage != null && currentPackage.getStoryDraft() != null) {
            safeParams.put("storyDraft", currentPackage.getStoryDraft());
        }

        // ═══ AŞAMA 3: DOĞRULAMA & EDİTÖRLÜK ═══
        log.info("✅ Aşama 3/3: Editör kontrolü başlıyor — projectId={}", projectId);

        TurkuGenerationSafetyEnvelope env3 = TurkuGenerationSafetyEnvelope.createDefault(
                projectId, "STORY_EDIT", mode);
        safetyPolicy.enforcePreGenerationPolicy(env3);

        TurkuAiGenerationRequest editReq = TurkuAiGenerationRequest.builder()
                .projectId(projectId)
                .generationStage("STORY_EDIT")
                .context(buildNarrativeContext(project))
                .promptTemplateId("turku-story-edit-v1")
                .safeParams(safeParams)
                .build();

        TurkuAiGenerationResult editResult = gateway.generateAi(editReq, env3);
        sanitizer.sanitizeEnvelope(env3);
        safetyPolicy.enforcePostGenerationPolicy(env3);

        String editOutput = editResult.getSafeOutputSummary();
        log.info("✅ Editör kontrolü tamamlandı — {} karakter", editOutput.length());

        // Editör çıktısından qualityStatus ve finalStory ayır
        String qualityStatus = extractQualityStatus(editOutput);
        String finalStory = extractFinalStory(editOutput);
        String characterSheet = extractCharacterSheet(finalStory);

        // Nihai package
        currentPackage = TurkuStoryPackage.builder()
                .turkuName(currentPackage.getTurkuName())
                .regionContext(currentPackage.getRegionContext())
                .emotionalTheme(currentPackage.getEmotionalTheme())
                .culturalSignificance("Kaynaklı araştırma + 3 aşamalı üretim")
                .narrativeAngle("Sağlayıcı perspektifi — editörden geçmiş")
                .originSummary(finalStory)
                .storySummary(finalStory)
                .characterSheet(characterSheet)
                .researchReport(currentPackage.getResearchReport())
                .storyDraft(currentPackage.getStoryDraft())
                .finalStory(finalStory)
                .qualityStatus(qualityStatus)
                .sourceConfidence(editResult.getOutputConfidence())
                .needsHumanReview(true)
                .copyrightReviewRequired(true)
                .reviewFlags(editResult.getReviewFlags())
                .build();

        project.setStoryPackage(currentPackage);
        project.advanceTo(TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED);
        repository.save(project);

        log.info("🎉 3 aşamalı hikâye pipeline tamamlandı — projectId={}", projectId);

        return buildResponse(projectId, project.getWorkflowStage(), mode,
                "STORY_PIPELINE", editResult, env3);
    }

    /** Editör çıktısından "## Editör Kontrolü" bölümünü çıkar */
    private String extractQualityStatus(String editOutput) {
        if (editOutput == null) return "BİLİNMİYOR";
        int idx = editOutput.indexOf("## Editör Kontrolü");
        if (idx < 0) idx = editOutput.indexOf("## Editor Kontrol");
        if (idx < 0) return "KONTROL KAYDI BULUNAMADI";

        // Editör kontrolü ile hikâye başlangıcı arasındaki metni al
        int end = editOutput.indexOf("\n# ", idx + 10);
        if (end < 0) end = Math.min(editOutput.length(), idx + 500);
        return editOutput.substring(idx, end).trim();
    }

    /** Editör çıktısından nihai hikâye metnini çıkar (editör notundan sonra) */
    private String extractFinalStory(String editOutput) {
        if (editOutput == null) return "";
        // "## Editör Kontrolü" bölümünden sonraki "# " başlığını bul
        int editIdx = editOutput.indexOf("## Editör Kontrolü");
        if (editIdx < 0) editIdx = editOutput.indexOf("## Editor Kontrol");
        if (editIdx < 0) return editOutput; // Editör notu yoksa tümünü döndür

        int storyStart = editOutput.indexOf("\n# ", editIdx + 10);
        if (storyStart < 0) return editOutput.substring(editIdx);
        return editOutput.substring(storyStart).trim();
    }

    // ── Generate Scenario ───────────────────────────────────

    public TurkuProviderPlanningGenerationResponse generateScenario(
            UUID projectId, TurkuProviderGenerationRequest request) {

        TurkuVideoProject project = getProject(projectId);
        // Senaryo yeniden üretilebilir — STORY_CONTEXT_PREPARED veya sonrası kabul edilir
        validateStageAtLeast(project, TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED);

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
        // Storyboard yeniden üretilebilir — SCENARIO_PREPARED veya sonraki aşamalar kabul edilir
        if (project.getWorkflowStage().ordinal() < TurkuVideoWorkflowStage.SCENARIO_PREPARED.ordinal()) {
            throw TurkuVideoProjectException.invalidStageTransition(
                    project.getWorkflowStage(), TurkuVideoWorkflowStage.SCENARIO_PREPARED);
        }

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
        // Planning bundle yeniden üretilebilir — DRAFT_CREATED veya sonrası kabul edilir
        validateStageAtLeast(project, TurkuVideoWorkflowStage.DRAFT_CREATED);

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
     *
     * <p>KRİTİK: Her stage'de önceki stage çıktıları da eklenir!
     * STORY → hikâye yok (ilk adım)
     * SCENARIO → storyPackage.storySummary eklenir (GPT hikâyeyi bilmeli)
     * STORYBOARD → storyPackage.storySummary + scenarioPlan eklenir
     * PLANNING_BUNDLE → tek çağrı, hepsi system prompt'ta</p>
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

        // ═══ ÖNCEKİ STAGE ÇIKTILARI — ZİNCİRLEME BAĞLAM ═══
        // Hikâye metni (SCENARIO ve STORYBOARD adımlarında kullanılır)
        if (project.getStoryPackage() != null && project.getStoryPackage().getStorySummary() != null) {
            String story = project.getStoryPackage().getStorySummary();
            // Token limiti aşmamak için max 6000 karakter (GPT-4o'ya uygun)
            if (story.length() > 6000) story = story.substring(0, 6000) + "\n\n[... hikâye devamı kırpıldı ...]";
            params.put("previousStoryOutput", story);

            // Karakter çizelgesi varsa ekle
            if (project.getStoryPackage().getCharacterSheet() != null) {
                String charSheet = project.getStoryPackage().getCharacterSheet();
                if (charSheet.length() > 2000) charSheet = charSheet.substring(0, 2000);
                params.put("characterSheet", charSheet);
            }
        }

        // Senaryo metni (STORYBOARD adımında kullanılır)
        if (project.getScenarioPlan() != null && project.getScenarioPlan().getLogline() != null) {
            String scenario = project.getScenarioPlan().getLogline();
            if (scenario.length() > 4000) scenario = scenario.substring(0, 4000) + "\n\n[... senaryo devamı kırpıldı ...]";
            params.put("previousScenarioOutput", scenario);
        }

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

    /** Aşama en az 'minimumStage' seviyesinde olmalı — yeniden üretim için geri dönüş de dahil */
    private void validateStageAtLeast(TurkuVideoProject project, TurkuVideoWorkflowStage minimumStage) {
        if (project.getWorkflowStage().ordinal() < minimumStage.ordinal()) {
            throw TurkuVideoProjectException.invalidStageTransition(
                    project.getWorkflowStage(), minimumStage);
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

        // Karakter çizelgesini GPT çıktısından parse et
        String characterSheet = extractCharacterSheet(fullText);

        return TurkuStoryPackage.builder()
                .turkuName(project.getTurkuName())
                .regionContext(project.getRegion() != null ? project.getRegion() : "Anadolu")
                .originSummary(fullText) // Gerçek GPT-4o çıktısı
                .emotionalTheme(project.getMood() != null ? project.getMood() : "Geleneksel Anadolu duygusu")
                .culturalSignificance("Kültürel bağlam — insan incelemesi gerektirir")
                .narrativeAngle("Sağlayıcı perspektifi")
                .storySummary(fullText) // Tam hikâye metni
                .characterSheet(characterSheet) // GPT-üretimi karakter tanımları
                .sourceConfidence(result.getOutputConfidence())
                .needsHumanReview(true)
                .copyrightReviewRequired(true)
                .reviewFlags(result.getReviewFlags())
                .build();
    }

    /**
     * GPT çıktısından "KARAKTER ÇİZELGESİ" bölümünü çıkarır.
     * Bu bölüm BÖLÜM 1.5 ile BÖLÜM 2 arasında yer alır.
     */
    private String extractCharacterSheet(String fullText) {
        if (fullText == null || fullText.isBlank()) return null;

        // "KARAKTER ÇİZELGESİ" veya "Karakterler" bölümünü bul
        int start = -1;
        String[] markers = {"KARAKTER ÇİZELGESİ", "## Karakterler", "### Ana Erkek Karakter"};
        for (String marker : markers) {
            int idx = fullText.indexOf(marker);
            if (idx >= 0) {
                start = idx;
                break;
            }
        }
        if (start < 0) return null;

        // Sonraki ana bölümde kes (BÖLÜM 2 veya SENARYO)
        int end = fullText.length();
        String[] endMarkers = {"# BÖLÜM 2", "BÖLÜM 2:", "# SENARYO", "═══════════════════════════════════════════\n                # BÖLÜM 2"};
        for (String endMarker : endMarkers) {
            int idx = fullText.indexOf(endMarker, start + 10);
            if (idx >= 0 && idx < end) {
                end = idx;
            }
        }
        // Daha genel: sonraki "═══" satırında kes
        int nextSep = fullText.indexOf("═══", start + 10);
        if (nextSep >= 0 && nextSep < end) {
            end = nextSep;
        }

        String sheet = fullText.substring(start, end).trim();
        return sheet.length() > 50 ? sheet : null; // Çok kısa ise geçersiz
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

        for (int i = 1; i < sections.length && i <= 40; i++) {
            String section = sections[i];

            String title = extractField(section, "^[:\\s]*([^\\n]+)");

            // Çok satırlı alanlar — Görsel ve Seslendirme birden fazla satıra yayılabilir
            String narration = extractMultiLineField(section, "Seslendirme");
            if (narration == null) {
                narration = extractMultiLineField(section, "Narration");
            }
            String visual = extractMultiLineField(section, "Görsel");
            if (visual == null) {
                visual = extractMultiLineField(section, "Visual");
            }

            // Tek satırlık alanlar
            String music = extractField(section, "\\*{0,2}Müzik:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            String mood = extractField(section, "\\*{0,2}(?:Mood|Duygu):?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            String camera = extractField(section, "\\*{0,2}Kamera:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            String transition = extractField(section, "\\*{0,2}Geçiş:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            String duration = extractField(section, "\\*{0,2}Süre:?\\*{0,2}\\s*(.+?)(?:\\n|$)");
            String characters = extractField(section, "\\*{0,2}Karakterler:?\\*{0,2}\\s*(.+?)(?:\\n|$)");

            // Süreyi saniyeye çevir (ör: "0:00 - 0:30" → 30 veya "15 saniye" → 15)
            int durationSec = parseDurationToSeconds(duration, 17); // 35 sahne × 17sn ≈ 600sn

            // Narration: tırnakları temizle
            if (narration != null) {
                narration = narration.replaceAll("^\"|\"$", "").trim();
            }

            // Visual: boşsa narration'dan fallback
            if ((visual == null || visual.isBlank()) && narration != null) {
                visual = "Sahne anlatımı: " + narration;
            }

            log.debug("Sahne {} parse: visualLen={}, narrationLen={}", i,
                    visual != null ? visual.length() : 0,
                    narration != null ? narration.length() : 0);

            scenes.add(new TurkuStoryboardScene(
                    i,
                    title != null ? title.replaceAll("^[:\\s]+", "").trim() : "Sahne " + i,
                    visual != null ? visual : "Genel Anadolu manzarası",
                    narration != null ? narration : "Anlatım metni",
                    mood != null ? mood : (project.getMood() != null ? project.getMood() : "Hüzünlü"),
                    durationSec,
                    camera != null ? camera : "Orta plan",
                    transition != null ? transition : (i == 1 ? "Fade in" : "Çapraz geçiş"),
                    music != null ? music : "Bağlama ve keman",
                    characters != null ? characters : ""
            ));
        }

        if (scenes.isEmpty()) {
            // Parse başarısız — tüm metni tek sahne olarak kaydet
            scenes.add(new TurkuStoryboardScene(
                    1, "Tam İçerik",
                    "Belgesel görünümü",
                    text.length() > 500 ? text.substring(0, 500) : text,
                    project.getMood() != null ? project.getMood() : "Hüzünlü",
                    60, "Geniş açı", "Fade in", "Arka plan müziği", null));
        }

        return scenes;
    }

    private String extractField(String section, String regex) {
        var matcher = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.MULTILINE)
                .matcher(section);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * Çok satırlı alan çıkarma — GPT çıktısında "**Görsel:**" gibi alanlar
     * birden fazla satıra yayılabilir. Bu metod alan başından sonraki "**" alanına
     * kadar olan TÜM satırları yakalar.
     *
     * Örnek GPT çıktısı:
     * **Görsel:** Genç adam tek başına karda yürüyor, sırtında sazı,
     * karşıda sisli dağlar, akşam güneşi alçak.
     * **Mood:** Hüzünlü
     *
     * → "Genç adam tek başına karda yürüyor, sırtında sazı, karşıda sisli dağlar, akşam güneşi alçak."
     */
    private String extractMultiLineField(String section, String fieldName) {
        // Field başlangıcını bul: **Görsel:** veya **Görsel** veya Görsel:
        String pattern = "\\*{0,2}" + fieldName + ":?\\*{0,2}\\s*";
        var matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.MULTILINE)
                .matcher(section);

        if (!matcher.find()) return null;

        int start = matcher.end();

        // Sonraki bold field'ı bul: **Mood:** veya **Kamera:** vs.
        var nextFieldMatcher = java.util.regex.Pattern.compile(
                "(?m)^\\s*\\*{2}\\w", java.util.regex.Pattern.MULTILINE
        ).matcher(section);

        int end = section.length();
        // start'tan sonraki ilk field başlangıcını bul
        nextFieldMatcher.region(start, section.length());
        if (nextFieldMatcher.find()) {
            end = nextFieldMatcher.start();
        }

        String result = section.substring(start, end).trim();

        // Boş satırları ve fazla boşlukları temizle, ama içeriği koru
        result = result.replaceAll("\\n\\s*\\n", " ")  // çift satır → boşluk
                       .replaceAll("\\n", " ")           // tek satır → boşluk
                       .replaceAll("\\s{2,}", " ")       // çoklu boşluk → tek
                       .replaceAll("^[\\[\\(]|[\\]\\)]$", "") // köşeli/yuvarlak parantez temizle
                       .trim();

        return result.isBlank() ? null : result;
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

            // "15 saniye" veya "15sn" veya "15 seconds" formatı
            var secMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*(?:saniye|sn|seconds?|sec)")
                    .matcher(durationStr);
            if (secMatcher.find()) {
                return Math.max(Integer.parseInt(secMatcher.group(1)), 5);
            }

            // Düz sayı: "17"
            var numMatcher = java.util.regex.Pattern.compile("^\\s*(\\d+)\\s*$")
                    .matcher(durationStr);
            if (numMatcher.find()) {
                return Math.max(Integer.parseInt(numMatcher.group(1)), 5);
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
