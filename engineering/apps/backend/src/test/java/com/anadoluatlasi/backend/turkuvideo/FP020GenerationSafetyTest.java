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
 * FP-020 Task 05: Admin UI Provider Mode / Generation Safety Visibility — backend tests.
 */
@DisplayName("FP-020 Task 05 — Generation Safety Visibility Backend")
class FP020GenerationSafetyTest {

    private TurkuVideoProjectRepository repository;
    private TurkuVideoProjectBootstrapService bootstrapService;
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
        var actorResolver = new AdminActorResolver();
        var compositionService = new TurkuVideoCompositionService(repository);
        var renderManifestService = new TurkuRenderManifestService(repository);
        controller = new TurkuVideoProjectController(
                bootstrapService, planningService, assetService,
                draftService, imageBatchService, compositionService,
                renderManifestService, actorResolver);
    }

    private TurkuVideoProject createProject() {
        TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("Çanakkale Türküsü");
        req.setRegion("Çanakkale");
        req.setMood("Kahramanlık");
        return bootstrapService.bootstrap(req, "test-admin");
    }

    // ── A. Endpoint Inventory ───────────────────────────────

    @Nested
    @DisplayName("A. Endpoint Inventory (39 = 23 POST + 16 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("total 39 endpoints: 23 POST + 16 GET")
        void total() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("POST").isEqualTo(23);
            assertThat(get).as("GET").isEqualTo(16);
            assertThat(post + get).as("total").isEqualTo(39);
        }

        @Test
        @DisplayName("TurkuVideoProjectController has 14 POST, 4 GET")
        void turkuController() {
            long post = countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).isEqualTo(14);
            assertThat(get).isEqualTo(4);
        }

        @Test
        @DisplayName("generation-safety GET endpoint exists")
        void safetyEndpoint() {
            var methods = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .map(java.lang.reflect.Method::getName)
                    .collect(Collectors.toSet());
            assertThat(methods).contains("getGenerationSafety");
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── B. Generation Safety Response ───────────────────────

    @Nested
    @DisplayName("B. Generation Safety Response")
    class SafetyResponseTests {

        @Test
        @DisplayName("returns safety for DRAFT_CREATED project")
        void draftProject() {
            var project = createProject();
            var resp = controller.getGenerationSafety(project.getId());
            var body = resp.getBody().getData();
            assertThat(body.getProjectId()).isEqualTo(project.getId());
            assertThat(body.getProviderMode()).isEqualTo(TurkuProviderMode.DETERMINISTIC_STUB);
            assertThat(body.getCurrentStage()).isEqualTo(TurkuVideoWorkflowStage.DRAFT_CREATED);
            assertThat(body.isGenerationAvailable()).isTrue();
            assertThat(body.isHumanReviewRequired()).isTrue();
            assertThat(body.isAutoApproved()).isFalse();
            assertThat(body.isCopyrightReviewRequired()).isTrue();
            assertThat(body.isLyricsIncluded()).isFalse();
        }

        @Test
        @DisplayName("canGenerateStory true at DRAFT_CREATED")
        void canStory() {
            var project = createProject();
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.isCanGenerateStory()).isTrue();
            assertThat(body.isCanGeneratePlanningBundle()).isTrue();
            assertThat(body.isCanGenerateScenario()).isFalse();
            assertThat(body.isCanGenerateStoryboard()).isFalse();
            assertThat(body.isCanRunImageBatch()).isFalse();
        }

        @Test
        @DisplayName("review flags present")
        void reviewFlags() {
            var project = createProject();
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.getReviewFlags()).contains("HUMAN_REVIEW_REQUIRED",
                    "COPYRIGHT_REVIEW_REQUIRED", "LYRICS_NOT_INCLUDED");
        }
    }

    // ── C. Blocked Mode Behavior ────────────────────────────

    @Nested
    @DisplayName("C. Blocked Mode Behavior")
    class BlockedModeTests {

        @Test
        @DisplayName("PROVIDER_DISABLED shows blocked")
        void disabled() {
            var project = createProject();
            project.setProviderMode(TurkuProviderMode.PROVIDER_DISABLED);
            repository.save(project);
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.isGenerationAvailable()).isFalse();
            assertThat(body.getBlockedReasonSummary()).isNotNull();
            assertThat(body.getSafeFailureCode()).isEqualTo("PROVIDER_DISABLED");
            assertThat(body.isCanGenerateStory()).isFalse();
            assertThat(body.isCanGeneratePlanningBundle()).isFalse();
        }

        @Test
        @DisplayName("PROVIDER_FAILED_SAFE shows blocked")
        void failedSafe() {
            var project = createProject();
            project.setProviderMode(TurkuProviderMode.PROVIDER_FAILED_SAFE);
            repository.save(project);
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.isGenerationAvailable()).isFalse();
            assertThat(body.getSafeFailureCode()).isEqualTo("PROVIDER_FAILED_SAFE");
        }

        @Test
        @DisplayName("HUMAN_REVIEW_REQUIRED shows blocked")
        void reviewRequired() {
            var project = createProject();
            project.setProviderMode(TurkuProviderMode.HUMAN_REVIEW_REQUIRED);
            repository.save(project);
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.isGenerationAvailable()).isFalse();
            assertThat(body.getSafeFailureCode()).isEqualTo("HUMAN_REVIEW_REQUIRED");
        }

        @Test
        @DisplayName("blocked modes still have safety flags")
        void blockedFlags() {
            var project = createProject();
            project.setProviderMode(TurkuProviderMode.PROVIDER_DISABLED);
            repository.save(project);
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.isHumanReviewRequired()).isTrue();
            assertThat(body.isAutoApproved()).isFalse();
            assertThat(body.getSafeWarnings()).isNotEmpty();
        }
    }

    // ── D. Response DTO Safety ───────────────────────────────

    @Nested
    @DisplayName("D. Response DTO Safety")
    class DtoSafetyTests {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawProviderResponse", "rawRequest", "rawResponse",
                "token", "jwt", "session", "credential", "password", "secret",
                "apiKey", "authorizationHeader", "publicUrl", "signedUrl",
                "cdnUrl", "storageKey", "base64", "lyrics", "imageData",
                "binaryData", "downloadUrl", "exportUrl", "publishUrl",
                "videoData", "providerEndpoint", "rawPrompt");

        @Test
        @DisplayName("TurkuGenerationSafetyResponse has no forbidden fields")
        void noForbidden() {
            Set<String> fields = Arrays.stream(TurkuGenerationSafetyResponse.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("field '%s'", f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("autoApproved is always false")
        void autoApproved() {
            var project = createProject();
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("humanReviewRequired is always true")
        void humanReview() {
            var project = createProject();
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.isHumanReviewRequired()).isTrue();
        }
    }

    // ── E. Safety Invariants ────────────────────────────────

    @Nested
    @DisplayName("E. Safety Invariants")
    class SafetyInvariants {

        @Test
        @DisplayName("no publish/export methods on controller")
        void noPublish() {
            var methods = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName)
                    .collect(Collectors.toSet());
            assertThat(methods).doesNotContain("publish", "export", "download",
                    "publishVideo", "exportVideo", "downloadVideo",
                    "renderVideo", "finalizeVideo");
        }

        @Test
        @DisplayName("lyricsIncluded always false")
        void noLyrics() {
            var project = createProject();
            var body = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(body.isLyricsIncluded()).isFalse();
        }
    }
}
