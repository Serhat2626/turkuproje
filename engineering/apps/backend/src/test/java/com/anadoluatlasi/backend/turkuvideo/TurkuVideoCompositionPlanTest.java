package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-021 Task 02: Composition plan backend tests.
 */
@DisplayName("FP-021 Task 02 — Composition Plan Backend")
class TurkuVideoCompositionPlanTest {

    private TurkuVideoProjectRepository repository;
    private TurkuVideoProjectBootstrapService bootstrapService;
    private TurkuVideoCompositionService compositionService;
    private TurkuVideoProjectController controller;

    @BeforeEach
    void setUp() {
        repository = new TurkuVideoProjectRepository();
        var gateway = new StubTurkuGenerationProviderGateway();
        var sanitizer = new TurkuProviderSafetySanitizer();
        var policy = new TurkuProviderSafetyPolicy();
        bootstrapService = new TurkuVideoProjectBootstrapService(repository);
        var planningService = new TurkuVideoPlanningService(repository);
        var assetService = new TurkuVideoAssetPlanningService(repository);
        var draftService = new TurkuProviderDraftPlanningService(repository, gateway, sanitizer, policy);
        var imageBatchService = new TurkuProviderImageBatchService(repository, gateway, sanitizer, policy);
        compositionService = new TurkuVideoCompositionService(repository);
        var actorResolver = new AdminActorResolver();
        controller = new TurkuVideoProjectController(
                bootstrapService, planningService, assetService,
                draftService, imageBatchService, compositionService, actorResolver);
    }

    private TurkuVideoProject bootstrapAndPrepare() {
        TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("Çanakkale Türküsü");
        req.setRegion("Çanakkale");
        req.setMood("Kahramanlık");
        var project = bootstrapService.bootstrap(req, "test-admin");
        UUID id = project.getId();
        // Advance through pipeline to IMAGE_BATCH_COMPLETED
        controller.prepareStory(id, null, null, null);
        controller.prepareScenario(id, null, null, null);
        controller.prepareStoryboard(id, null, null, null);
        controller.prepareSceneAssets(id, null, null, null);
        controller.prepareImageBatch(id, null, null, null);
        controller.completeImageBatch(id, null, null, null);
        return bootstrapService.getById(id);
    }

    // ── A. Endpoint Inventory ────────────────────────────

    @Nested
    @DisplayName("A. Endpoint Inventory (37 = 22 POST + 15 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("total 37 = 22 POST + 15 GET")
        void totalEndpoints() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("total POST").isEqualTo(22);
            assertThat(get).as("total GET").isEqualTo(15);
            assertThat(post + get).as("total endpoints").isEqualTo(37);
        }

        @Test
        @DisplayName("TurkuVideoProjectController: 13 POST + 3 GET")
        void turkuControllerCounts() {
            assertThat(countMethods(TurkuVideoProjectController.class, PostMapping.class))
                    .as("POST").isEqualTo(13);
            assertThat(countMethods(TurkuVideoProjectController.class, GetMapping.class))
                    .as("GET").isEqualTo(3);
        }

        @Test
        @DisplayName("no PUT/DELETE endpoints")
        void noUnsafe() {
            long put = countMethods(TurkuVideoProjectController.class, PutMapping.class);
            long del = countMethods(TurkuVideoProjectController.class, DeleteMapping.class);
            assertThat(put).isEqualTo(0);
            assertThat(del).isEqualTo(0);
        }

