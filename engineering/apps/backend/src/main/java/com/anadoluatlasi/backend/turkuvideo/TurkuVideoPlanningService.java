package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-019 Task 03: Planning service for story/scenario/storyboard orchestration.
 *
 * <p>Uses deterministic internal generation (stubs) for Task 03.
 * No external AI provider integration yet. Provider-ready seams can be
 * added in future tasks when a safe adapter is established.</p>
 *
 * <p>Enforces controlled stage transitions:
 * DRAFT_CREATED → STORY_CONTEXT_PREPARED → SCENARIO_PREPARED → STORYBOARD_PREPARED</p>
 *
 * <p>No lyrics, no copyrighted media, no public URLs, no raw provider data.</p>
 */
@Service
public class TurkuVideoPlanningService {

    private final TurkuVideoProjectRepository repository;

    public TurkuVideoPlanningService(TurkuVideoProjectRepository repository) {
        this.repository = repository;
    }

    // ── Story Preparation ───────────────────────────────────

    public TurkuStoryPackage prepareStory(UUID projectId, TurkuStoryPreparationRequest request) {
        TurkuVideoProject project = getAndValidateProject(projectId);
        validateStageTransition(project, TurkuVideoWorkflowStage.DRAFT_CREATED,
                TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED);

        if (request != null) {
            validateOptionalHint(request.getNarrativeAngleHint(), "narrativeAngleHint", 200);
            validateOptionalHint(request.getAdditionalContextHint(), "additionalContextHint", 500);
        }

        // Deterministic internal generation (stub)
        List<String> reviewFlags = buildStoryReviewFlags(project);

        TurkuStoryPackage storyPackage = TurkuStoryPackage.builder()
                .turkuName(project.getTurkuName())
                .regionContext(project.getRegion() != null ? project.getRegion() : "Anadolu")
                .originSummary(buildOriginSummary(project))
                .emotionalTheme(project.getMood() != null ? project.getMood() : "Geleneksel Anadolu duygusu")
                .culturalSignificance("Bu türkü, Anadolu halk geleneğinin önemli bir parçasıdır. " +
                        "Kültürel bağlamı ve anlamı detaylı inceleme gerektirir.")
                .narrativeAngle(request != null && request.getNarrativeAngleHint() != null
                        ? request.getNarrativeAngleHint()
                        : "Hikâye anlatıcısı perspektifi")
                .storySummary(buildStorySummary(project))
                .sourceConfidence(project.getSourceConfidence())
                .needsHumanReview(true) // always true for story packages
                .copyrightReviewRequired(true) // always true
                .reviewFlags(reviewFlags)
                .build();

        project.advanceTo(TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED);
        project.setStoryPackage(storyPackage);
        repository.save(project);

        return storyPackage;
    }

    // ── Scenario Preparation ────────────────────────────────

    public TurkuScenarioPlan prepareScenario(UUID projectId, TurkuScenarioPreparationRequest request) {
        TurkuVideoProject project = getAndValidateProject(projectId);
        validateStageTransition(project, TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED,
                TurkuVideoWorkflowStage.SCENARIO_PREPARED);

        if (project.getStoryPackage() == null) {
            throw TurkuVideoProjectException.missingPrerequisite("Hikâye paketi hazırlanmalıdır");
        }

        if (request != null) {
            validateOptionalHint(request.getNarrationToneHint(), "narrationToneHint", 200);
        }

        int sceneCount = (request != null && request.getSceneCountHint() != null)
                ? Math.max(3, Math.min(request.getSceneCountHint(), 12))
                : 6;

        String tone = (request != null && request.getNarrationToneHint() != null)
                ? request.getNarrationToneHint()
                : (project.getNarrationStyle() != null ? project.getNarrationStyle() : "Hikâyeci");

        int durationHint = project.getTargetDurationMinutes() != null
                ? project.getTargetDurationMinutes() * 60 : 240;

        List<String> beats = buildScenarioBeats(project, sceneCount);
        List<String> reviewFlags = new ArrayList<>();
        reviewFlags.add("LYRICS_NOT_INCLUDED");
        reviewFlags.add("COPYRIGHT_REVIEW_REQUIRED");
        if (project.getSourceConfidence() == TurkuSourceConfidence.UNKNOWN
                || project.getSourceConfidence() == TurkuSourceConfidence.LOW) {
            reviewFlags.add("SOURCE_REVIEW_REQUIRED");
        }

        TurkuScenarioPlan plan = TurkuScenarioPlan.builder()
                .title(project.getTurkuName() + " — Video Senaryosu")
                .logline("\"" + project.getTurkuName() + "\" türküsünün hikâyesini anlatan " +
                         "kısa belgesel format video senaryosu.")
                .narrationTone(tone)
                .targetDurationHintSeconds(durationHint)
                .sceneCountHint(sceneCount)
                .scenarioBeats(beats)
                .reviewFlags(reviewFlags)
                .build();

        project.advanceTo(TurkuVideoWorkflowStage.SCENARIO_PREPARED);
        project.setScenarioPlan(plan);
        repository.save(project);

        return plan;
    }

