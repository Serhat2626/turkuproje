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
 * FP-022 Task 05: E2E Render Execution Safety Regression + Final Closure.
 *
 * <p>Full regression covering render job → stub execution → safety envelope →
 * execution safety, DTO safety, endpoint inventory, stage machine,
 * and forbidden behavior across entire FP-022 scope.</p>
 */
@DisplayName("FP-022 Task 05 — E2E Render Execution Safety Regression")
class FP022FinalClosureRegressionTest {

    private TurkuVideoProjectRepository repository;
    private TurkuVideoProjectBootstrapService bootstrapService;
    private TurkuRenderExecutionService executionService;
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
        var renderJobService = new TurkuRenderJobService(repository, renderManifestService);
        var renderRunner = new StubTurkuRenderRunnerGateway();
        executionService = new TurkuRenderExecutionService(repository, renderRunner);
        var actorResolver = new AdminActorResolver();
        controller = new TurkuVideoProjectController(
                bootstrapService, planningService, assetService,
                draftService, imageBatchService, compositionService,
                renderManifestService, renderJobService, executionService, new TurkuStoryboardInternalRenderChainHarness(), actorResolver, null, null, null, null);
    }

    private TurkuVideoProject bootstrapProject() {
        TurkuVideoBootstrapRequest req = new TurkuVideoBootstrapRequest();
        req.setTurkuName("\u00c7anakkale T\u00fcrk\u00fcs\u00fc");
        req.setRegion("\u00c7anakkale");
        req.setMood("Kahramanl\u0131k");
        return bootstrapService.bootstrap(req, "test-admin");
    }

    private UUID runFullPipeline() {
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

    private UUID runToRenderCompleted() {
        UUID id = runFullPipeline();
        controller.prepareRenderJob(id, null, null, null);
        controller.runRenderJob(id, null, null, null);
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
        @DisplayName("no PUT/DELETE endpoints")
        void noUnsafe() {
            assertThat(countMethods(TurkuVideoProjectController.class, PutMapping.class)).isEqualTo(0);
            assertThat(countMethods(TurkuVideoProjectController.class, DeleteMapping.class)).isEqualTo(0);
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
            assertThat(Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(PostMapping.class).value()))
                    .anyMatch(v -> v.contains("run-render-job"))).isTrue();
            assertThat(Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .anyMatch(v -> v.contains("render-execution-safety"))).isTrue();
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── B. E2E Render Execution Flow ───────────────────

    @Nested
    @DisplayName("B. E2E Render Execution Flow")
    class E2EFlow {

        @Test
        @DisplayName("full pipeline to RENDER_JOB_COMPLETED_INTERNAL")
        void fullPipeline() {
            UUID id = runToRenderCompleted();
            var project = bootstrapService.getById(id);
            assertThat(project.getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_JOB_COMPLETED_INTERNAL);
        }

        @Test
        @DisplayName("render job PREPARED after prepare-render-job")
        void prepareRenderJob() {
            UUID id = runFullPipeline();
            controller.prepareRenderJob(id, null, null, null);
            assertThat(bootstrapService.getById(id).getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_JOB_PREPARED);
        }

        @Test
        @DisplayName("render job response has safe metadata")
        void renderJobResponse() {
            UUID id = runFullPipeline();
            var resp = controller.prepareRenderJob(id, null, null, null);
            var data = resp.getBody().getData();
            assertThat(data.getJobId()).isNotNull();
            assertThat(data.isHumanReviewRequired()).isTrue();
            assertThat(data.isAutoApproved()).isFalse();
            assertThat(data.isNotPublishReady()).isTrue();
            assertThat(data.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("GET render-job returns prepared job")
        void getRenderJob() {
            UUID id = runFullPipeline();
            controller.prepareRenderJob(id, null, null, null);
            var resp = controller.getRenderJob(id);
            var data = resp.getBody().getData();
            assertThat(data.getStatus()).isEqualTo(TurkuRenderJobStatus.PREPARED);
        }

        @Test
        @DisplayName("run render job returns COMPLETED_INTERNAL via stub")
        void runRenderJob() {
            UUID id = runFullPipeline();
            controller.prepareRenderJob(id, null, null, null);
            var resp = controller.runRenderJob(id, null, null, null);
            var data = resp.getBody().getData();
            assertThat(data.getStatus()).isEqualTo(TurkuRenderJobStatus.COMPLETED_INTERNAL);
            assertThat(data.getSafeTraceRef()).startsWith("stub-trace:");
            assertThat(data.getArtifactRef()).startsWith("internal-stub-artifact:");
        }

        @Test
        @DisplayName("execution safety envelope after render")
        void executionSafety() {
            UUID id = runToRenderCompleted();
            var resp = controller.getRenderExecutionSafety(id);
            var data = resp.getBody().getData();
            assertThat(data.getJobStatus()).isEqualTo(TurkuRenderJobStatus.COMPLETED_INTERNAL);
            assertThat(data.getSafetyEnvelope().isHumanReviewRequired()).isTrue();
            assertThat(data.getSafetyEnvelope().isAutoApproved()).isFalse();
            assertThat(data.getSafetyEnvelope().isLyricsIncluded()).isFalse();
            assertThat(data.getSafetyEnvelope().isCopyrightClearanceVerified()).isFalse();
        }

        @Test
        @DisplayName("render attempt stored on job")
        void attemptStored() {
            UUID id = runToRenderCompleted();
            var job = bootstrapService.getById(id).getRenderJob();
            assertThat(job.getAttempts()).hasSize(1);
            assertThat(job.getAttempts().get(0).getStatus())
                    .isEqualTo(TurkuRenderJobStatus.COMPLETED_INTERNAL);
            assertThat(job.getAttempts().get(0).getDurationMs()).isGreaterThan(0);
        }

        @Test
        @DisplayName("render artifact on job is safe")
        void artifactSafe() {
            UUID id = runToRenderCompleted();
            var artifact = bootstrapService.getById(id).getRenderJob().getArtifact();
            assertThat(artifact).isNotNull();
            assertThat(artifact.getArtifactRef()).startsWith("internal-stub-artifact:");
            assertThat(artifact.isHumanReviewRequired()).isTrue();
            assertThat(artifact.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("progress response after render")
        void progressResponse() {
            UUID id = runToRenderCompleted();
            var resp = controller.getProgress(id);
            var data = resp.getBody().getData();
            assertThat(data.getCurrentStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_JOB_COMPLETED_INTERNAL);
        }
    }

    // ── C. Stage Machine ────────────────────────────────

    @Nested
    @DisplayName("C. Workflow Stage Machine")
    class StageMachine {

        @Test
        @DisplayName("stage enum has exactly 19 values")
        void enumSize() {
            assertThat(TurkuVideoWorkflowStage.values()).hasSize(19);
        }

        @Test
        @DisplayName("FP-022 stages exist")
        void fp022Stages() {
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_JOB_PREPARED")).isNotNull();
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_JOB_RUNNING")).isNotNull();
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_JOB_COMPLETED_INTERNAL")).isNotNull();
            assertThat(TurkuVideoWorkflowStage.valueOf("RENDER_JOB_FAILED_SAFE")).isNotNull();
            assertThat(TurkuVideoWorkflowStage.valueOf("INTERNAL_RENDER_ARTIFACT_READY")).isNotNull();
        }

        @Test
        @DisplayName("no forbidden stages")
        void noForbiddenStages() {
            var stages = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(stages).doesNotContain(
                    "PUBLISHED", "EXPORTED", "AUTO_APPROVED",
                    "PUBLIC_READY", "CDN_DELIVERED", "DOWNLOAD_READY",
                    "PUBLICLY_DELIVERED", "COMPLETED_AS_PUBLIC_VIDEO");
        }

        @Test
        @DisplayName("stage ordering")
        void stageOrdering() {
            assertThat(TurkuVideoWorkflowStage.RENDER_JOB_PREPARED.ordinal())
                    .isGreaterThan(TurkuVideoWorkflowStage.RENDER_MANIFEST_PREPARED.ordinal());
            assertThat(TurkuVideoWorkflowStage.RENDER_JOB_RUNNING.ordinal())
                    .isGreaterThan(TurkuVideoWorkflowStage.RENDER_JOB_PREPARED.ordinal());
            assertThat(TurkuVideoWorkflowStage.RENDER_JOB_COMPLETED_INTERNAL.ordinal())
                    .isGreaterThan(TurkuVideoWorkflowStage.RENDER_JOB_RUNNING.ordinal());
        }
    }

    // ── D. DTO Safety ──────────────────────────────────

    @Nested
    @DisplayName("D. DTO Safety — Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawProviderResponse", "rawProviderRequest",
                "rawRendererPayload", "token", "jwt", "session", "credential",
                "password", "secret", "apiKey", "authorizationHeader", "principalRaw",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "imageData", "binaryData",
                "lyrics", "videoData", "audioData", "providerEndpoint",
                "renderedVideo", "videoFile", "downloadFile", "videoStream",
                "filePath", "renderedVideoUrl", "commandLine", "ffmpegCommand");

        @Test
        @DisplayName("TurkuRenderJobResponse safe")
        void jobResponse() { assertNoForbidden(TurkuRenderJobResponse.class); }

        @Test
        @DisplayName("TurkuRenderExecutionResponse safe")
        void execResponse() { assertNoForbidden(TurkuRenderExecutionResponse.class); }

        @Test
        @DisplayName("TurkuRenderExecutionSafetyResponse safe")
        void safetyResponse() { assertNoForbidden(TurkuRenderExecutionSafetyResponse.class); }

        @Test
        @DisplayName("TurkuRenderExecutionSafetyEnvelope safe")
        void envelope() { assertNoForbidden(TurkuRenderExecutionSafetyEnvelope.class); }

        @Test
        @DisplayName("TurkuRenderExecutionPlan safe")
        void plan() { assertNoForbidden(TurkuRenderExecutionPlan.class); }

        @Test
        @DisplayName("TurkuRenderExecutionResult safe")
        void result() { assertNoForbidden(TurkuRenderExecutionResult.class); }

        @Test
        @DisplayName("TurkuRenderExecutionRequest safe")
        void request() { assertNoForbidden(TurkuRenderExecutionRequest.class); }

        @Test
        @DisplayName("TurkuRenderJob safe")
        void renderJob() { assertNoForbidden(TurkuRenderJob.class); }

        @Test
        @DisplayName("TurkuRenderAttempt safe")
        void attempt() { assertNoForbidden(TurkuRenderAttempt.class); }

        @Test
        @DisplayName("TurkuRenderArtifact safe")
        void artifact() { assertNoForbidden(TurkuRenderArtifact.class); }

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

    // ── E. Forbidden Behavior ────────────────────────────

    @Nested
    @DisplayName("E. Forbidden Behavior")
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
        @DisplayName("SYSTEM blocked on run-render-job")
        void systemBlocked() {
            UUID id = runFullPipeline();
            controller.prepareRenderJob(id, null, null, null);
            assertThatThrownBy(() ->
                    controller.runRenderJob(id, null, ReviewActorRole.SYSTEM, null))
                    .isInstanceOf(AdminActorResolutionException.class);
        }

        @Test
        @DisplayName("SYSTEM blocked on prepare-render-job")
        void systemBlockedPrepare() {
            UUID id = runFullPipeline();
            assertThatThrownBy(() ->
                    controller.prepareRenderJob(id, null, ReviewActorRole.SYSTEM, null))
                    .isInstanceOf(AdminActorResolutionException.class);
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
        }

        @Test
        @DisplayName("artifact ref is opaque")
        void opaqueRef() {
            UUID id = runToRenderCompleted();
            var artifact = bootstrapService.getById(id).getRenderJob().getArtifact();
            assertThat(artifact.getArtifactRef()).doesNotContain(
                    "http", "cdn", "public", "signed", "storage", "file://",
                    ".mp4", ".avi", ".mov", ".mkv", ".webm");
        }

        @Test
        @DisplayName("no RenderJobStatus PUBLISHED/EXPORTED/AUTO_APPROVED")
        void noForbiddenStatuses() {
            var statuses = Arrays.stream(TurkuRenderJobStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain(
                    "PUBLISHED", "EXPORTED", "AUTO_APPROVED", "DOWNLOAD_READY", "PUBLIC_READY");
        }
    }

    // ── F. Safety Flags ─────────────────────────────────

    @Nested
    @DisplayName("F. Safety Flags")
    class SafetyFlags {

        @Test
        @DisplayName("execution response safety flags")
        void executionFlags() {
            UUID id = runFullPipeline();
            controller.prepareRenderJob(id, null, null, null);
            var resp = controller.runRenderJob(id, null, null, null);
            var data = resp.getBody().getData();
            assertThat(data.isHumanReviewRequired()).isTrue();
            assertThat(data.isCopyrightReviewRequired()).isTrue();
            assertThat(data.isAutoApproved()).isFalse();
            assertThat(data.isNotPublishReady()).isTrue();
            assertThat(data.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("safety envelope flags")
        void envelopeFlags() {
            UUID id = runToRenderCompleted();
            var resp = controller.getRenderExecutionSafety(id);
            var envelope = resp.getBody().getData().getSafetyEnvelope();
            assertThat(envelope.getSafetyFlags()).contains(
                    "NO_REAL_RENDER", "NO_BINARY_OUTPUT", "NO_PUBLIC_DELIVERY", "NO_EXPORT");
            assertThat(envelope.getReviewFlags()).contains(
                    "HUMAN_REVIEW_REQUIRED", "NOT_PUBLISH_READY", "NOT_EXPORT_READY");
        }

        @Test
        @DisplayName("render job safety flags")
        void jobFlags() {
            UUID id = runFullPipeline();
            var resp = controller.prepareRenderJob(id, null, null, null);
            var data = resp.getBody().getData();
            assertThat(data.getReviewFlags()).contains("HUMAN_REVIEW_REQUIRED", "NOT_PUBLISH_READY");
        }
    }

    // ── G. Backward Compatibility ───────────────────────

    @Nested
    @DisplayName("G. Backward Compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("bootstrap still works")
        void bootstrap() {
            var project = bootstrapProject();
            assertThat(project.getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.DRAFT_CREATED);
        }

        @Test
        @DisplayName("full pipeline still works")
        void fullPipeline() {
            UUID id = runFullPipeline();
            assertThat(bootstrapService.getById(id).getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_MANIFEST_PREPARED);
        }

        @Test
        @DisplayName("composition preview still works")
        void compositionPreview() {
            UUID id = runFullPipeline();
            var resp = controller.getCompositionPreview(id);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("render readiness still works")
        void renderReadiness() {
            UUID id = runFullPipeline();
            var resp = controller.getRenderReadiness(id);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("generation safety still works")
        void generationSafety() {
            UUID id = runFullPipeline();
            var resp = controller.getGenerationSafety(id);
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
        }
    }
}