        @Test
        @DisplayName("prepare-composition-plan endpoint exists")
        void endpointExists() {
            boolean found = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .anyMatch(m -> {
                        PostMapping ann = m.getAnnotation(PostMapping.class);
                        return Arrays.stream(ann.value())
                                .anyMatch(v -> v.contains("prepare-composition-plan"));
                    });
            assertThat(found).as("prepare-composition-plan endpoint").isTrue();
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── B. Happy Path ────────────────────────────────────

    @Nested
    @DisplayName("B. Composition Plan Happy Path")
    class HappyPath {

        @Test
        @DisplayName("composition plan after IMAGE_BATCH_COMPLETED succeeds")
        void completedBatch() {
            var project = bootstrapAndPrepare();
            assertThat(project.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.IMAGE_BATCH_COMPLETED);

            var resp = controller.prepareCompositionPlan(project.getId(), null, null, null);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var body = resp.getBody().getData();
            assertThat(body.getProjectId()).isEqualTo(project.getId());
            assertThat(body.getCompositionId()).isNotNull();
            assertThat(body.getSceneCount()).isGreaterThan(0);
            assertThat(body.getTotalDurationSeconds()).isGreaterThan(0);
            assertThat(body.isHumanReviewRequired()).isTrue();
            assertThat(body.isCopyrightReviewRequired()).isTrue();
            assertThat(body.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("stage advances to COMPOSITION_PLAN_PREPARED")
        void stageAdvance() {
            var project = bootstrapAndPrepare();
            controller.prepareCompositionPlan(project.getId(), null, null, null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.COMPOSITION_PLAN_PREPARED);
        }

        @Test
        @DisplayName("composition plan is stored on project")
        void planStored() {
            var project = bootstrapAndPrepare();
            controller.prepareCompositionPlan(project.getId(), null, null, null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getCompositionPlan()).isNotNull();
            assertThat(updated.getCompositionPlan().getScenes()).isNotEmpty();
        }

        @Test
        @DisplayName("composition scenes align with storyboard scenes")
        void scenesAlign() {
            var project = bootstrapAndPrepare();
            int storyboardSceneCount = project.getStoryboardPlan().getScenes().size();
            controller.prepareCompositionPlan(project.getId(), null, null, null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getCompositionPlan().getScenes())
                    .hasSize(storyboardSceneCount);
        }

        @Test
        @DisplayName("narration segments match scene count")
        void narrationSegments() {
            var project = bootstrapAndPrepare();
            controller.prepareCompositionPlan(project.getId(), null, null, null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getCompositionPlan().getNarrationSegments())
                    .hasSize(updated.getCompositionPlan().getScenes().size());
        }

        @Test
        @DisplayName("timing plan has durations for all scenes")
        void timingPlan() {
            var project = bootstrapAndPrepare();
            controller.prepareCompositionPlan(project.getId(), null, null, null);
            var plan = bootstrapService.getById(project.getId()).getCompositionPlan();
            assertThat(plan.getTimingPlan()).isNotNull();
            assertThat(plan.getTimingPlan().getSceneDurations())
                    .hasSize(plan.getScenes().size());
        }

        @Test
        @DisplayName("transition plan has directives")
        void transitionPlan() {
            var project = bootstrapAndPrepare();
            controller.prepareCompositionPlan(project.getId(), null, null, null);
            var plan = bootstrapService.getById(project.getId()).getCompositionPlan();
            assertThat(plan.getTransitionPlan()).isNotNull();
            assertThat(plan.getTransitionPlan().getDefaultTransition()).isNotEmpty();
        }
    }

    // ── C. Partial/Error Cases ───────────────────────────

    @Nested
    @DisplayName("C. Error/Edge Cases")
    class ErrorCases {

        @Test
        @DisplayName("DRAFT_CREATED fails safely")
        void draftCreated() {
            TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Test Türkü");
            var project = bootstrapService.bootstrap(req, "admin");
            assertThatThrownBy(() ->
                    compositionService.prepareCompositionPlan(project.getId(), null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("STORYBOARD_PREPARED fails safely (needs image batch)")
        void storyboardPrepared() {
            TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Test Türkü");
            var project = bootstrapService.bootstrap(req, "admin");
            controller.prepareStory(project.getId(), null, null, null);
            controller.prepareScenario(project.getId(), null, null, null);
            controller.prepareStoryboard(project.getId(), null, null, null);
            assertThatThrownBy(() ->
                    compositionService.prepareCompositionPlan(project.getId(), null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("unknown project ID safe error")
        void unknownId() {
            UUID unknown = UUID.randomUUID();
            assertThatThrownBy(() ->
                    compositionService.prepareCompositionPlan(unknown, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("IMAGE_BATCH_PARTIAL can still prepare composition")
        void partialBatch() {
            var project = bootstrapAndPrepare();
            // Force partial stage
            project.advanceTo(TurkuVideoWorkflowStage.IMAGE_BATCH_PARTIAL);
            repository.save(project);
            var resp = controller.prepareCompositionPlan(project.getId(), null, null, null);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var body = resp.getBody().getData();
            assertThat(body.getReviewFlags()).contains("IMAGE_BATCH_PARTIAL");
        }
    }

    // ── D. Safety Invariants ────────────────────────────

    @Nested
    @DisplayName("D. Safety Invariants")
    class SafetyInvariants {

        @Test
        @DisplayName("humanReviewRequired=true always")
        void humanReview() {
            var project = bootstrapAndPrepare();
            var resp = controller.prepareCompositionPlan(project.getId(), null, null, null);
            assertThat(resp.getBody().getData().isHumanReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("autoApproved=false always")
        void noAutoApproval() {
            var project = bootstrapAndPrepare();
            var resp = controller.prepareCompositionPlan(project.getId(), null, null, null);
            assertThat(resp.getBody().getData().isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("copyrightReviewRequired=true always")
        void copyrightReview() {
            var project = bootstrapAndPrepare();
            var resp = controller.prepareCompositionPlan(project.getId(), null, null, null);
            assertThat(resp.getBody().getData().isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("review flags include critical safety flags")
        void reviewFlags() {
            var project = bootstrapAndPrepare();
            var resp = controller.prepareCompositionPlan(project.getId(), null, null, null);
            assertThat(resp.getBody().getData().getReviewFlags()).contains(
                    "HUMAN_REVIEW_REQUIRED",
                    "COPYRIGHT_REVIEW_REQUIRED",
                    "LYRICS_NOT_INCLUDED",
                    "NO_RENDER_PRODUCED");
        }

        @Test
        @DisplayName("narration segments do NOT contain lyrics keywords")
        void noLyrics() {
            var project = bootstrapAndPrepare();
            controller.prepareCompositionPlan(project.getId(), null, null, null);
            var plan = bootstrapService.getById(project.getId()).getCompositionPlan();
            for (var seg : plan.getNarrationSegments()) {
                assertThat(seg.getNarrationText()).doesNotContainIgnoringCase("şarkı sözü");
                assertThat(seg.getNarrationText()).doesNotContainIgnoringCase("lyrics");
            }
        }

        @Test
        @DisplayName("image refs are safe internal refs only")
        void safeImageRefs() {
            var project = bootstrapAndPrepare();
            controller.prepareCompositionPlan(project.getId(), null, null, null);
            var plan = bootstrapService.getById(project.getId()).getCompositionPlan();
            for (var scene : plan.getScenes()) {
                if (scene.getImageRef() != null) {
                    assertThat(scene.getImageRef()).startsWith("internal-ref:");
                    assertThat(scene.getImageRef()).doesNotContain("http");
                    assertThat(scene.getImageRef()).doesNotContain("cdn");
                    assertThat(scene.getImageRef()).doesNotContain("public");
                    assertThat(scene.getImageRef()).doesNotContain("signed");
                }
            }
        }

        @Test
        @DisplayName("SYSTEM role blocked")
        void systemBlocked() {
            var project = bootstrapAndPrepare();
            assertThatThrownBy(() ->
                    controller.prepareCompositionPlan(project.getId(), null,
                            ReviewActorRole.SYSTEM, null))
                    .isInstanceOf(AdminActorResolutionException.class);
        }
    }

    // ── E. DTO Safety — Forbidden Fields ────────────────

    @Nested
    @DisplayName("E. DTO Safety — Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawProviderResponse", "rawProviderRequest",
                "rawRequest", "rawResponse", "rawPrompt",
                "token", "jwt", "session", "credential", "password", "secret",
                "apiKey", "authorizationHeader", "principalRaw",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "imageData", "binaryData",
                "lyrics", "videoData", "audioData", "providerEndpoint",
                "renderedVideo", "videoFile", "downloadFile");

        @Test
        @DisplayName("TurkuCompositionPlanResponse has no forbidden fields")
        void response() {
            assertNoForbiddenFields(TurkuCompositionPlanResponse.class);
        }

        @Test
        @DisplayName("TurkuVideoCompositionPlan has no forbidden fields")
        void plan() {
            assertNoForbiddenFields(TurkuVideoCompositionPlan.class);
        }

        @Test
        @DisplayName("TurkuVideoCompositionScene has no forbidden fields")
        void scene() {
            assertNoForbiddenFields(TurkuVideoCompositionScene.class);
        }

        @Test
        @DisplayName("TurkuNarrationSegment has no forbidden fields")
        void narration() {
            assertNoForbiddenFields(TurkuNarrationSegment.class);
        }

        @Test
        @DisplayName("TurkuSceneTimingPlan has no forbidden fields")
        void timing() {
            assertNoForbiddenFields(TurkuSceneTimingPlan.class);
        }

        @Test
        @DisplayName("TurkuTransitionPlan has no forbidden fields")
        void transition() {
            assertNoForbiddenFields(TurkuTransitionPlan.class);
        }

        private void assertNoForbiddenFields(Class<?> clazz) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("field '%s' in %s", f, clazz.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }

    // ── F. Forbidden Behavior ───────────────────────────

    @Nested
    @DisplayName("F. Forbidden Behavior")
    class ForbiddenBehavior {

        @Test
        @DisplayName("no publish/export/download/render methods")
        void noPublish() {
            var methods = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName).collect(Collectors.toSet());
            for (String m : List.of("publish", "export", "download", "publishVideo",
                    "exportVideo", "downloadVideo", "renderVideo", "finalizeVideo",
                    "autoApprove", "updateStatus", "genericStatusUpdate")) {
                assertThat(methods).doesNotContain(m);
            }
        }

        @Test
        @DisplayName("no forbidden workflow stages")
        void noForbiddenStages() {
            var stages = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(stages).doesNotContain("PUBLISHED", "EXPORTED", "AUTO_APPROVED",
                    "PUBLIC_READY", "CDN_DELIVERED", "DOWNLOAD_READY");
        }

        @Test
        @DisplayName("COMPOSITION_PLAN_PREPARED stage exists")
        void compositionStageExists() {
            assertThat(TurkuVideoWorkflowStage.valueOf("COMPOSITION_PLAN_PREPARED")).isNotNull();
        }

        @Test
        @DisplayName("no FP-021 migration files")
        void noMigrations() {
            var dir = new java.io.File("src/main/resources/db/migration");
            if (dir.exists() && dir.listFiles() != null) {
                boolean has021 = Arrays.stream(dir.listFiles())
                        .anyMatch(f -> f.getName().toLowerCase().contains("fp021")
                                || f.getName().toLowerCase().contains("composition"));
                assertThat(has021).isFalse();
            }
        }
    }

    // ── G. Composition Plan Domain ──────────────────────

    @Nested
    @DisplayName("G. Composition Plan Domain")
    class DomainTests {

        @Test
        @DisplayName("composition plan has correct projectId")
        void projectId() {
            var project = bootstrapAndPrepare();
            compositionService.prepareCompositionPlan(project.getId(), null);
            var plan = bootstrapService.getById(project.getId()).getCompositionPlan();
            assertThat(plan.getProjectId()).isEqualTo(project.getId());
        }

        @Test
        @DisplayName("composition plan has unique compositionId")
        void compositionId() {
            var p1 = bootstrapAndPrepare();
            compositionService.prepareCompositionPlan(p1.getId(), null);
            var plan1 = bootstrapService.getById(p1.getId()).getCompositionPlan();

            var p2 = bootstrapAndPrepare();
            compositionService.prepareCompositionPlan(p2.getId(), null);
            var plan2 = bootstrapService.getById(p2.getId()).getCompositionPlan();

            assertThat(plan1.getCompositionId()).isNotEqualTo(plan2.getCompositionId());
        }

        @Test
        @DisplayName("narration segments have language=tr")
        void narrationLanguage() {
            var project = bootstrapAndPrepare();
            compositionService.prepareCompositionPlan(project.getId(), null);
            var plan = bootstrapService.getById(project.getId()).getCompositionPlan();
            for (var seg : plan.getNarrationSegments()) {
                assertThat(seg.getLanguage()).isEqualTo("tr");
            }
        }

        @Test
        @DisplayName("narration segments ttsReady=false initially")
        void ttsNotReady() {
            var project = bootstrapAndPrepare();
            compositionService.prepareCompositionPlan(project.getId(), null);
            var plan = bootstrapService.getById(project.getId()).getCompositionPlan();
            for (var seg : plan.getNarrationSegments()) {
                assertThat(seg.isTtsReady()).isFalse();
            }
        }

        @Test
        @DisplayName("composition plan autoApproved=false on domain object")
        void domainAutoApproved() {
            var project = bootstrapAndPrepare();
            compositionService.prepareCompositionPlan(project.getId(), null);
            var plan = bootstrapService.getById(project.getId()).getCompositionPlan();
            assertThat(plan.isAutoApproved()).isFalse();
            assertThat(plan.isHumanReviewRequired()).isTrue();
            assertThat(plan.isCopyrightReviewRequired()).isTrue();
        }
    }
}
