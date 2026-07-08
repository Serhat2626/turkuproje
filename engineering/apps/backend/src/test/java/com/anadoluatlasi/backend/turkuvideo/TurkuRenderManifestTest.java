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
 * FP-021 Task 03: Render manifest + readiness backend tests.
 */
@DisplayName("FP-021 Task 03 — Render Manifest + Readiness Backend")
class TurkuRenderManifestTest {

    private TurkuVideoProjectRepository repository;
    private TurkuVideoProjectBootstrapService bootstrapService;
    private TurkuVideoCompositionService compositionService;
    private TurkuRenderManifestService renderManifestService;
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
        renderManifestService = new TurkuRenderManifestService(repository);
        var renderJobService = new TurkuRenderJobService(repository, renderManifestService);
        var renderRunner = new StubTurkuRenderRunnerGateway();
        var renderExecutionService = new TurkuRenderExecutionService(repository, renderRunner);
        var actorResolver = new AdminActorResolver();
        controller = new TurkuVideoProjectController(
                bootstrapService, planningService, assetService,
                draftService, imageBatchService, compositionService,
                renderManifestService, renderJobService, renderExecutionService,
                new TurkuStoryboardInternalRenderChainHarness(), actorResolver);
    }

    private TurkuVideoProject bootstrapToCompositionReady() {
        TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("Çanakkale Türküsü");
        req.setRegion("Çanakkale");
        req.setMood("Kahramanlık");
        var project = bootstrapService.bootstrap(req, "test-admin");
        UUID id = project.getId();
        controller.prepareStory(id, null, null, null);
        controller.prepareScenario(id, null, null, null);
        controller.prepareStoryboard(id, null, null, null);
        controller.prepareSceneAssets(id, null, null, null);
        controller.prepareImageBatch(id, null, null, null);
        controller.completeImageBatch(id, null, null, null);
        controller.prepareCompositionPlan(id, null, null, null);
        return bootstrapService.getById(id);
    }

    // ── A. Endpoint Inventory ────────────────────────────

    @Nested
    @DisplayName("A. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("total 46 = 26 POST + 20 GET")
        void totalEndpoints() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("total POST").isEqualTo(26);
            assertThat(get).as("total GET").isEqualTo(20);
            assertThat(post + get).as("total endpoints").isEqualTo(46);
        }

        @Test
        @DisplayName("TurkuVideoProjectController: 17 POST + 8 GET")
        void turkuControllerCounts() {
            assertThat(countMethods(TurkuVideoProjectController.class, PostMapping.class))
                    .as("POST").isEqualTo(17);
            assertThat(countMethods(TurkuVideoProjectController.class, GetMapping.class))
                    .as("GET").isEqualTo(8);
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
        @DisplayName("prepare-render-manifest endpoint exists")
        void manifestEndpoint() {
            boolean found = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .anyMatch(m -> Arrays.stream(m.getAnnotation(PostMapping.class).value())
                            .anyMatch(v -> v.contains("prepare-render-manifest")));
            assertThat(found).isTrue();
        }

        @Test
        @DisplayName("render-readiness endpoint exists")
        void readinessEndpoint() {
            boolean found = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .anyMatch(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value())
                            .anyMatch(v -> v.contains("render-readiness")));
            assertThat(found).isTrue();
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── B. Render Manifest Happy Path ────────────────────

    @Nested
    @DisplayName("B. Render Manifest Happy Path")
    class ManifestHappyPath {

        @Test
        @DisplayName("render manifest after COMPOSITION_PLAN_PREPARED succeeds")
        void success() {
            var project = bootstrapToCompositionReady();
            assertThat(project.getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.COMPOSITION_PLAN_PREPARED);
            var resp = controller.prepareRenderManifest(project.getId(), null, null, null);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var body = resp.getBody().getData();
            assertThat(body.getProjectId()).isEqualTo(project.getId());
            assertThat(body.getManifestId()).isNotNull();
            assertThat(body.getSceneCount()).isGreaterThan(0);
            assertThat(body.getTotalDurationSeconds()).isGreaterThan(0);
            assertThat(body.getTargetResolution()).isEqualTo("1920x1080");
            assertThat(body.getTargetFps()).isEqualTo(30);
            assertThat(body.getTargetFormat()).isEqualTo("mp4");
        }

        @Test
        @DisplayName("stage advances to RENDER_MANIFEST_PREPARED")
        void stageAdvance() {
            var project = bootstrapToCompositionReady();
            controller.prepareRenderManifest(project.getId(), null, null, null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_MANIFEST_PREPARED);
        }

        @Test
        @DisplayName("manifest stored on project")
        void manifestStored() {
            var project = bootstrapToCompositionReady();
            controller.prepareRenderManifest(project.getId(), null, null, null);
            var updated = bootstrapService.getById(project.getId());
            assertThat(updated.getRenderManifest()).isNotNull();
            assertThat(updated.getRenderManifest().getScenes()).isNotEmpty();
        }

        @Test
        @DisplayName("manifest scene order matches composition plan")
        void sceneOrder() {
            var project = bootstrapToCompositionReady();
            int compositionSceneCount = project.getCompositionPlan().getScenes().size();
            controller.prepareRenderManifest(project.getId(), null, null, null);
            var manifest = bootstrapService.getById(project.getId()).getRenderManifest();
            assertThat(manifest.getScenes()).hasSize(compositionSceneCount);
            for (int i = 0; i < manifest.getScenes().size(); i++) {
                assertThat(manifest.getScenes().get(i).getSceneNo())
                        .isEqualTo(project.getCompositionPlan().getScenes().get(i).getSceneNo());
            }
        }
    }

    // ── C. Render Readiness ─────────────────────────────

    @Nested
    @DisplayName("C. Render Readiness")
    class ReadinessTests {

        @Test
        @DisplayName("readiness returns safe report")
        void safeReport() {
            var project = bootstrapToCompositionReady();
            controller.prepareRenderManifest(project.getId(), null, null, null);
            var resp = controller.getRenderReadiness(project.getId());
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var report = resp.getBody().getData();
            assertThat(report.getProjectId()).isEqualTo(project.getId());
            assertThat(report.isHumanReviewRequired()).isTrue();
            assertThat(report.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("readiness without manifest shows BLOCKED")
        void noManifest() {
            var project = bootstrapToCompositionReady();
            // don't prepare manifest
            var resp = controller.getRenderReadiness(project.getId());
            var report = resp.getBody().getData();
            assertThat(report.getStatus()).isEqualTo(TurkuRenderReadinessStatus.BLOCKED);
            assertThat(report.getIssues()).anyMatch(i -> "MISSING_MANIFEST".equals(i.getCode()));
        }

        @Test
        @DisplayName("readiness with manifest shows NEEDS_REVIEW")
        void withManifest() {
            var project = bootstrapToCompositionReady();
            controller.prepareRenderManifest(project.getId(), null, null, null);
            var resp = controller.getRenderReadiness(project.getId());
            var report = resp.getBody().getData();
            assertThat(report.isCompositionReady()).isTrue();
            assertThat(report.isManifestReady()).isTrue();
            assertThat(report.getStatus()).isEqualTo(TurkuRenderReadinessStatus.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("readiness for DRAFT_CREATED is BLOCKED")
        void draftBlocked() {
            TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Test Türkü");
            var project = bootstrapService.bootstrap(req, "admin");
            var resp = controller.getRenderReadiness(project.getId());
            var report = resp.getBody().getData();
            assertThat(report.getStatus()).isEqualTo(TurkuRenderReadinessStatus.BLOCKED);
            assertThat(report.isCompositionReady()).isFalse();
            assertThat(report.isManifestReady()).isFalse();
        }

        @Test
        @DisplayName("readiness does NOT claim publish/export readiness")
        void notPublishReady() {
            var project = bootstrapToCompositionReady();
            controller.prepareRenderManifest(project.getId(), null, null, null);
            var resp = controller.getRenderReadiness(project.getId());
            var report = resp.getBody().getData();
            assertThat(report.getReviewFlags()).contains("NOT_PUBLISH_READY", "NOT_EXPORT_READY");
            assertThat(report.isCopyrightReviewDone()).isFalse();
        }

        @Test
        @DisplayName("readiness score is 0-100")
        void readinessScore() {
            var project = bootstrapToCompositionReady();
            controller.prepareRenderManifest(project.getId(), null, null, null);
            var resp = controller.getRenderReadiness(project.getId());
            var report = resp.getBody().getData();
            assertThat(report.getReadinessScore()).isBetween(0, 100);
        }
    }

    // ── D. Error Cases ──────────────────────────────────

    @Nested
    @DisplayName("D. Error Cases")
    class ErrorCases {

        @Test
        @DisplayName("manifest before composition fails safely")
        void noComposition() {
            TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Test Türkü");
            var project = bootstrapService.bootstrap(req, "admin");
            assertThatThrownBy(() ->
                    renderManifestService.prepareRenderManifest(project.getId(), null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("manifest at IMAGE_BATCH_COMPLETED fails safely")
        void wrongStage() {
            TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Test Türkü");
            var project = bootstrapService.bootstrap(req, "admin");
            controller.prepareStory(project.getId(), null, null, null);
            controller.prepareScenario(project.getId(), null, null, null);
            controller.prepareStoryboard(project.getId(), null, null, null);
            controller.prepareSceneAssets(project.getId(), null, null, null);
            controller.prepareImageBatch(project.getId(), null, null, null);
            controller.completeImageBatch(project.getId(), null, null, null);
            assertThatThrownBy(() ->
                    renderManifestService.prepareRenderManifest(project.getId(), null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("unknown project ID safe error")
        void unknownId() {
            UUID unknown = UUID.randomUUID();
            assertThatThrownBy(() ->
                    renderManifestService.prepareRenderManifest(unknown, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("readiness for unknown project safe error")
        void readinessUnknown() {
            UUID unknown = UUID.randomUUID();
            assertThatThrownBy(() ->
                    renderManifestService.evaluateReadiness(unknown))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── E. Safety Invariants ────────────────────────────

    @Nested
    @DisplayName("E. Safety Invariants")
    class SafetyInvariants {

        @Test
        @DisplayName("humanReviewRequired=true always")
        void humanReview() {
            var project = bootstrapToCompositionReady();
            var resp = controller.prepareRenderManifest(project.getId(), null, null, null);
            assertThat(resp.getBody().getData().isHumanReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("autoApproved=false always")
        void noAutoApproval() {
            var project = bootstrapToCompositionReady();
            var resp = controller.prepareRenderManifest(project.getId(), null, null, null);
            assertThat(resp.getBody().getData().isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("copyrightReviewRequired=true always")
        void copyrightReview() {
            var project = bootstrapToCompositionReady();
            var resp = controller.prepareRenderManifest(project.getId(), null, null, null);
            assertThat(resp.getBody().getData().isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("review flags include internal-only marker")
        void internalOnly() {
            var project = bootstrapToCompositionReady();
            var resp = controller.prepareRenderManifest(project.getId(), null, null, null);
            assertThat(resp.getBody().getData().getReviewFlags())
                    .contains("INTERNAL_READINESS_ONLY", "NO_RENDER_PRODUCED");
        }

        @Test
        @DisplayName("manifest image refs are safe internal refs")
        void safeRefs() {
            var project = bootstrapToCompositionReady();
            controller.prepareRenderManifest(project.getId(), null, null, null);
            var manifest = bootstrapService.getById(project.getId()).getRenderManifest();
            for (var scene : manifest.getScenes()) {
                if (scene.getImageRef() != null) {
                    assertThat(scene.getImageRef()).startsWith("internal-ref:");
                    assertThat(scene.getImageRef()).doesNotContain("http", "cdn", "public", "signed");
                }
                if (scene.getNarrationRef() != null) {
                    assertThat(scene.getNarrationRef()).startsWith("narration-ref:");
                }
            }
        }

        @Test
        @DisplayName("SYSTEM role blocked for POST")
        void systemBlocked() {
            var project = bootstrapToCompositionReady();
            assertThatThrownBy(() ->
                    controller.prepareRenderManifest(project.getId(), null,
                            ReviewActorRole.SYSTEM, null))
                    .isInstanceOf(AdminActorResolutionException.class);
        }
    }

    // ── F. DTO Safety — Forbidden Fields ────────────────

    @Nested
    @DisplayName("F. DTO Safety — Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawProviderResponse", "rawProviderRequest",
                "token", "jwt", "session", "credential", "password", "secret",
                "apiKey", "authorizationHeader", "principalRaw",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "imageData", "binaryData",
                "lyrics", "videoData", "audioData", "providerEndpoint",
                "renderedVideo", "videoFile", "downloadFile");

        @Test
        @DisplayName("TurkuRenderManifestResponse has no forbidden fields")
        void response() { assertNoForbiddenFields(TurkuRenderManifestResponse.class); }

        @Test
        @DisplayName("TurkuRenderManifest has no forbidden fields")
        void manifest() { assertNoForbiddenFields(TurkuRenderManifest.class); }

        @Test
        @DisplayName("TurkuRenderManifestScene has no forbidden fields")
        void scene() { assertNoForbiddenFields(TurkuRenderManifestScene.class); }

        @Test
        @DisplayName("TurkuRenderReadinessReport has no forbidden fields")
        void readiness() { assertNoForbiddenFields(TurkuRenderReadinessReport.class); }

        @Test
        @DisplayName("TurkuRenderSafetyState has no forbidden fields")
        void safety() { assertNoForbiddenFields(TurkuRenderSafetyState.class); }

        @Test
        @DisplayName("TurkuRenderReadinessIssue has no forbidden fields")
        void issue() { assertNoForbiddenFields(TurkuRenderReadinessIssue.class); }

        private void assertNoForbiddenFields(Class<?> clazz) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("field '%s' in %s", f, clazz.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }

    // ── G. Forbidden Behavior ───────────────────────────

    @Nested
    @DisplayName("G. Forbidden Behavior")
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
        @DisplayName("RENDER_MANIFEST_PREPARED stage exists")
        void renderStageExists() {
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_MANIFEST_PREPARED")).isNotNull();
        }

        @Test
        @DisplayName("no forbidden workflow stages")
        void noForbiddenStages() {
            var stages = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(stages).doesNotContain("PUBLISHED", "EXPORTED", "AUTO_APPROVED",
                    "PUBLIC_READY", "CDN_DELIVERED", "DOWNLOAD_READY");
        }
    }
}
