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
 * FP-023 Task 06: E2E Render Adapter Safety Regression + Final Closure.
 */
@DisplayName("FP-023 Task 06 — E2E Render Adapter Safety Regression")
class FP023FinalClosureRegressionTest {

    private TurkuVideoProjectRepository repository;
    private StubTurkuRenderRunnerGateway stubRunner;
    private FfmpegTurkuRenderRunnerGateway ffmpegDisabled;
    private FfmpegTurkuRenderRunnerGateway ffmpegSandbox;
    private TurkuRenderRunnerResolver defaultResolver;
    private TurkuRenderArtifactService artifactService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTurkuVideoProjectRepository();
        stubRunner = new StubTurkuRenderRunnerGateway();
        ffmpegDisabled = new FfmpegTurkuRenderRunnerGateway();
        ffmpegSandbox = new FfmpegTurkuRenderRunnerGateway(true);
        defaultResolver = new TurkuRenderRunnerResolver(stubRunner);
        artifactService = new TurkuRenderArtifactService();
    }

    private TurkuRenderExecutionPlan validPlan() {
        return new TurkuRenderExecutionPlan(
                UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "mp4",
                60.0, "deterministic-stub", "DETERMINISTIC_STUB");
    }

    // ── A. Endpoint Inventory ──

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
        @DisplayName("TurkuVideoProjectController: 17 POST + 8 GET = 25")
        void turkuController() {
            long post = countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("türkü POST").isEqualTo(17);
            assertThat(get).as("türkü GET").isEqualTo(8);
        }

        @Test
        @DisplayName("no PUT/DELETE endpoints")
        void noUnsafe() {
            assertThat(countMethods(TurkuVideoProjectController.class, PutMapping.class)).isEqualTo(0);
            assertThat(countMethods(TurkuVideoProjectController.class, DeleteMapping.class)).isEqualTo(0);
            assertThat(countMethods(AdminReviewController.class, PutMapping.class)).isEqualTo(0);
            assertThat(countMethods(AdminReviewController.class, DeleteMapping.class)).isEqualTo(0);
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── B. Full E2E Pipeline ──

    @Nested
    @DisplayName("B. Full E2E Pipeline — Stub Runner")
    class FullE2E {

        @Test
        @DisplayName("complete bootstrap-to-render pipeline with stub")
        void fullPipeline() {
            var bootstrapService = new TurkuVideoProjectBootstrapService(repository);
            var planningService = new TurkuVideoPlanningService(repository);
            var assetService = new TurkuVideoAssetPlanningService(repository);
            var gateway = new StubTurkuGenerationProviderGateway();
            var sanitizerP = new TurkuProviderSafetySanitizer();
            var policyP = new TurkuProviderSafetyPolicy();
            var draftService = new TurkuProviderDraftPlanningService(repository, gateway, sanitizerP, policyP);
            var imageBatchService = new TurkuProviderImageBatchService(repository, gateway, sanitizerP, policyP);
            var compositionService = new TurkuVideoCompositionService(repository);
            var renderManifestService = new TurkuRenderManifestService(repository);
            var renderJobService = new TurkuRenderJobService(repository, renderManifestService);
            var executionService = new TurkuRenderExecutionService(repository, stubRunner);
            var actorResolver = new AdminActorResolver();
            var controller = new TurkuVideoProjectController(
                    bootstrapService, planningService, assetService,
                    draftService, imageBatchService, compositionService,
                    renderManifestService, renderJobService, executionService, new TurkuStoryboardInternalRenderChainHarness(), actorResolver);

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
            controller.prepareRenderManifest(id, null, null, null);
            controller.prepareRenderJob(id, null, null, null);
            controller.runRenderJob(id, null, null, null);

            var p = bootstrapService.getById(id);
            assertThat(p.getWorkflowStage())
                    .isEqualTo(TurkuVideoWorkflowStage.RENDER_JOB_COMPLETED_INTERNAL);
        }

        @Test
        @DisplayName("artifact created from stub E2E result")
        void artifactFromStub() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            assertThat(artifact).isNotNull();
            assertThat(artifact.getStatus()).isEqualTo(TurkuRenderArtifactStatus.VALIDATED);
            assertThat(artifact.getReference().getOpaqueRef()).startsWith("internal-stub-artifact:");
            assertThat(artifact.getSafetyReport().isHumanReviewRequired()).isTrue();
            assertThat(artifact.getSafetyReport().isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("artifact created from sandbox E2E result")
        void artifactFromSandbox() {
            var result = ffmpegSandbox.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.FFMPEG_READY);
            assertThat(artifact).isNotNull();
            assertThat(artifact.getReference().getOpaqueRef()).startsWith("internal-ffmpeg-sandbox:");
            assertThat(artifact.getReference().getRendererMode()).isEqualTo(TurkuRenderAdapterMode.FFMPEG_READY);
        }

        @Test
        @DisplayName("cleanup plan from artifact is metadata-only")
        void cleanupMetadataOnly() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            var plan = artifactService.createCleanupPlan(artifact);
            assertThat(plan).isNotNull();
            assertThat(plan.isEligible()).isTrue();
            assertThat(plan.getRetentionPolicy().getRetentionHours()).isEqualTo(24);
        }
    }

    // ── C. Render Adapter Contract ──

    @Nested
    @DisplayName("C. Render Adapter Contract")
    class AdapterContract {

        @Test
        @DisplayName("stub remains default runner")
        void stubDefault() {
            assertThat(defaultResolver.getActiveMode()).isEqualTo(TurkuRenderAdapterMode.STUB);
            assertThat(defaultResolver.isStubActive()).isTrue();
            assertThat(defaultResolver.isFfmpegActive()).isFalse();
        }

        @Test
        @DisplayName("FFmpeg disabled by default")
        void ffmpegDisabledDefault() {
            assertThat(ffmpegDisabled.isEnabled()).isFalse();
            assertThat(ffmpegDisabled.getAdapterMode()).isEqualTo(TurkuRenderAdapterMode.FFMPEG_DISABLED);
        }

        @Test
        @DisplayName("disabled FFmpeg returns safe failure")
        void disabledFailure() {
            var result = ffmpegDisabled.execute(validPlan());
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureCode()).isEqualTo(TurkuRenderFailureCode.RENDERER_UNAVAILABLE);
            assertThat(result.getArtifact()).isNull();
        }

        @Test
        @DisplayName("sandbox FFmpeg builds plan but does not execute")
        void sandboxNoExecution() {
            var plan = ffmpegSandbox.buildCommandPlanOnly(validPlan());
            assertThat(plan.isValidated()).isTrue();
            assertThat(plan.getExecutableToken()).isEqualTo("ffmpeg");
            // Sandbox execution returns opaque artifact, not real file
            var result = ffmpegSandbox.execute(validPlan());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getArtifact().getArtifactRef()).startsWith("internal-ffmpeg-sandbox:");
        }

        @Test
        @DisplayName("both runners implement same interface")
        void sameInterface() {
            assertThat(stubRunner).isInstanceOf(TurkuRenderRunnerGateway.class);
            assertThat(ffmpegDisabled).isInstanceOf(TurkuRenderRunnerGateway.class);
            assertThat(ffmpegSandbox).isInstanceOf(TurkuRenderRunnerGateway.class);
        }

        @Test
        @DisplayName("adapter mode enum has 5 values")
        void adapterModeValues() {
            assertThat(TurkuRenderAdapterMode.values()).hasSize(5);
            var names = Arrays.stream(TurkuRenderAdapterMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).contains("STUB", "FFMPEG_DISABLED", "FFMPEG_CONFIG_MISSING",
                    "FFMPEG_READY", "FFMPEG_FAILED_SAFE");
        }

        @Test
        @DisplayName("adapter failure code enum has 8 values")
        void failureCodeValues() {
            assertThat(TurkuRenderAdapterFailureCode.values()).hasSize(8);
        }
    }

    // ── D. Secure Command Builder ──

    @Nested
    @DisplayName("D. Secure Command Builder")
    class CommandBuilder {

        @Test
        @DisplayName("rejects oversized resolution")
        void rejectsOversize() {
            var badPlan = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "7680x4320", 30, "mp4",
                    60.0, "stub", "STUB");
            var cmd = ffmpegSandbox.buildCommandPlanOnly(badPlan);
            assertThat(cmd.isValidated()).isFalse();
        }

        @Test
        @DisplayName("rejects shell metacharacters in format")
        void rejectsShell() {
            var badPlan = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "mp4;rm -rf /",
                    60.0, "stub", "STUB");
            var cmd = ffmpegSandbox.buildCommandPlanOnly(badPlan);
            assertThat(cmd.isValidated()).isFalse();
        }

        @Test
        @DisplayName("valid plan produces validated command plan")
        void validPlanProducesValidated() {
            var plan = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "mp4",
                    60.0, "deterministic-stub", "DETERMINISTIC_STUB");
            var cmd = ffmpegSandbox.buildCommandPlanOnly(plan);
            assertThat(cmd.isValidated()).isTrue();
            assertThat(cmd.getSafeArgs()).doesNotContain(";", "&&", "||", "$(", "`");
        }
    }

    // ── E. Artifact Metadata Safety ──

    @Nested
    @DisplayName("E. Artifact Metadata Safety")
    class ArtifactSafety {

        @Test
        @DisplayName("artifact status enum has no forbidden values")
        void noForbiddenStatuses() {
            var names = Arrays.stream(TurkuRenderArtifactStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED", "EXPORTED", "DOWNLOADED",
                    "PUBLIC_READY", "AUTO_APPROVED", "COMPLETED_AS_PUBLIC_VIDEO");
        }

        @Test
        @DisplayName("safety report always has correct flags")
        void safetyReportFlags() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            var report = artifact.getSafetyReport();
            assertThat(report.isHumanReviewRequired()).isTrue();
            assertThat(report.isCopyrightReviewRequired()).isTrue();
            assertThat(report.isAutoApproved()).isFalse();
            assertThat(report.isNotPublishReady()).isTrue();
            assertThat(report.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("unsafe artifact refs rejected")
        void unsafeRefsRejected() {
            List<String> unsafe = List.of(
                    "https://cdn.example.com/video.mp4", "C:\\Users\\video.mp4",
                    "/usr/local/render.mp4", "s3://bucket/key",
                    "storageKey=abc", "base64:data", "Bearer token",
                    "password=secret", "$(rm -rf /)", "ref && cat /etc/passwd");
            for (String ref : unsafe) {
                assertThat(artifactService.isValidOpaqueRef(ref))
                        .as("should reject: %s", ref).isFalse();
            }
        }

        @Test
        @DisplayName("retention policy bounded")
        void retentionBounded() {
            var low = new TurkuRenderArtifactRetentionPolicy(-100, false, "test");
            assertThat(low.getRetentionHours()).isEqualTo(1);
            var high = new TurkuRenderArtifactRetentionPolicy(99999, false, "test");
            assertThat(high.getRetentionHours()).isEqualTo(168);
        }
    }

    // ── F. No Process Execution ──

    @Nested
    @DisplayName("F. No Process Execution")
    class NoProcessExecution {

        private static final Set<String> PROCESS_FIELDS = Set.of(
                "processBuilder", "process", "runtime", "processHandle", "exec", "command");

        @Test
        @DisplayName("FfmpegTurkuRenderRunnerGateway has no process fields")
        void ffmpegNoProcess() {
            assertNoProcessFields(FfmpegTurkuRenderRunnerGateway.class);
        }

        @Test
        @DisplayName("TurkuRenderRunnerResolver has no process fields")
        void resolverNoProcess() {
            assertNoProcessFields(TurkuRenderRunnerResolver.class);
        }

        @Test
        @DisplayName("TurkuFfmpegCommandBuilder has no process fields")
        void builderNoProcess() {
            assertNoProcessFields(TurkuFfmpegCommandBuilder.class);
        }

        @Test
        @DisplayName("TurkuRenderArtifactService has no process fields")
        void artifactServiceNoProcess() {
            assertNoProcessFields(TurkuRenderArtifactService.class);
        }

        private void assertNoProcessFields(Class<?> clazz) {
            var fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : PROCESS_FIELDS) {
                assertThat(fields).as("%s in %s", f, clazz.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── G. Forbidden State Regression ──

    @Nested
    @DisplayName("G. Forbidden State Regression")
    class ForbiddenStates {

        @Test
        @DisplayName("workflow stages exclude forbidden states")
        void noForbiddenStages() {
            var stages = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(stages).doesNotContain(
                    "PUBLISHED", "EXPORTED", "PUBLICLY_DELIVERED",
                    "AUTO_APPROVED", "COMPLETED_AS_PUBLIC_VIDEO");
        }

        @Test
        @DisplayName("workflow stage count")
        void stageCount() {
            assertThat(TurkuVideoWorkflowStage.values()).hasSizeGreaterThanOrEqualTo(14);
        }
    }

    // ── H. DTO Safety Regression ──

    @Nested
    @DisplayName("H. DTO Safety Regression — All FP-023 Classes")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawRendererPayload", "rawProviderPayload",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "credential", "password", "secret", "apiKey",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "binaryData",
                "renderedVideo", "videoFile", "downloadFile",
                "filePath", "renderedVideoUrl", "commandLine", "ffmpegCommand",
                "shellCommand", "absolutePath", "processBuilder");

        private static final List<Class<?>> CLASSES = List.of(
                FfmpegTurkuRenderRunnerGateway.class,
                TurkuRenderRunnerResolver.class,
                TurkuRenderAdapterMode.class,
                TurkuRenderAdapterFailureCode.class,
                TurkuRenderAdapterPolicy.class,
                TurkuRenderAdapterSanitizer.class,
                TurkuRenderAdapterResult.class,
                TurkuRenderAdapterTraceReference.class,
                TurkuRenderAdapterSafetyEnvelope.class,
                TurkuFfmpegCommandBuilder.class,
                TurkuFfmpegCommandPlan.class,
                TurkuFfmpegExecutionLimits.class,
                TurkuInternalRenderArtifact.class,
                TurkuRenderArtifactReference.class,
                TurkuRenderArtifactSafetyReport.class,
                TurkuRenderArtifactRetentionPolicy.class,
                TurkuRenderArtifactCleanupPlan.class,
                TurkuRenderArtifactService.class,
                TurkuRenderArtifactStatus.class);

        @Test
        @DisplayName("19 FP-023 classes x 31 forbidden fields = 0 violations")
        void allClassesSafe() {
            for (Class<?> clazz : CLASSES) {
                Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN) {
                    assertThat(fields)
                            .as("field '%s' in %s", f, clazz.getSimpleName())
                            .doesNotContain(f);
                }
            }
        }
    }

    // ── I. Safety Envelope Defaults ──

    @Nested
    @DisplayName("I. Safety Envelope Defaults")
    class SafetyEnvelopes {

        @Test
        @DisplayName("adapter safety envelope defaults")
        void adapterEnvelope() {
            var envelope = new TurkuRenderAdapterSafetyEnvelope(TurkuRenderAdapterMode.STUB);
            assertThat(envelope.isHumanReviewRequired()).isTrue();
            assertThat(envelope.isCopyrightReviewRequired()).isTrue();
            assertThat(envelope.isAutoApproved()).isFalse();
            assertThat(envelope.isNotPublishReady()).isTrue();
            assertThat(envelope.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("disabled FFmpeg diagnostics envelope")
        void disabledDiagnostics() {
            var diag = ffmpegDisabled.getAdapterDiagnostics();
            assertThat(diag.getSafetyEnvelope().isHumanReviewRequired()).isTrue();
            assertThat(diag.getSafetyEnvelope().isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("sandbox FFmpeg diagnostics envelope")
        void sandboxDiagnostics() {
            var diag = ffmpegSandbox.getAdapterDiagnostics();
            assertThat(diag.isSuccess()).isTrue();
            assertThat(diag.getSafetyEnvelope().isNotPublishReady()).isTrue();
        }

        @Test
        @DisplayName("resolver diagnostics envelope")
        void resolverDiagnostics() {
            var result = defaultResolver.resolveWithDiagnostics(validPlan());
            assertThat(result.getSafetyEnvelope().isHumanReviewRequired()).isTrue();
            assertThat(result.getSafetyEnvelope().isAutoApproved()).isFalse();
        }
    }

    // ── J. Sanitizer Integration ──

    @Nested
    @DisplayName("J. Sanitizer Integration")
    class Sanitizer {

        @Test
        @DisplayName("disabled result sanitized")
        void disabledSanitized() {
            var result = ffmpegDisabled.execute(validPlan());
            assertThat(result.getFailureMessage())
                    .doesNotContain("password", "secret", "/usr", "C:\\");
        }

        @Test
        @DisplayName("sandbox trace refs opaque")
        void sandboxTrace() {
            var result = ffmpegSandbox.execute(validPlan());
            assertThat(result.getSafeTraceRef()).startsWith("ffmpeg-sandbox-trace:");
            assertThat(result.getSafeTraceRef()).doesNotContain("http", "file://", ".mp4");
        }

        @Test
        @DisplayName("disabled trace refs opaque")
        void disabledTrace() {
            var result = ffmpegDisabled.execute(validPlan());
            assertThat(result.getSafeTraceRef()).startsWith("ffmpeg-disabled-trace:");
        }
    }

    // ── K. No Publish/Export/Download ──

    @Nested
    @DisplayName("K. No Publish/Export/Download Endpoints")
    class NoPublishExport {

        @Test
        @DisplayName("controller has no publish method")
        void noPublish() {
            var methods = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            assertThat(methods).doesNotContain("publish", "publishProject", "exportProject",
                    "downloadVideo", "exportVideo", "publicDelivery");
        }

        @Test
        @DisplayName("controller has no download method")
        void noDownload() {
            var methods = Arrays.stream(TurkuVideoProjectController.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            assertThat(methods).doesNotContain("download", "downloadFile", "downloadRender",
                    "downloadArtifact", "getVideoFile");
        }
    }
}
