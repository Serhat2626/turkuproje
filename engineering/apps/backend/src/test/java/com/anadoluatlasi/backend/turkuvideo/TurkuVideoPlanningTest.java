package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FP-019 Task 03: Story/Scenario/Storyboard Orchestration Tests.
 */
@DisplayName("FP-019 Task 03 — Story/Scenario/Storyboard Orchestration")
class TurkuVideoPlanningTest {

    private TurkuVideoProjectRepository repository;
    private TurkuVideoProjectBootstrapService bootstrapService;
    private TurkuVideoPlanningService planningService;

    @BeforeEach
    void setUp() {
        repository = new TurkuVideoProjectRepository();
        bootstrapService = new TurkuVideoProjectBootstrapService(repository);
        planningService = new TurkuVideoPlanningService(repository);
    }

    // ── Helper ──

    private TurkuVideoProject createDraftProject() {
        var req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("Mihriban");
        req.setRegion("Erzurum");
        req.setSourceNotes("Aşık Sümmani'ye atfedilir");
        req.setMood("hüzünlü, özlem");
        req.setTargetDurationMinutes(4);
        req.setNarrationStyle("hikâyeci");
        req.setVisualStyle("suluboya");
        return bootstrapService.bootstrap(req, "admin-test");
    }

    // ── A. Story Preparation ────────────────────────────────

    @Nested
    @DisplayName("A. Story Preparation")
    class StoryPreparation {

        @Test
        @DisplayName("prepare story from DRAFT_CREATED succeeds")
        void storyFromDraft() {
            var project = createDraftProject();
            var story = planningService.prepareStory(project.getId(), null);

            assertThat(story).isNotNull();
            assertThat(story.getTurkuName()).isEqualTo("Mihriban");
            assertThat(story.getRegionContext()).isNotBlank();
            assertThat(story.getOriginSummary()).isNotBlank();
            assertThat(story.getStorySummary()).isNotBlank();
            assertThat(story.getCulturalSignificance()).isNotBlank();
        }

