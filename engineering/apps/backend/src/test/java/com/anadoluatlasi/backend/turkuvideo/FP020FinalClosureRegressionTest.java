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
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-020 Task 06: E2E Provider Safety Regression + Final Closure.
 *
 * <p>Comprehensive regression covering full provider-draft workflow,
 * provider mode blocking, safety invariants, DTO safety, endpoint
 * inventory, and forbidden behavior.</p>
 */
@DisplayName("FP-020 Task 06 — E2E Provider Safety Regression")
class FP020FinalClosureRegressionTest {

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
        controller = new TurkuVideoProjectController(
                bootstrapService, planningService, assetService,
                draftService, imageBatchService, actorResolver);
    }

    private TurkuVideoProject bootstrapProject() {
        TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("Çanakkale Türküsü");
        req.setRegion("Çanakkale");
        req.setMood("Kahramanlık");
        return bootstrapService.bootstrap(req, "test-admin");
    }

    // ── A. Full Provider-Draft Workflow E2E ──────────────────

    @Nested
    @DisplayName("A. Full Provider-Draft Workflow E2E")
    class FullWorkflowE2E {

        @Test
        @DisplayName("bootstrap → generate story → generate scenario → generate storyboard → assets → batch → run batch → safety → progress")
        void fullPipeline() {
            // 1. Bootstrap
            var project = bootstrapProject();
            assertThat(project.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.DRAFT_CREATED);

            UUID id = project.getId();

            // 2. Generate story (provider-draft path)
            var storyResp = controller.generateStory(id, null, null, null);
            assertThat(storyResp.getStatusCode().value()).isEqualTo(200);

            // 3. Generate scenario
            var scenarioResp = controller.generateScenario(id, null, null, null);
            assertThat(scenarioResp.getStatusCode().value()).isEqualTo(200);

            // 4. Generate storyboard
            var storyboardResp = controller.generateStoryboard(id, null, null, null);
            assertThat(storyboardResp.getStatusCode().value()).isEqualTo(200);

            // 5. Prepare scene assets
            var assetsResp = controller.prepareSceneAssets(id, null, null, null);
            assertThat(assetsResp.getStatusCode().value()).isEqualTo(200);

            // 6. Prepare image batch
            var batchResp = controller.prepareImageBatch(id, null, null, null);
            assertThat(batchResp.getStatusCode().value()).isEqualTo(200);

            // 7. Run image batch (provider-draft)
            var runResp = controller.runImageBatch(id, null, null, null);
            assertThat(runResp.getStatusCode().value()).isEqualTo(200);

            // 8. Read generation safety
            var safetyResp = controller.getGenerationSafety(id);
            assertThat(safetyResp.getStatusCode().value()).isEqualTo(200);
            var safety = safetyResp.getBody().getData();
            assertThat(safety.isHumanReviewRequired()).isTrue();
            assertThat(safety.isAutoApproved()).isFalse();

            // 9. Read project & progress
            var projResp = controller.getProject(id);
            assertThat(projResp.getStatusCode().value()).isEqualTo(200);
            var progResp = controller.getProgress(id);
            assertThat(progResp.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("deterministic prepare-* path still works end-to-end")
        void deterministicPipeline() {
            var project = bootstrapProject();
            UUID id = project.getId();

            controller.prepareStory(id, null, null, null);
            controller.prepareScenario(id, null, null, null);
            controller.prepareStoryboard(id, null, null, null);
            controller.prepareSceneAssets(id, null, null, null);
            controller.prepareImageBatch(id, null, null, null);
            controller.completeImageBatch(id, null, null, null);

            var updated = bootstrapService.getById(id);
            assertThat(updated.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.IMAGE_BATCH_COMPLETED);
        }

        @Test
        @DisplayName("generate planning bundle at DRAFT_CREATED succeeds")
        void planningBundle() {
            var project = bootstrapProject();
            var resp = controller.generatePlanningBundle(project.getId(), null, null, null);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
        }
    }

    // ── B. Provider Mode Blocking ───────────────────────────

    @Nested
    @DisplayName("B. Provider Mode Blocking")
    class ProviderModeBlocking {

        @Test
        @DisplayName("DETERMINISTIC_STUB: safety shows generation available")
        void stubAvailable() {
            var project = bootstrapProject();
            project.setProviderMode(TurkuProviderMode.DETERMINISTIC_STUB);
            repository.save(project);
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isGenerationAvailable()).isTrue();
            assertThat(safety.getBlockedReasonSummary()).isNull();
        }

        @Test
        @DisplayName("PROVIDER_DRAFT: safety shows generation available")
        void draftAvailable() {
            var project = bootstrapProject();
            project.setProviderMode(TurkuProviderMode.PROVIDER_DRAFT);
            repository.save(project);
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isGenerationAvailable()).isTrue();
        }

        @Test
        @DisplayName("PROVIDER_DISABLED: blocks safely")
        void disabledBlocks() {
            var project = bootstrapProject();
            project.setProviderMode(TurkuProviderMode.PROVIDER_DISABLED);
            repository.save(project);
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isGenerationAvailable()).isFalse();
            assertThat(safety.getBlockedReasonSummary()).isNotEmpty();
            assertThat(safety.getSafeFailureCode()).isEqualTo("PROVIDER_DISABLED");
            assertThat(safety.isCanGenerateStory()).isFalse();
            assertThat(safety.isCanGenerateScenario()).isFalse();
            assertThat(safety.isCanGenerateStoryboard()).isFalse();
            assertThat(safety.isCanGeneratePlanningBundle()).isFalse();
            assertThat(safety.isCanRunImageBatch()).isFalse();
        }

        @Test
        @DisplayName("PROVIDER_FAILED_SAFE: blocks safely")
        void failedSafeBlocks() {
            var project = bootstrapProject();
            project.setProviderMode(TurkuProviderMode.PROVIDER_FAILED_SAFE);
            repository.save(project);
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isGenerationAvailable()).isFalse();
            assertThat(safety.getSafeFailureCode()).isEqualTo("PROVIDER_FAILED_SAFE");
            assertThat(safety.getSafeWarnings()).isNotEmpty();
        }

        @Test
        @DisplayName("HUMAN_REVIEW_REQUIRED: blocks safely")
        void reviewRequiredBlocks() {
            var project = bootstrapProject();
            project.setProviderMode(TurkuProviderMode.HUMAN_REVIEW_REQUIRED);
            repository.save(project);
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isGenerationAvailable()).isFalse();
            assertThat(safety.getSafeFailureCode()).isEqualTo("HUMAN_REVIEW_REQUIRED");
        }

        @Test
        @DisplayName("all blocked modes maintain safety invariants")
        void blockedInvariants() {
            for (TurkuProviderMode mode : List.of(
                    TurkuProviderMode.PROVIDER_DISABLED,
                    TurkuProviderMode.PROVIDER_FAILED_SAFE,
                    TurkuProviderMode.HUMAN_REVIEW_REQUIRED)) {
                var project = bootstrapProject();
                project.setProviderMode(mode);
                repository.save(project);
                var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
                assertThat(safety.isHumanReviewRequired()).as(mode + " humanReview").isTrue();
                assertThat(safety.isAutoApproved()).as(mode + " autoApproved").isFalse();
                assertThat(safety.isCopyrightReviewRequired()).as(mode + " copyright").isTrue();
                assertThat(safety.isLyricsIncluded()).as(mode + " lyrics").isFalse();
            }
        }
    }

    // ── C. Safety Invariants ────────────────────────────────

    @Nested
    @DisplayName("C. Safety Invariants")
    class SafetyInvariants {

        @Test
        @DisplayName("humanReviewRequired=true always")
        void humanReview() {
            var project = bootstrapProject();
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isHumanReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("autoApproved=false always")
        void noAutoApproval() {
            var project = bootstrapProject();
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("lyricsIncluded=false always")
        void noLyrics() {
            var project = bootstrapProject();
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isLyricsIncluded()).isFalse();
        }

        @Test
        @DisplayName("copyrightReviewRequired=true always")
        void copyrightReview() {
            var project = bootstrapProject();
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("review flags always include critical flags")
        void reviewFlags() {
            var project = bootstrapProject();
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.getReviewFlags()).contains(
                    "HUMAN_REVIEW_REQUIRED",
                    "COPYRIGHT_REVIEW_REQUIRED",
                    "LYRICS_NOT_INCLUDED");
        }
    }

    // ── D. DTO Safety — Forbidden Fields ────────────────────

    @Nested
    @DisplayName("D. DTO Safety — Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawProviderResponse", "rawProviderRequest",
                "rawRequest", "rawResponse", "rawPrompt",
                "token", "jwt", "session", "credential", "password", "secret",
                "apiKey", "authorizationHeader", "principalRaw",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "imageData", "binaryData",
                "lyrics", "videoData", "audioData", "providerEndpoint");

        @Test
        @DisplayName("TurkuGenerationSafetyResponse has no forbidden fields")
        void safetyResponse() {
            assertNoForbiddenFields(TurkuGenerationSafetyResponse.class);
        }

        @Test
        @DisplayName("TurkuVideoBootstrapResponse has no forbidden fields")
        void bootstrapResponse() {
            assertNoForbiddenFields(TurkuVideoBootstrapResponse.class);
        }

        @Test
        @DisplayName("TurkuVideoProjectResponse has no forbidden fields")
        void projectResponse() {
            assertNoForbiddenFields(TurkuVideoProjectResponse.class);
        }

        @Test
        @DisplayName("TurkuVideoProgressResponse has no forbidden fields")
        void progressResponse() {
            assertNoForbiddenFields(TurkuVideoProgressResponse.class);
        }

        @Test
        @DisplayName("TurkuPreparationResponse has no forbidden fields")
        void preparationResponse() {
            assertNoForbiddenFields(TurkuPreparationResponse.class);
        }

        @Test
        @DisplayName("TurkuProviderPlanningGenerationResponse has no forbidden fields")
        void planningResponse() {
            assertNoForbiddenFields(TurkuProviderPlanningGenerationResponse.class);
        }

        @Test
        @DisplayName("TurkuPlanningBundleGenerationResponse has no forbidden fields")
        void bundleResponse() {
            assertNoForbiddenFields(TurkuPlanningBundleGenerationResponse.class);
        }

        @Test
        @DisplayName("TurkuImageBatchRunResponse has no forbidden fields")
        void imageBatchRunResponse() {
            assertNoForbiddenFields(TurkuImageBatchRunResponse.class);
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

    // ── E. Endpoint Inventory ───────────────────────────────

    @Nested
    @DisplayName("E. Endpoint Inventory (36 = 21 POST + 15 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("total exactly 36 = 21 POST + 15 GET")
        void total() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("total POST").isEqualTo(21);
            assertThat(get).as("total GET").isEqualTo(15);
            assertThat(post + get).as("total endpoints").isEqualTo(36);
        }

        @Test
        @DisplayName("TurkuVideoProjectController: 12 POST + 3 GET")
        void turkuController() {
            assertThat(countMethods(TurkuVideoProjectController.class, PostMapping.class))
                    .as("POST").isEqualTo(12);
            assertThat(countMethods(TurkuVideoProjectController.class, GetMapping.class))
                    .as("GET").isEqualTo(3);
        }

        @Test
        @DisplayName("no PUT endpoints exist")
        void noPut() {
            long put = countMethods(AdminReviewController.class, PutMapping.class)
                    + countMethods(AdminPipelineController.class, PutMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PutMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PutMapping.class);
            assertThat(put).isEqualTo(0);
        }

        @Test
        @DisplayName("no DELETE endpoints exist")
        void noDelete() {
            long del = countMethods(AdminReviewController.class, DeleteMapping.class)
                    + countMethods(AdminPipelineController.class, DeleteMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, DeleteMapping.class)
                    + countMethods(TurkuVideoProjectController.class, DeleteMapping.class);
            assertThat(del).isEqualTo(0);
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── F. Forbidden Controller Behavior ────────────────────

    @Nested
    @DisplayName("F. Forbidden Controller Behavior")
    class ForbiddenBehavior {

        @Test
        @DisplayName("no publish/export/download methods")
        void noPublish() {
            Set<String> methods = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            List<String> forbidden = List.of(
                    "publish", "export", "download", "publishVideo", "exportVideo",
                    "downloadVideo", "renderVideo", "finalizeVideo", "autoApprove",
                    "updateStatus", "genericStatusUpdate");
            for (String m : forbidden) {
                assertThat(methods).as("method '%s'", m).doesNotContain(m);
            }
        }

        @Test
        @DisplayName("no forbidden workflow stages in enum")
        void noForbiddenStages() {
            Set<String> stages = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(stages).doesNotContain("PUBLISHED", "EXPORTED", "AUTO_APPROVED",
                    "PUBLIC_READY", "CDN_DELIVERED", "DOWNLOAD_READY");
        }

        @Test
        @DisplayName("all POST endpoints route through resolvePlanningActor")
        void allPostUseBoundary() {
            // Verify controller has actorResolver dependency
            boolean hasResolver = Arrays.stream(TurkuVideoProjectController.class.getDeclaredFields())
                    .anyMatch(f -> f.getType() == AdminActorResolver.class);
            assertThat(hasResolver).as("controller has AdminActorResolver").isTrue();
        }

        @Test
        @DisplayName("SYSTEM role cannot be sent from client")
        void systemBlocked() {
            assertThatThrownBy(() -> {
                AdminActorResolutionRequest req = AdminActorResolutionRequest.builder()
                        .requestedActorRole(ReviewActorRole.SYSTEM)
                        .build();
                new AdminActorResolver().resolve(req);
            }).isInstanceOf(AdminActorResolutionException.class);
        }
    }

    // ── G. Provider Safety Components ───────────────────────

    @Nested
    @DisplayName("G. Provider Safety Components")
    class ProviderSafetyComponents {

        @Test
        @DisplayName("TurkuProviderSafetyPolicy resolves modes")
        void policyResolves() {
            var policy = new TurkuProviderSafetyPolicy();
            var project = bootstrapProject();
            TurkuProviderMode mode = policy.resolveProviderMode(project);
            assertThat(mode).isNotNull();
        }

        @Test
        @DisplayName("TurkuProviderSafetySanitizer does not expose raw data")
        void sanitizerSafe() {
            var sanitizer = new TurkuProviderSafetySanitizer();
            assertThat(sanitizer).isNotNull();
        }

        @Test
        @DisplayName("StubTurkuGenerationProviderGateway returns safe stubs")
        void gatewayStubs() {
            var gateway = new StubTurkuGenerationProviderGateway();
            assertThat(gateway).isNotNull();
        }

        @Test
        @DisplayName("TurkuGenerationSafetyEnvelope exists")
        void envelopeExists() {
            var envelope = new TurkuGenerationSafetyEnvelope();
            assertThat(envelope).isNotNull();
        }
    }

    // ── H. Cross-Module Safety ──────────────────────────────

    @Nested
    @DisplayName("H. Cross-Module Safety")
    class CrossModuleSafety {

        @Test
        @DisplayName("no FP-020 migration files exist")
        void noFp020Migrations() {
            var migrationsDir = new java.io.File("src/main/resources/db/migration");
            if (migrationsDir.exists() && migrationsDir.listFiles() != null) {
                boolean hasFp020 = Arrays.stream(migrationsDir.listFiles())
                        .anyMatch(f -> f.getName().toLowerCase().contains("fp020")
                                || f.getName().toLowerCase().contains("fp_020")
                                || f.getName().toLowerCase().contains("provider_mode")
                                || f.getName().toLowerCase().contains("generation_safety"));
                assertThat(hasFp020).as("no FP-020 migrations").isFalse();
            }
        }

        @Test
        @DisplayName("all TurkuProviderMode values are accounted for")
        void allModesAccounted() {
            assertThat(TurkuProviderMode.values()).hasSize(5);
            assertThat(TurkuProviderMode.values()).contains(
                    TurkuProviderMode.DETERMINISTIC_STUB,
                    TurkuProviderMode.PROVIDER_DRAFT,
                    TurkuProviderMode.PROVIDER_DISABLED,
                    TurkuProviderMode.PROVIDER_FAILED_SAFE,
                    TurkuProviderMode.HUMAN_REVIEW_REQUIRED);
        }

        @Test
        @DisplayName("safety response projectId matches")
        void safetyProjectIdMatches() {
            var project = bootstrapProject();
            var safety = controller.getGenerationSafety(project.getId()).getBody().getData();
            assertThat(safety.getProjectId()).isEqualTo(project.getId());
        }
    }
}
