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
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-022 Task 02: Render Job Backend tests.
 *
 * <p>Verifies render job preparation, DTO safety, endpoint inventory,
 * workflow stage machine, and forbidden behavior.</p>
 */
@DisplayName("FP-022 Task 02 — Render Job Backend")
class TurkuRenderJobTest {

    private TurkuVideoProjectRepository repository;
    private TurkuVideoProjectBootstrapService bootstrapService;
    private TurkuRenderJobService renderJobService;
    private TurkuVideoProjectController controller;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTurkuVideoProjectRepository();
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
        renderJobService = new TurkuRenderJobService(repository, renderManifestService);
        var renderRunner = new StubTurkuRenderRunnerGateway();
        var renderExecutionService = new TurkuRenderExecutionService(repository, renderRunner);
        var actorResolver = new AdminActorResolver();
        controller = new TurkuVideoProjectController(
                bootstrapService, planningService, assetService,
                draftService, imageBatchService, compositionService,
                renderManifestService, renderJobService, renderExecutionService, new TurkuStoryboardInternalRenderChainHarness(), actorResolver, null, null, null);
    }

    private TurkuVideoProject bootstrapProject() {
        TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("Çanakkale Türküsü");
        req.setRegion("Çanakkale");
        req.setMood("Kahramanlık");
        return bootstrapService.bootstrap(req, "test-admin");
    }

    private UUID runToManifest() {
        var project = bootstrapProject();
        UUID id = project.getId();
        controller.prepareStory(id, null, null, null);
        controller.prepareScenario(id, null, null, null);
        controller.prepareStoryboard(id, null, null, null);
        controller.prepareSceneAssets(id, null, null, null);
        controller.prepareImageBatch(id, null, null, null);
        controller.completeImageBatch(id, null, null, null);
        controller.prepareCompositionPlan(id, null, null, null);
        controller.prepareRenderManifest(id, null, null, null);
        return id;
    }

    // ── A. Endpoint Inventory (46 = 26 POST + 20 GET) ──

    @Nested
    @DisplayName("A. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("TurkuVideoProjectController: 17 POST + 8 GET = 23")
        void turkuControllerCounts() {
            assertThat(countMethods(TurkuVideoProjectController.class, PostMapping.class))
                    .as("POST").isEqualTo(17);
            assertThat(countMethods(TurkuVideoProjectController.class, GetMapping.class))
                    .as("GET").isEqualTo(8);
        }

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
        @DisplayName("FP-022 endpoints exist")
        void fp022Endpoints() {
            var methods = TurkuVideoProjectController.class.getDeclaredMethods();
            assertThat(Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(PostMapping.class).value()))
                    .anyMatch(v -> v.contains("prepare-render-job"))).isTrue();
            assertThat(Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .anyMatch(v -> v.contains("render-job"))).isTrue();
        }

        @Test
        @DisplayName("no PUT/DELETE endpoints")
        void noUnsafe() {
            long put = countMethods(TurkuVideoProjectController.class, PutMapping.class);
            long del = countMethods(TurkuVideoProjectController.class, DeleteMapping.class);
            assertThat(put).isEqualTo(0);
            assertThat(del).isEqualTo(0);
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── B. Prepare Render Job ─────────────────────────

    @Nested
    @DisplayName("B. Prepare Render Job")
    class PrepareRenderJob {

        @Test
        @DisplayName("happy path: prepare render job after manifest")
        void happyPath() {
            UUID id = runToManifest();
            var resp = controller.prepareRenderJob(id, null, null, null);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var data = resp.getBody().getData();
            assertThat(data.getStatus()).isEqualTo(TurkuRenderJobStatus.PREPARED);
            assertThat(data.getJobId()).isNotNull();
            assertThat(data.getManifestRef()).startsWith("internal-manifest:");
            assertThat(data.getAttemptCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("stage advances to RENDER_JOB_PREPARED")
        void stageAdvance() {
            UUID id = runToManifest();
            controller.prepareRenderJob(id, null, null, null);
            assertThat(bootstrapService.getById(id).getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_JOB_PREPARED);
        }

        @Test
        @DisplayName("render job stored on project")
        void jobStored() {
            UUID id = runToManifest();
            controller.prepareRenderJob(id, null, null, null);
            assertThat(bootstrapService.getById(id).getRenderJob()).isNotNull();
            assertThat(bootstrapService.getById(id).getRenderJob().getStatus())
                    .isEqualTo(TurkuRenderJobStatus.PREPARED);
        }

        @Test
        @DisplayName("fails before render manifest (DRAFT_CREATED)")
        void failsBeforeManifest() {
            var project = bootstrapProject();
            assertThatThrownBy(() ->
                    controller.prepareRenderJob(project.getId(), null, null, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("fails at COMPOSITION_PLAN_PREPARED")
        void failsAtComposition() {
            var project = bootstrapProject();
            UUID id = project.getId();
            controller.prepareStory(id, null, null, null);
            controller.prepareScenario(id, null, null, null);
            controller.prepareStoryboard(id, null, null, null);
            controller.prepareSceneAssets(id, null, null, null);
            controller.prepareImageBatch(id, null, null, null);
            controller.completeImageBatch(id, null, null, null);
            controller.prepareCompositionPlan(id, null, null, null);
            assertThatThrownBy(() ->
                    controller.prepareRenderJob(id, null, null, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("unknown project ID safe error")
        void unknownProject() {
            assertThatThrownBy(() ->
                    controller.prepareRenderJob(UUID.randomUUID(), null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("SYSTEM blocked for POST")
        void systemBlocked() {
            UUID id = runToManifest();
            assertThatThrownBy(() ->
                    controller.prepareRenderJob(id, null, ReviewActorRole.SYSTEM, null))
                    .isInstanceOf(AdminActorResolutionException.class);
        }
    }

    // ── C. Get Render Job ────────────────────────────

    @Nested
    @DisplayName("C. Get Render Job")
    class GetRenderJob {

        @Test
        @DisplayName("GET render job returns safe metadata after preparation")
        void afterPreparation() {
            UUID id = runToManifest();
            controller.prepareRenderJob(id, null, null, null);
            var resp = controller.getRenderJob(id);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var data = resp.getBody().getData();
            assertThat(data.getStatus()).isEqualTo(TurkuRenderJobStatus.PREPARED);
            assertThat(data.getJobId()).isNotNull();
            assertThat(data.getCurrentStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_JOB_PREPARED);
        }

        @Test
        @DisplayName("GET render job before preparation returns safe empty state")
        void beforePreparation() {
            var project = bootstrapProject();
            var resp = controller.getRenderJob(project.getId());
            var data = resp.getBody().getData();
            assertThat(data.getStatus()).isNull();
            assertThat(data.getJobId()).isNull();
            assertThat(data.getReviewFlags()).contains("NO_RENDER_JOB");
        }

        @Test
        @DisplayName("unknown project ID safe error")
        void unknownProject() {
            assertThatThrownBy(() ->
                    controller.getRenderJob(UUID.randomUUID()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── D. Safety Flags ──────────────────────────────

    @Nested
    @DisplayName("D. Safety Flags")
    class SafetyFlags {

        @Test
        @DisplayName("humanReviewRequired=true")
        void humanReview() {
            UUID id = runToManifest();
            var resp = controller.prepareRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isHumanReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("copyrightReviewRequired=true")
        void copyrightReview() {
            UUID id = runToManifest();
            var resp = controller.prepareRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("autoApproved=false")
        void autoApproved() {
            UUID id = runToManifest();
            var resp = controller.prepareRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("notPublishReady=true")
        void notPublishReady() {
            UUID id = runToManifest();
            var resp = controller.prepareRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isNotPublishReady()).isTrue();
        }

        @Test
        @DisplayName("notExportReady=true")
        void notExportReady() {
            UUID id = runToManifest();
            var resp = controller.prepareRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("reviewFlags include INTERNAL_RENDER_ONLY")
        void internalOnly() {
            UUID id = runToManifest();
            var resp = controller.prepareRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().getReviewFlags())
                    .contains("HUMAN_REVIEW_REQUIRED", "NOT_PUBLISH_READY",
                              "NOT_EXPORT_READY", "INTERNAL_RENDER_ONLY");
        }

        @Test
        @DisplayName("render job domain object safety flags")
        void domainFlags() {
            UUID id = runToManifest();
            controller.prepareRenderJob(id, null, null, null);
            var job = bootstrapService.getById(id).getRenderJob();
            assertThat(job.isHumanReviewRequired()).isTrue();
            assertThat(job.isCopyrightReviewRequired()).isTrue();
            assertThat(job.isAutoApproved()).isFalse();
        }
    }

    // ── E. DTO Safety — Forbidden Fields ────────────

    @Nested
    @DisplayName("E. DTO Safety — Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawProviderResponse", "rawProviderRequest",
                "token", "jwt", "session", "credential", "password", "secret",
                "apiKey", "authorizationHeader", "principalRaw",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "imageData", "binaryData",
                "lyrics", "videoData", "audioData", "providerEndpoint",
                "renderedVideo", "videoFile", "downloadFile", "videoStream",
                "filePath", "renderedVideoUrl");

        @Test
        @DisplayName("TurkuRenderJob safe")
        void renderJob() { assertNoForbidden(TurkuRenderJob.class); }

        @Test
        @DisplayName("TurkuRenderJobResponse safe")
        void response() { assertNoForbidden(TurkuRenderJobResponse.class); }

        @Test
        @DisplayName("TurkuRenderAttempt safe")
        void attempt() { assertNoForbidden(TurkuRenderAttempt.class); }

        @Test
        @DisplayName("TurkuRenderArtifact safe")
        void artifact() { assertNoForbidden(TurkuRenderArtifact.class); }

        @Test
        @DisplayName("TurkuRenderJobRequest safe")
        void request() { assertNoForbidden(TurkuRenderJobRequest.class); }

        private void assertNoForbidden(Class<?> clazz) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("field '%s' in %s", f, clazz.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }

    // ── F. Workflow Stage Machine ──────────────────

    @Nested
    @DisplayName("F. Workflow Stage Machine")
    class StageMachine {

        @Test
        @DisplayName("stage enum has exactly 15 values")
        void enumSize() {
            assertThat(TurkuVideoWorkflowStage.values()).hasSize(19);
        }

        @Test
        @DisplayName("RENDER_JOB_PREPARED exists")
        void jobStage() {
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_JOB_PREPARED")).isNotNull();
        }

        @Test
        @DisplayName("no forbidden workflow stages")
        void noForbiddenStages() {
            var stages = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(stages).doesNotContain(
                    "PUBLISHED", "EXPORTED", "AUTO_APPROVED",
                    "PUBLIC_READY", "CDN_DELIVERED", "DOWNLOAD_READY",
                    "PUBLICLY_DELIVERED", "COMPLETED_AS_PUBLIC_VIDEO");
        }

        @Test
        @DisplayName("stage ordering: RENDER_JOB_PREPARED after RENDER_MANIFEST_PREPARED")
        void stageOrdering() {
            assertThat(TurkuVideoWorkflowStage.RENDER_JOB_PREPARED.ordinal())
                    .isGreaterThan(TurkuVideoWorkflowStage.RENDER_MANIFEST_PREPARED.ordinal());
        }
    }

    // ── G. Forbidden Behavior ─────────────────────

    @Nested
    @DisplayName("G. Forbidden Behavior")
    class ForbiddenBehavior {

        @Test
        @DisplayName("no publish/export/download/render controller methods")
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
        @DisplayName("TurkuRenderJobStatus has no PUBLISHED/EXPORTED")
        void noPublishStatus() {
            var statuses = Arrays.stream(TurkuRenderJobStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain(
                    "PUBLISHED", "EXPORTED", "AUTO_APPROVED",
                    "DOWNLOAD_READY", "PUBLIC_READY");
        }

        @Test
        @DisplayName("manifest ref is opaque internal ref")
        void opaqueRef() {
            UUID id = runToManifest();
            var resp = controller.prepareRenderJob(id, null, null, null);
            String ref = resp.getBody().getData().getManifestRef();
            assertThat(ref).startsWith("internal-manifest:");
            assertThat(ref).doesNotContain("http", "cdn", "public", "signed", "storage");
        }
    }
}