    // ── Storyboard Preparation ──────────────────────────────

    public TurkuStoryboardPlan prepareStoryboard(UUID projectId, TurkuStoryboardPreparationRequest request) {
        TurkuVideoProject project = getAndValidateProject(projectId);
        validateStageTransition(project, TurkuVideoWorkflowStage.SCENARIO_PREPARED,
                TurkuVideoWorkflowStage.STORYBOARD_PREPARED);

        if (project.getScenarioPlan() == null) {
            throw TurkuVideoProjectException.missingPrerequisite("Senaryo planı hazırlanmalıdır");
        }

        if (request != null) {
            validateOptionalHint(request.getVisualStyleHint(), "visualStyleHint", 200);
            validateOptionalHint(request.getCameraStyleHint(), "cameraStyleHint", 200);
        }

        String visualStyle = (request != null && request.getVisualStyleHint() != null)
                ? request.getVisualStyleHint()
                : (project.getVisualStyle() != null ? project.getVisualStyle() : "Sinematik Anadolu");

        String cameraStyle = (request != null && request.getCameraStyleHint() != null)
                ? request.getCameraStyleHint() : "Yavaş geçişli belgesel";

        TurkuScenarioPlan scenario = project.getScenarioPlan();
        int sceneCount = scenario.getSceneCountHint();
        int durationPerScene = scenario.getTargetDurationHintSeconds() != null
                ? scenario.getTargetDurationHintSeconds() / Math.max(1, sceneCount) : 40;

        List<TurkuStoryboardScene> scenes = buildStoryboardScenes(
                project, scenario, sceneCount, durationPerScene, visualStyle, cameraStyle);

        int totalDuration = scenes.stream().mapToInt(TurkuStoryboardScene::getEstimatedDurationSeconds).sum();

        List<String> reviewFlags = new ArrayList<>();
        reviewFlags.add("LYRICS_NOT_INCLUDED");
        reviewFlags.add("COPYRIGHT_REVIEW_REQUIRED");
        reviewFlags.add("CULTURAL_CONTEXT_REVIEW_REQUIRED");

        TurkuStoryboardPlan plan = new TurkuStoryboardPlan(scenes, totalDuration, reviewFlags);

        project.advanceTo(TurkuVideoWorkflowStage.STORYBOARD_PREPARED);
        project.setStoryboardPlan(plan);
        repository.save(project);

        return plan;
    }

    // ── Validation Helpers ──────────────────────────────────

    private TurkuVideoProject getAndValidateProject(UUID id) {
        return repository.findById(id)
                .orElseThrow(TurkuVideoProjectException::projectNotFound);
    }

    private void validateStageTransition(TurkuVideoProject project,
                                          TurkuVideoWorkflowStage required,
                                          TurkuVideoWorkflowStage target) {
        if (project.getWorkflowStage() != required) {
            throw TurkuVideoProjectException.invalidStageTransition(
                    project.getWorkflowStage(), target);
        }
    }

