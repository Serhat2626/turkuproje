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
 * FP-022 Task 03: Render Execution Safety + Stub Runner Backend tests.
 */
@DisplayName("FP-022 Task 03 — Render Execution Safety + Stub Runner")
class TurkuRenderExecutionTest {

    private TurkuVideoProjectRepository repository;
    private TurkuVideoProjectBootstrapService bootstrapService;
    private TurkuRenderExecutionService executionService;
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
        var compositionService = new TurkuVideoCompositionService(repository);
        var renderManifestService = new TurkuRenderManifestService(repository);
        var renderJobService = new TurkuRenderJobService(repository, renderManifestService);
        var renderRunner = new StubTurkuRenderRunnerGateway();
        executionService = new TurkuRenderExecutionService(repository, renderRunner);
        var actorResolver = new AdminActorResolver();
        controller = new TurkuVideoProjectController(
                bootstrapService, planningService, assetService,
                draftService, imageBatchService, compositionService,
                renderManifestService, renderJobService, executionService, new TurkuStoryboardInternalRenderChainHarness(), actorResolver);
    }

    private TurkuVideoProject bootstrapProject() {
        TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("Çanakkale Türküsü");
        req.setRegion("Çanakkale");
        req.setMood("Kahramanlık");
        return bootstrapService.bootstrap(req, "test-admin");
    }

    private UUID runToRenderJobPrepared() {
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
        controller.prepareRenderJob(id, null, null, null);
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
        @DisplayName("FP-022 Task 03 endpoints exist")
        void fp022Endpoints() {
            var methods = TurkuVideoProjectController.class.getDeclaredMethods();
            assertThat(Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(PostMapping.class).value()))
                    .anyMatch(v -> v.contains("run-render-job"))).isTrue();
            assertThat(Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .anyMatch(v -> v.contains("render-execution-safety"))).isTrue();
        }

        @Test
        @DisplayName("no PUT/DELETE endpoints")
        void noUnsafe() {
            assertThat(countMethods(TurkuVideoProjectController.class, PutMapping.class)).isEqualTo(0);
            assertThat(countMethods(TurkuVideoProjectController.class, DeleteMapping.class)).isEqualTo(0);
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── B. Run Render Job ─────────────────────────

    @Nested
    @DisplayName("B. Run Render Job")
    class RunRenderJob {

        @Test
        @DisplayName("happy path: run render job after RENDER_JOB_PREPARED")
        void happyPath() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.runRenderJob(id, null, null, null);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var data = resp.getBody().getData();
            assertThat(data.getStatus()).isEqualTo(TurkuRenderJobStatus.COMPLETED_INTERNAL);
            assertThat(data.getAttemptId()).isNotNull();
            assertThat(data.getSafeTraceRef()).startsWith("stub-trace:");
            assertThat(data.getArtifactRef()).startsWith("internal-stub-artifact:");
        }

        @Test
        @DisplayName("stage advances to RENDER_JOB_COMPLETED_INTERNAL")
        void stageAdvance() {
            UUID id = runToRenderJobPrepared();
            controller.runRenderJob(id, null, null, null);
            assertThat(bootstrapService.getById(id).getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_JOB_COMPLETED_INTERNAL);
        }

        @Test
        @DisplayName("render job has attempt after execution")
        void attemptStored() {
            UUID id = runToRenderJobPrepared();
            controller.runRenderJob(id, null, null, null);
            var job = bootstrapService.getById(id).getRenderJob();
            assertThat(job.getAttempts()).hasSize(1);
            assertThat(job.getAttempts().get(0).getStatus())
                    .isEqualTo(TurkuRenderJobStatus.COMPLETED_INTERNAL);
        }

        @Test
        @DisplayName("render artifact stored on job")
        void artifactStored() {
            UUID id = runToRenderJobPrepared();
            controller.runRenderJob(id, null, null, null);
            var job = bootstrapService.getById(id).getRenderJob();
            assertThat(job.getArtifact()).isNotNull();
            assertThat(job.getArtifact().getArtifactRef()).startsWith("internal-stub-artifact:");
            assertThat(job.getArtifact().isHumanReviewRequired()).isTrue();
            assertThat(job.getArtifact().isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("fails before RENDER_JOB_PREPARED (DRAFT_CREATED)")
        void failsBeforePrepared() {
            var project = bootstrapProject();
            assertThatThrownBy(() ->
                    controller.runRenderJob(project.getId(), null, null, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("fails at RENDER_MANIFEST_PREPARED (job not prepared)")
        void failsAtManifest() {
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
            assertThatThrownBy(() ->
                    controller.runRenderJob(id, null, null, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("unknown project ID safe error")
        void unknownProject() {
            assertThatThrownBy(() ->
                    controller.runRenderJob(UUID.randomUUID(), null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("SYSTEM blocked for POST")
        void systemBlocked() {
            UUID id = runToRenderJobPrepared();
            assertThatThrownBy(() ->
                    controller.runRenderJob(id, null, ReviewActorRole.SYSTEM, null))
                    .isInstanceOf(AdminActorResolutionException.class);
        }
    }

    // ── C. Render Execution Safety GET ────────────────

    @Nested
    @DisplayName("C. Render Execution Safety GET")
    class RenderExecutionSafety {

        @Test
        @DisplayName("returns safety status before execution")
        void beforeExecution() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.getRenderExecutionSafety(id);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            var data = resp.getBody().getData();
            var envelope = data.getSafetyEnvelope();
            assertThat(envelope.isHumanReviewRequired()).isTrue();
            assertThat(envelope.isCopyrightReviewRequired()).isTrue();
            assertThat(envelope.isAutoApproved()).isFalse();
            assertThat(envelope.isLyricsIncluded()).isFalse();
            assertThat(envelope.isCopyrightClearanceVerified()).isFalse();
        }

        @Test
        @DisplayName("returns safety status after execution")
        void afterExecution() {
            UUID id = runToRenderJobPrepared();
            controller.runRenderJob(id, null, null, null);
            var resp = controller.getRenderExecutionSafety(id);
            var data = resp.getBody().getData();
            assertThat(data.getJobStatus()).isEqualTo(TurkuRenderJobStatus.COMPLETED_INTERNAL);
            assertThat(data.getSafetyEnvelope().getRenderMode()).isEqualTo("DETERMINISTIC_STUB");
        }

        @Test
        @DisplayName("safety envelope contains proper flags")
        void flags() {
            var project = bootstrapProject();
            var resp = controller.getRenderExecutionSafety(project.getId());
            var envelope = resp.getBody().getData().getSafetyEnvelope();
            assertThat(envelope.getSafetyFlags()).contains("NO_REAL_RENDER", "NO_BINARY_OUTPUT",
                    "NO_PUBLIC_DELIVERY", "NO_EXPORT");
            assertThat(envelope.getReviewFlags()).contains("HUMAN_REVIEW_REQUIRED",
                    "NOT_PUBLISH_READY", "NOT_EXPORT_READY");
        }

        @Test
        @DisplayName("unknown project ID safe error")
        void unknownProject() {
            assertThatThrownBy(() ->
                    controller.getRenderExecutionSafety(UUID.randomUUID()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── D. Safety Flags ──────────────────────────────

    @Nested
    @DisplayName("D. Safety Flags")
    class SafetyFlags {

        @Test
        @DisplayName("humanReviewRequired=true on execution response")
        void humanReview() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.runRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isHumanReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("copyrightReviewRequired=true on execution response")
        void copyrightReview() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.runRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("autoApproved=false on execution response")
        void autoApproved() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.runRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("notPublishReady=true on execution response")
        void notPublishReady() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.runRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isNotPublishReady()).isTrue();
        }

        @Test
        @DisplayName("notExportReady=true on execution response")
        void notExportReady() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.runRenderJob(id, null, null, null);
            assertThat(resp.getBody().getData().isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("safety envelope on render artifact")
        void artifactSafety() {
            UUID id = runToRenderJobPrepared();
            controller.runRenderJob(id, null, null, null);
            var artifact = bootstrapService.getById(id).getRenderJob().getArtifact();
            assertThat(artifact.isHumanReviewRequired()).isTrue();
            assertThat(artifact.isAutoApproved()).isFalse();
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
                "filePath", "renderedVideoUrl", "commandLine", "ffmpegCommand");

        @Test
        @DisplayName("TurkuRenderExecutionResponse safe")
        void response() { assertNoForbidden(TurkuRenderExecutionResponse.class); }

        @Test
        @DisplayName("TurkuRenderExecutionSafetyResponse safe")
        void safetyResponse() { assertNoForbidden(TurkuRenderExecutionSafetyResponse.class); }

        @Test
        @DisplayName("TurkuRenderExecutionSafetyEnvelope safe")
        void envelope() { assertNoForbidden(TurkuRenderExecutionSafetyEnvelope.class); }

        @Test
        @DisplayName("TurkuRenderExecutionResult safe")
        void result() { assertNoForbidden(TurkuRenderExecutionResult.class); }

        @Test
        @DisplayName("TurkuRenderExecutionPlan safe")
        void plan() { assertNoForbidden(TurkuRenderExecutionPlan.class); }

        @Test
        @DisplayName("TurkuRenderExecutionRequest safe")
        void request() { assertNoForbidden(TurkuRenderExecutionRequest.class); }

        @Test
        @DisplayName("StubTurkuRenderRunnerGateway safe")
        void stubRunner() { assertNoForbidden(StubTurkuRenderRunnerGateway.class); }

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
        @DisplayName("stage enum has exactly 19 values")
        void enumSize() {
            assertThat(TurkuVideoWorkflowStage.values()).hasSize(19);
        }

        @Test
        @DisplayName("new render execution stages exist")
        void executionStages() {
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_JOB_RUNNING")).isNotNull();
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_JOB_COMPLETED_INTERNAL")).isNotNull();
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_JOB_FAILED_SAFE")).isNotNull();
            assertThat(TurkuVideoWorkflowStage.valueOf("INTERNAL_RENDER_ARTIFACT_READY")).isNotNull();
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
        @DisplayName("stage ordering correct")
        void stageOrdering() {
            assertThat(TurkuVideoWorkflowStage.RENDER_JOB_RUNNING.ordinal())
                    .isGreaterThan(TurkuVideoWorkflowStage.RENDER_JOB_PREPARED.ordinal());
            assertThat(TurkuVideoWorkflowStage.RENDER_JOB_COMPLETED_INTERNAL.ordinal())
                    .isGreaterThan(TurkuVideoWorkflowStage.RENDER_JOB_RUNNING.ordinal());
            assertThat(TurkuVideoWorkflowStage.INTERNAL_RENDER_ARTIFACT_READY.ordinal())
                    .isGreaterThan(TurkuVideoWorkflowStage.RENDER_JOB_COMPLETED_INTERNAL.ordinal());
        }
    }

    // ── G. Forbidden Behavior ─────────────────────

    @Nested
    @DisplayName("G. Forbidden Behavior")
    class ForbiddenBehavior {

        @Test
        @DisplayName("no publish/export/download controller methods")
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
        @DisplayName("stub runner produces no real file")
        void stubNoFile() {
            var runner = new StubTurkuRenderRunnerGateway();
            var plan = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 3, 120.0, "1920x1080", 30, "mp4", 60.0,
                    "deterministic-stub", "DETERMINISTIC_STUB");
            var result = runner.execute(plan);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getArtifact()).isNotNull();
            assertThat(result.getArtifact().getArtifactRef()).startsWith("internal-stub-artifact:");
            assertThat(result.getArtifact().getArtifactRef()).doesNotContain(
                    "http", "cdn", "public", "signed", "storage", "file://");
            assertThat(result.getSafeTraceRef()).startsWith("stub-trace:");
        }

        @Test
        @DisplayName("artifact ref is opaque internal ref")
        void opaqueRef() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.runRenderJob(id, null, null, null);
            String ref = resp.getBody().getData().getArtifactRef();
            assertThat(ref).startsWith("internal-stub-artifact:");
            assertThat(ref).doesNotContain("http", "cdn", "public", "signed", "storage");
        }

        @Test
        @DisplayName("trace ref is opaque internal ref")
        void opaqueTrace() {
            UUID id = runToRenderJobPrepared();
            var resp = controller.runRenderJob(id, null, null, null);
            String trace = resp.getBody().getData().getSafeTraceRef();
            assertThat(trace).startsWith("stub-trace:");
            assertThat(trace).doesNotContain("http", "cdn", "public", "signed");
        }
    }

    // ── H. Render Job Status Enum ─────────────────

    @Nested
    @DisplayName("H. Render Job Status Enum")
    class StatusEnum {

        @Test
        @DisplayName("no PUBLISHED/EXPORTED/AUTO_APPROVED statuses")
        void noForbiddenStatuses() {
            var statuses = Arrays.stream(TurkuRenderJobStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain(
                    "PUBLISHED", "EXPORTED", "AUTO_APPROVED", "DOWNLOAD_READY", "PUBLIC_READY");
        }

        @Test
        @DisplayName("failure codes have no secrets")
        void failureCodesSafe() {
            var codes = Arrays.stream(TurkuRenderFailureCode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(codes).doesNotContain("SECRET_LEAKED", "TOKEN_EXPIRED", "JWT_INVALID");
        }
    }
}
