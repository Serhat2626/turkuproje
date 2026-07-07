package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-021 Task 04: Composition preview endpoint tests.
 */
@DisplayName("FP-021 Task 04 — Composition Preview Endpoint")
class TurkuCompositionPreviewTest {

    private TurkuVideoProjectBootstrapService bootstrapService;
    private TurkuVideoProjectController controller;

    @BeforeEach
    void setUp() {
        var repository = new TurkuVideoProjectRepository();
        var gateway = new StubTurkuGenerationProviderGateway();
        var sanitizer = new TurkuProviderSafetySanitizer();
        var policy = new TurkuProviderSafetyPolicy();
        bootstrapService = new TurkuVideoProjectBootstrapService(repository);
        var planningService = new TurkuVideoPlanningService(repository);
        var assetService = new TurkuVideoAssetPlanningService(repository);
        var draftService = new TurkuProviderDraftPlanningService(repository, gateway, sanitizer, policy);
        var imageBatchService = new TurkuProviderImageBatchService(repository, gateway, sanitizer, policy);
        var compositionService = new TurkuVideoCompositionService(repository);
        var renderManifestService = new TurkuRenderManifestService(repository);
        var actorResolver = new AdminActorResolver();
        controller = new TurkuVideoProjectController(
                bootstrapService, planningService, assetService,
                draftService, imageBatchService, compositionService,
                renderManifestService, actorResolver);
    }

    private TurkuVideoProject bootstrapToRenderReady() {
        TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("Çanakkale Türküsü");
        req.setRegion("Çanakkale");
        req.setMood("Kahramanlık");
        var project = bootstrapService.bootstrap(req, "admin");
        UUID id = project.getId();
        controller.prepareStory(id, null, null, null);
        controller.prepareScenario(id, null, null, null);
        controller.prepareStoryboard(id, null, null, null);
        controller.prepareSceneAssets(id, null, null, null);
        controller.prepareImageBatch(id, null, null, null);
        controller.completeImageBatch(id, null, null, null);
        controller.prepareCompositionPlan(id, null, null, null);
        controller.prepareRenderManifest(id, null, null, null);
        return bootstrapService.getById(id);
    }

    @Nested
    @DisplayName("A. Endpoint Inventory (40 = 23 POST + 17 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("total 40 = 23 POST + 17 GET")
        void totalEndpoints() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("total POST").isEqualTo(23);
            assertThat(get).as("total GET").isEqualTo(17);
            assertThat(post + get).as("total endpoints").isEqualTo(40);
        }

        @Test
        @DisplayName("TurkuVideoProjectController: 14 POST + 5 GET")
        void turkuControllerCounts() {
            assertThat(countMethods(TurkuVideoProjectController.class, PostMapping.class))
                    .as("POST").isEqualTo(14);
            assertThat(countMethods(TurkuVideoProjectController.class, GetMapping.class))
                    .as("GET").isEqualTo(5);
        }

        @Test
        @DisplayName("composition-preview endpoint exists")
        void previewEndpoint() {
            boolean found = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .anyMatch(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value())
                            .anyMatch(v -> v.contains("composition-preview")));
            assertThat(found).isTrue();
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    @Nested
    @DisplayName("B. Preview Happy Path")
    class HappyPath {

        @Test
        @DisplayName("preview after render manifest returns 200")
        void afterManifest() {
            var project = bootstrapToRenderReady();
            var resp = controller.getCompositionPreview(project.getId());
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var body = resp.getBody().getData();
            assertThat(body.getProjectId()).isEqualTo(project.getId());
            assertThat(body.isCompositionReady()).isTrue();
            assertThat(body.isManifestReady()).isTrue();
            assertThat(body.getSceneCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("preview without composition returns safe state")
        void noComposition() {
            TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Test");
            var project = bootstrapService.bootstrap(req, "admin");
            var resp = controller.getCompositionPreview(project.getId());
            var body = resp.getBody().getData();
            assertThat(body.isCompositionReady()).isFalse();
            assertThat(body.isManifestReady()).isFalse();
            assertThat(body.getTimelineScenes()).isEmpty();
            assertThat(body.getReadinessStatus()).isEqualTo("BLOCKED");
        }

        @Test
        @DisplayName("timeline scenes match composition plan order")
        void sceneOrder() {
            var project = bootstrapToRenderReady();
            var resp = controller.getCompositionPreview(project.getId());
            var scenes = resp.getBody().getData().getTimelineScenes();
            for (int i = 0; i < scenes.size() - 1; i++) {
                assertThat(scenes.get(i).getSceneNo())
                        .isLessThan(scenes.get(i + 1).getSceneNo());
            }
        }
    }

    @Nested
    @DisplayName("C. Safety")
    class Safety {

        @Test
        @DisplayName("humanReviewRequired=true")
        void humanReview() {
            var project = bootstrapToRenderReady();
            var body = controller.getCompositionPreview(project.getId()).getBody().getData();
            assertThat(body.isHumanReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("autoApproved=false")
        void noAutoApproval() {
            var project = bootstrapToRenderReady();
            var body = controller.getCompositionPreview(project.getId()).getBody().getData();
            assertThat(body.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("copyrightReviewRequired=true")
        void copyrightReview() {
            var project = bootstrapToRenderReady();
            var body = controller.getCompositionPreview(project.getId()).getBody().getData();
            assertThat(body.isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("review flags include NOT_PUBLISH_READY")
        void notPublishReady() {
            var project = bootstrapToRenderReady();
            var body = controller.getCompositionPreview(project.getId()).getBody().getData();
            assertThat(body.getReviewFlags())
                    .contains("NOT_PUBLISH_READY", "NOT_EXPORT_READY");
        }

        @Test
        @DisplayName("narration summary is not lyrics")
        void noLyrics() {
            var project = bootstrapToRenderReady();
            var body = controller.getCompositionPreview(project.getId()).getBody().getData();
            for (var scene : body.getTimelineScenes()) {
                assertThat(scene.getNarrationSummary())
                        .doesNotContain("lyrics", "şarkı sözleri");
            }
        }
    }

    @Nested
    @DisplayName("D. DTO Safety — Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawProviderResponse", "token", "jwt", "session",
                "credential", "password", "secret", "apiKey", "authorizationHeader",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "imageData", "binaryData",
                "lyrics", "videoData", "audioData", "renderedVideo", "videoFile",
                "downloadFile");

        @Test
        @DisplayName("TurkuCompositionPreviewResponse has no forbidden fields")
        void response() { assertNoForbidden(TurkuCompositionPreviewResponse.class); }

        @Test
        @DisplayName("TurkuCompositionPreviewScene has no forbidden fields")
        void scene() { assertNoForbidden(TurkuCompositionPreviewScene.class); }

        private void assertNoForbidden(Class<?> clazz) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).doesNotContain(f);
            }
        }
    }
}