    private void validateOptionalHint(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) return;
        if (value.length() > maxLength) {
            throw TurkuVideoProjectException.fieldTooLong(fieldName, maxLength);
        }
        // unsafe content check
        if (value.matches("(?i).*(javascript:|<script|on\\w+=|password|secret|token|bearer|authorization|eyJ[A-Za-z0-9]).*")) {
            throw TurkuVideoProjectException.unsafeInput(fieldName);
        }
    }

    // ── Deterministic Generation (Stubs) ────────────────────

    private String buildOriginSummary(TurkuVideoProject project) {
        String region = project.getRegion() != null ? project.getRegion() : "Anadolu";
        String notes = project.getSourceNotes() != null ? " " + project.getSourceNotes() : "";
        return "\"" + project.getTurkuName() + "\" türküsü, " + region +
                " yöresine ait geleneksel bir halk türküsüdür." + notes +
                " Kesin tarihsel bilgi için uzman incelemesi önerilir.";
    }

    private String buildStorySummary(TurkuVideoProject project) {
        return "\"" + project.getTurkuName() + "\" türküsünün hikâyesi, Anadolu kültürel geleneği " +
                "içinde önemli bir yere sahiptir. Bu özet, ön araştırma niteliğindedir ve " +
                "yayın öncesi uzman incelemesi gerektirir. Şu anda sözleri dahil edilmemiştir.";
    }

    private List<String> buildStoryReviewFlags(TurkuVideoProject project) {
        List<String> flags = new ArrayList<>();
        flags.add("LYRICS_NOT_INCLUDED");
        flags.add("COPYRIGHT_REVIEW_REQUIRED");
        if (project.getSourceConfidence() == TurkuSourceConfidence.UNKNOWN
                || project.getSourceConfidence() == TurkuSourceConfidence.LOW) {
            flags.add("SOURCE_REVIEW_REQUIRED");
        }
        flags.add("CULTURAL_CONTEXT_REVIEW_REQUIRED");
        return flags;
    }

    private List<String> buildScenarioBeats(TurkuVideoProject project, int sceneCount) {
        String name = project.getTurkuName();
        List<String> beats = new ArrayList<>();
        beats.add("[Hook] \"" + name + "\" — Bu türkünün arkasında nasıl bir hikâye yatıyor?");
        beats.add("[Giriş] " + name + " türküsünün doğduğu coğrafya ve dönem.");
        beats.add("[Hikâye] Türkünün anlattığı olaylar ve karakterler.");
        beats.add("[Duygusal Zirve] Türkünün en derin duygusal mesajı.");
        beats.add("[Kapanış] Bu türkünün bugün bize ne anlattığı.");
        beats.add("[CTA] Yorumlarınızı paylaşın — bu türkünün sizin için anlamı nedir?");
        // sceneCount'a göre ek detaylar eklenebilir
        if (sceneCount > 6) {
            for (int i = 7; i <= sceneCount; i++) {
                beats.add("[Ek Sahne " + i + "] Kültürel/tarihi bağlam detayı.");
            }
        }
        return beats;
    }

    private List<TurkuStoryboardScene> buildStoryboardScenes(
            TurkuVideoProject project, TurkuScenarioPlan scenario,
            int sceneCount, int durationPerScene, String visualStyle, String cameraStyle) {
        List<String> beats = scenario.getScenarioBeats();
        List<TurkuStoryboardScene> scenes = new ArrayList<>();

        String[] moods = {"meraklı", "nostaljik", "hüzünlü", "derin", "huzurlu", "umutlu"};
        String[] cameras = {"Geniş açı manzara", "Yakın çekim detay", "Orta plan sahne",
                            "Yavaş pan hareket", "Sabit kare çekim", "Geçiş efekti"};
        String[] transitions = {"Yumuşak geçiş", "Çapraz geçiş", "Karartma", "Sert kesim",
                                "Yumuşak geçiş", "Son kare"};

        for (int i = 0; i < Math.min(sceneCount, beats.size()); i++) {
            scenes.add(new TurkuStoryboardScene(
                    i + 1,
                    "Sahne " + (i + 1),
                    visualStyle + " — " + beats.get(i).replaceAll("\\[.*?]\\s*", ""),
                    beats.get(i),
                    moods[i % moods.length],
                    durationPerScene,
                    cameras[i % cameras.length] + " (" + cameraStyle + ")",
                    transitions[i % transitions.length],
                    "Geleneksel Anadolu enstrümanları — telif kontrol gerekli"
            ));
        }
        return scenes;
    }
}