        @Test
        @DisplayName("project advances to STORY_CONTEXT_PREPARED")
        void stageAdvanced() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED);
        }

        @Test
        @DisplayName("story package stored on project")
        void storyStored() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getStoryPackage()).isNotNull();
            assertThat(updated.getStoryPackage().getTurkuName()).isEqualTo("Mihriban");
        }

        @Test
        @DisplayName("story with narrative angle hint")
        void withHint() {
            var project = createDraftProject();
            var req = new TurkuStoryPreparationRequest();
            req.setNarrativeAngleHint("Göç perspektifi");
            var story = planningService.prepareStory(project.getId(), req);
            assertThat(story.getNarrativeAngle()).isEqualTo("Göç perspektifi");
        }

        @Test
        @DisplayName("needsHumanReview=true in story package")
        void humanReview() {
            var project = createDraftProject();
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.isNeedsHumanReview()).isTrue();
        }

        @Test
        @DisplayName("copyrightReviewRequired=true in story package")
        void copyrightReview() {
            var project = createDraftProject();
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("story package includes LYRICS_NOT_INCLUDED flag")
        void lyricsNotIncluded() {
            var project = createDraftProject();
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.getReviewFlags()).contains("LYRICS_NOT_INCLUDED");
        }

        @Test
        @DisplayName("story package includes COPYRIGHT_REVIEW_REQUIRED flag")
        void copyrightFlag() {
            var project = createDraftProject();
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.getReviewFlags()).contains("COPYRIGHT_REVIEW_REQUIRED");
        }

        @Test
        @DisplayName("story summary does not contain actual lyrics body")
        void noLyricsInSummary() {
            var project = createDraftProject();
            var story = planningService.prepareStory(project.getId(), null);
            // Should not contain verse/chorus patterns or actual lyric text
            assertThat(story.getStorySummary()).doesNotContain("[Nakarat]");
            assertThat(story.getStorySummary()).doesNotContain("[Kıta]");
        }
    }

    // ── B. Scenario Preparation ─────────────────────────────

    @Nested
    @DisplayName("B. Scenario Preparation")
    class ScenarioPreparation {

        @Test
        @DisplayName("prepare scenario after story succeeds")
        void scenarioAfterStory() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            var scenario = planningService.prepareScenario(project.getId(), null);

            assertThat(scenario).isNotNull();
            assertThat(scenario.getTitle()).contains("Mihriban");
            assertThat(scenario.getLogline()).isNotBlank();
            assertThat(scenario.getScenarioBeats()).isNotEmpty();
        }

        @Test
        @DisplayName("scenario has 6 beats by default")
        void sixBeats() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            var scenario = planningService.prepareScenario(project.getId(), null);
            assertThat(scenario.getScenarioBeats()).hasSize(6);
        }

        @Test
        @DisplayName("scenario beats include standard structure")
        void standardBeats() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            var scenario = planningService.prepareScenario(project.getId(), null);
            List<String> beats = scenario.getScenarioBeats();
            assertThat(beats.get(0)).contains("Hook");
            assertThat(beats.get(1)).contains("Giriş");
            assertThat(beats.get(4)).contains("Kapanış");
            assertThat(beats.get(5)).contains("CTA");
        }

        @Test
        @DisplayName("project advances to SCENARIO_PREPARED")
        void stageAdvanced() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            planningService.prepareScenario(project.getId(), null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.SCENARIO_PREPARED);
        }

        @Test
        @DisplayName("scenario includes LYRICS_NOT_INCLUDED flag")
        void lyricsFlag() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            var scenario = planningService.prepareScenario(project.getId(), null);
            assertThat(scenario.getReviewFlags()).contains("LYRICS_NOT_INCLUDED");
        }

        @Test
        @DisplayName("custom scene count respected (clamped 3-12)")
        void customSceneCount() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            var req = new TurkuScenarioPreparationRequest();
            req.setSceneCountHint(8);
            var scenario = planningService.prepareScenario(project.getId(), req);
            assertThat(scenario.getSceneCountHint()).isEqualTo(8);
        }
    }

    // ── C. Storyboard Preparation ───────────────────────────

    @Nested
    @DisplayName("C. Storyboard Preparation")
    class StoryboardPreparation {

        @Test
        @DisplayName("prepare storyboard after scenario succeeds")
        void storyboardAfterScenario() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            planningService.prepareScenario(project.getId(), null);
            var storyboard = planningService.prepareStoryboard(project.getId(), null);

            assertThat(storyboard).isNotNull();
            assertThat(storyboard.getScenes()).isNotEmpty();
            assertThat(storyboard.getTotalEstimatedDurationSeconds()).isGreaterThan(0);
        }

        @Test
        @DisplayName("storyboard scenes have required fields")
        void sceneFields() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            planningService.prepareScenario(project.getId(), null);
            var storyboard = planningService.prepareStoryboard(project.getId(), null);

            var scene = storyboard.getScenes().get(0);
            assertThat(scene.getSceneNo()).isEqualTo(1);
            assertThat(scene.getSceneTitle()).isNotBlank();
            assertThat(scene.getVisualDescription()).isNotBlank();
            assertThat(scene.getNarrationSummary()).isNotBlank();
            assertThat(scene.getMood()).isNotBlank();
            assertThat(scene.getEstimatedDurationSeconds()).isGreaterThan(0);
            assertThat(scene.getCameraDirection()).isNotBlank();
        }

        @Test
        @DisplayName("storyboard has copyright review flags")
        void reviewFlags() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            planningService.prepareScenario(project.getId(), null);
            var storyboard = planningService.prepareStoryboard(project.getId(), null);
            assertThat(storyboard.getReviewFlags()).contains("COPYRIGHT_REVIEW_REQUIRED");
            assertThat(storyboard.getReviewFlags()).contains("CULTURAL_CONTEXT_REVIEW_REQUIRED");
        }

        @Test
        @DisplayName("project advances to STORYBOARD_PREPARED")
        void stageAdvanced() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            planningService.prepareScenario(project.getId(), null);
            planningService.prepareStoryboard(project.getId(), null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.STORYBOARD_PREPARED);
        }

        @Test
        @DisplayName("music note indicates copyright check needed")
        void musicNoteCopyright() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            planningService.prepareScenario(project.getId(), null);
            var storyboard = planningService.prepareStoryboard(project.getId(), null);
            assertThat(storyboard.getScenes().get(0).getMusicNote())
                    .contains("telif");
        }
    }

    // ── D. Stage Transition Enforcement ─────────────────────

    @Nested
    @DisplayName("D. Stage Transition Enforcement")
    class StageTransition {

        @Test
        @DisplayName("story from wrong stage rejected")
        void storyFromWrongStage() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            // try again from STORY_CONTEXT_PREPARED
            assertThatThrownBy(() -> planningService.prepareStory(project.getId(), null))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("Geçersiz aşama");
        }

        @Test
        @DisplayName("scenario from DRAFT_CREATED rejected")
        void scenarioFromDraft() {
            var project = createDraftProject();
            assertThatThrownBy(() -> planningService.prepareScenario(project.getId(), null))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("Geçersiz aşama");
        }

        @Test
        @DisplayName("storyboard from DRAFT_CREATED rejected")
        void storyboardFromDraft() {
            var project = createDraftProject();
            assertThatThrownBy(() -> planningService.prepareStoryboard(project.getId(), null))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("storyboard from STORY_CONTEXT_PREPARED rejected")
        void storyboardFromStory() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            assertThatThrownBy(() -> planningService.prepareStoryboard(project.getId(), null))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("unknown project ID returns safe error")
        void unknownProject() {
            assertThatThrownBy(() -> planningService.prepareStory(UUID.randomUUID(), null))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("bulunamadı");
        }
    }

    // ── E. Input Validation ─────────────────────────────────

    @Nested
    @DisplayName("E. Input Validation")
    class InputValidation {

        @Test
        @DisplayName("too long narrative hint rejected")
        void longHint() {
            var project = createDraftProject();
            var req = new TurkuStoryPreparationRequest();
            req.setNarrativeAngleHint("a".repeat(201));
            assertThatThrownBy(() -> planningService.prepareStory(project.getId(), req))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("script injection in hint rejected")
        void scriptHint() {
            var project = createDraftProject();
            var req = new TurkuStoryPreparationRequest();
            req.setNarrativeAngleHint("<script>alert('xss')</script>");
            assertThatThrownBy(() -> planningService.prepareStory(project.getId(), req))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("credential pattern in hint rejected")
        void credentialHint() {
            var project = createDraftProject();
            var req = new TurkuStoryPreparationRequest();
            req.setNarrativeAngleHint("password=mysecret");
            assertThatThrownBy(() -> planningService.prepareStory(project.getId(), req))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("too long scenario tone hint rejected")
        void longTone() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            var req = new TurkuScenarioPreparationRequest();
            req.setNarrationToneHint("a".repeat(201));
            assertThatThrownBy(() -> planningService.prepareScenario(project.getId(), req))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("too long visual style hint rejected")
        void longVisual() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            planningService.prepareScenario(project.getId(), null);
            var req = new TurkuStoryboardPreparationRequest();
            req.setVisualStyleHint("a".repeat(201));
            assertThatThrownBy(() -> planningService.prepareStoryboard(project.getId(), req))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }
    }

    // ── F. No Forbidden DTO Fields ──────────────────────────

    @Nested
    @DisplayName("F. No Forbidden DTO Fields")
    class NoForbiddenFields {

        private static final List<String> FORBIDDEN = List.of(
                "token", "jwt", "session", "credential", "password",
                "secret", "apiKey", "accessKey", "principalRaw",
                "authorizationHeader", "publicUrl", "signedUrl",
                "cdnUrl", "providerUrl", "storageKey", "bucket",
                "objectPath", "rawPayload", "base64", "lyrics",
                "downloadUrl", "exportUrl");

        @Test
        @DisplayName("TurkuStoryPackage no forbidden fields")
        void storyPackage() { checkFields(TurkuStoryPackage.class); }

        @Test
        @DisplayName("TurkuScenarioPlan no forbidden fields")
        void scenarioPlan() { checkFields(TurkuScenarioPlan.class); }

        @Test
        @DisplayName("TurkuStoryboardPlan no forbidden fields")
        void storyboardPlan() { checkFields(TurkuStoryboardPlan.class); }

        @Test
        @DisplayName("TurkuStoryboardScene no forbidden fields")
        void storyboardScene() { checkFields(TurkuStoryboardScene.class); }

        @Test
        @DisplayName("TurkuNarrativeContext no forbidden fields")
        void narrativeContext() { checkFields(TurkuNarrativeContext.class); }

        @Test
        @DisplayName("TurkuPreparationResponse no forbidden fields")
        void preparationResponse() { checkFields(TurkuPreparationResponse.class); }

        @Test
        @DisplayName("TurkuStoryPreparationRequest no forbidden fields")
        void storyRequest() { checkFields(TurkuStoryPreparationRequest.class); }

        @Test
        @DisplayName("TurkuScenarioPreparationRequest no forbidden fields")
        void scenarioRequest() { checkFields(TurkuScenarioPreparationRequest.class); }

        @Test
        @DisplayName("TurkuStoryboardPreparationRequest no forbidden fields")
        void storyboardRequest() { checkFields(TurkuStoryboardPreparationRequest.class); }

        private void checkFields(Class<?> c) {
            Set<String> fields = Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }

    // ── G. Full Workflow Pipeline ────────────────────────────

    @Nested
    @DisplayName("G. Full Workflow Pipeline")
    class FullPipeline {

        @Test
        @DisplayName("DRAFT → STORY → SCENARIO → STORYBOARD full pipeline")
        void fullPipeline() {
            var project = createDraftProject();

            // story
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.getTurkuName()).isEqualTo("Mihriban");

            // scenario
            var scenario = planningService.prepareScenario(project.getId(), null);
            assertThat(scenario.getScenarioBeats()).hasSizeGreaterThanOrEqualTo(6);

            // storyboard
            var storyboard = planningService.prepareStoryboard(project.getId(), null);
            assertThat(storyboard.getScenes()).hasSizeGreaterThanOrEqualTo(6);

            // final state
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.STORYBOARD_PREPARED);
            assertThat(updated.getStoryPackage()).isNotNull();
            assertThat(updated.getScenarioPlan()).isNotNull();
            assertThat(updated.getStoryboardPlan()).isNotNull();
        }

        @Test
        @DisplayName("progress reflects stage after storyboard preparation")
        void progressReflects() {
            var project = createDraftProject();
            planningService.prepareStory(project.getId(), null);
            planningService.prepareScenario(project.getId(), null);
            planningService.prepareStoryboard(project.getId(), null);

            var progress = TurkuVideoProgressResponse.from(bootstrapService.getById(project.getId()));
            assertThat(progress.getCurrentStage()).isEqualTo(TurkuVideoWorkflowStage.STORYBOARD_PREPARED);
            assertThat(progress.getNextAvailableAction()).isEqualTo("prepare-scene-assets");
        }
    }

    // ── H. Source Confidence / Safety ────────────────────────

    @Nested
    @DisplayName("H. Source Confidence and Safety")
    class SourceSafety {

        @Test
        @DisplayName("LOW confidence story includes SOURCE_REVIEW_REQUIRED")
        void lowConfidenceFlag() {
            var project = createDraftProject();
            // project has source notes → LOW confidence
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.getReviewFlags()).contains("SOURCE_REVIEW_REQUIRED");
        }

        @Test
        @DisplayName("UNKNOWN confidence story includes SOURCE_REVIEW_REQUIRED")
        void unknownConfidenceFlag() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Test");
            var project = bootstrapService.bootstrap(req, "admin");
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.getReviewFlags()).contains("SOURCE_REVIEW_REQUIRED");
        }

        @Test
        @DisplayName("CULTURAL_CONTEXT_REVIEW_REQUIRED in story flags")
        void culturalFlag() {
            var project = createDraftProject();
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.getReviewFlags()).contains("CULTURAL_CONTEXT_REVIEW_REQUIRED");
        }

        @Test
        @DisplayName("story origin summary uses cautious wording")
        void cautiousWording() {
            var project = createDraftProject();
            var story = planningService.prepareStory(project.getId(), null);
            assertThat(story.getOriginSummary()).contains("uzman incelemesi önerilir");
        }
    }

    // ── I. Controller Endpoint Inventory ────────────────────

    @Nested
    @DisplayName("I. Endpoint Inventory (30 = 16 POST + 14 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("TurkuVideoProjectController: 7 POST + 2 GET")
        void turkuController() {
            long post = countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("türkü POST").isEqualTo(7);
            assertThat(get).as("türkü GET").isEqualTo(2);
        }

        @Test
        @DisplayName("AdminReviewController: 9 POST + 8 GET (unchanged)")
        void reviewController() {
            assertThat(countMethods(AdminReviewController.class, PostMapping.class)).isEqualTo(9);
            assertThat(countMethods(AdminReviewController.class, GetMapping.class)).isEqualTo(8);
        }

        @Test
        @DisplayName("total 30 = 16 POST + 14 GET")
        void total() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("total POST").isEqualTo(16);
            assertThat(get).as("total GET").isEqualTo(14);
            assertThat(post + get).as("total").isEqualTo(30);
        }

        @Test
        @DisplayName("no PUT/DELETE on türkü controller")
        void noPutDelete() {
            assertThat(countMethods(TurkuVideoProjectController.class, PutMapping.class)).isZero();
            assertThat(countMethods(TurkuVideoProjectController.class, DeleteMapping.class)).isZero();
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── J. Controller Dependencies ──────────────────────────

    @Nested
    @DisplayName("J. Controller Dependencies")
    class ControllerDeps {

        @Test
        @DisplayName("controller has TurkuVideoPlanningService")
        void hasPlanningService() {
            assertThat(Arrays.stream(TurkuVideoProjectController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet()))
                    .contains(TurkuVideoPlanningService.class);
        }

        @Test
        @DisplayName("controller has AdminActorResolver")
        void hasResolver() {
            assertThat(Arrays.stream(TurkuVideoProjectController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet()))
                    .contains(AdminActorResolver.class);
        }
    }

    // ── K. Exception Safety ─────────────────────────────────

    @Nested
    @DisplayName("K. Exception Safety")
    class ExceptionSafety {

        @Test
        @DisplayName("INVALID_STAGE_TRANSITION exception is safe")
        void stageTransitionSafe() {
            var ex = TurkuVideoProjectException.invalidStageTransition(
                    TurkuVideoWorkflowStage.DRAFT_CREATED, TurkuVideoWorkflowStage.SCENARIO_PREPARED);
            assertThat(ex.getSafeErrorCode()).isEqualTo("INVALID_STAGE_TRANSITION");
            assertThat(ex.getMessage()).doesNotContain("token");
            assertThat(ex.getMessage()).doesNotContain("password");
        }

        @Test
        @DisplayName("MISSING_PREREQUISITE exception is safe")
        void prerequisiteSafe() {
            var ex = TurkuVideoProjectException.missingPrerequisite("Test");
            assertThat(ex.getSafeErrorCode()).isEqualTo("MISSING_PREREQUISITE");
        }
    }
}
