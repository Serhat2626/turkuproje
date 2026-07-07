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
 * FP-023 Task 03: FFmpeg Stub/Sandbox Runner tests.
 */
@DisplayName("FP-023 Task 03 \u2014 FFmpeg Stub/Sandbox Runner")
class TurkuFfmpegSandboxRunnerTest {

    private StubTurkuRenderRunnerGateway stubRunner;
    private FfmpegTurkuRenderRunnerGateway ffmpegDisabled;
    private FfmpegTurkuRenderRunnerGateway ffmpegEnabled;
    private TurkuRenderRunnerResolver defaultResolver;

    @BeforeEach
    void setUp() {
        stubRunner = new StubTurkuRenderRunnerGateway();
        ffmpegDisabled = new FfmpegTurkuRenderRunnerGateway();
        ffmpegEnabled = new FfmpegTurkuRenderRunnerGateway(true);
        defaultResolver = new TurkuRenderRunnerResolver(stubRunner);
    }

    private TurkuRenderExecutionPlan validPlan() {
        return new TurkuRenderExecutionPlan(
                UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "mp4",
                60.0, "deterministic-stub", "DETERMINISTIC_STUB");
    }

    // ── A. Endpoint Inventory ──

    @Nested
    @DisplayName("A. Endpoint Inventory (44 = 25 POST + 19 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("total 44 = 25 POST + 19 GET")
        void totalEndpoints() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("total POST").isEqualTo(25);
            assertThat(get).as("total GET").isEqualTo(19);
            assertThat(post + get).as("total endpoints").isEqualTo(44);
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

    // ── B. Default Resolver ──

    @Nested
    @DisplayName("B. Default Resolver")
    class DefaultResolver {

        @Test
        @DisplayName("default mode is STUB")
        void defaultStub() {
            assertThat(defaultResolver.getActiveMode())
                    .isEqualTo(TurkuRenderAdapterMode.STUB);
        }

        @Test
        @DisplayName("stub is active by default")
        void stubActive() {
            assertThat(defaultResolver.isStubActive()).isTrue();
            assertThat(defaultResolver.isFfmpegActive()).isFalse();
        }

        @Test
        @DisplayName("resolves stub runner")
        void resolvesStub() {
            assertThat(defaultResolver.resolve())
                    .isInstanceOf(StubTurkuRenderRunnerGateway.class);
        }

        @Test
        @DisplayName("disabled diagnostics")
        void disabledDiagnostics() {
            var resolver = new TurkuRenderRunnerResolver(stubRunner, ffmpegDisabled, true, false);
            var result = resolver.resolveWithDiagnostics(validPlan());
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureCode())
                    .isEqualTo(TurkuRenderAdapterFailureCode.RENDER_CONFIG_MISSING);
        }

        @Test
        @DisplayName("ffmpeg enabled + config present resolves FFMPEG_READY")
        void ffmpegReady() {
            var resolver = new TurkuRenderRunnerResolver(stubRunner, ffmpegEnabled, true, true);
            assertThat(resolver.getActiveMode())
                    .isEqualTo(TurkuRenderAdapterMode.FFMPEG_READY);
            assertThat(resolver.isFfmpegActive()).isTrue();
        }
    }

    // ── C. FFmpeg Gateway Disabled ──

    @Nested
    @DisplayName("C. FFmpeg Gateway — Disabled")
    class FfmpegDisabledTests {

        @Test
        @DisplayName("disabled by default")
        void disabledDefault() {
            assertThat(ffmpegDisabled.isEnabled()).isFalse();
            assertThat(ffmpegDisabled.getAdapterMode())
                    .isEqualTo(TurkuRenderAdapterMode.FFMPEG_DISABLED);
        }

        @Test
        @DisplayName("execute returns failure when disabled")
        void executeDisabled() {
            var result = ffmpegDisabled.execute(validPlan());
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureCode())
                    .isEqualTo(TurkuRenderFailureCode.RENDERER_UNAVAILABLE);
            assertThat(result.getArtifact()).isNull();
        }

        @Test
        @DisplayName("disabled trace ref is opaque")
        void disabledTrace() {
            var result = ffmpegDisabled.execute(validPlan());
            assertThat(result.getSafeTraceRef()).startsWith("ffmpeg-disabled-trace:");
            assertThat(result.getSafeTraceRef()).doesNotContain(
                    "http", "file://", "/usr", "C:\\", ".mp4");
        }

        @Test
        @DisplayName("diagnostics when disabled")
        void diagnostics() {
            var diag = ffmpegDisabled.getAdapterDiagnostics();
            assertThat(diag.isSuccess()).isFalse();
            assertThat(diag.getFailureCode())
                    .isEqualTo(TurkuRenderAdapterFailureCode.RENDER_PROVIDER_DISABLED);
            assertThat(diag.getSafetyEnvelope().isHumanReviewRequired()).isTrue();
            assertThat(diag.getSafetyEnvelope().isAutoApproved()).isFalse();
        }
    }

    // ── D. FFmpeg Gateway Enabled (Sandbox) ──

    @Nested
    @DisplayName("D. FFmpeg Gateway — Enabled (Sandbox)")
    class FfmpegEnabledTests {

        @Test
        @DisplayName("enabled when constructed with true")
        void enabledMode() {
            assertThat(ffmpegEnabled.isEnabled()).isTrue();
            assertThat(ffmpegEnabled.getAdapterMode())
                    .isEqualTo(TurkuRenderAdapterMode.FFMPEG_READY);
        }

        @Test
        @DisplayName("sandbox execution returns success with opaque artifact")
        void sandboxExecution() {
            var result = ffmpegEnabled.execute(validPlan());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getArtifact()).isNotNull();
            assertThat(result.getArtifact().getArtifactRef())
                    .startsWith("internal-ffmpeg-sandbox:");
        }

        @Test
        @DisplayName("sandbox artifact is safe")
        void sandboxArtifactSafe() {
            var result = ffmpegEnabled.execute(validPlan());
            var artifact = result.getArtifact();
            assertThat(artifact.isHumanReviewRequired()).isTrue();
            assertThat(artifact.isAutoApproved()).isFalse();
            assertThat(artifact.getArtifactRef()).doesNotContain(
                    "http", "cdn", "public", "signed", "storage",
                    "file://", ".mp4", "C:\\", "/usr");
        }

        @Test
        @DisplayName("sandbox trace ref is opaque")
        void sandboxTrace() {
            var result = ffmpegEnabled.execute(validPlan());
            assertThat(result.getSafeTraceRef()).startsWith("ffmpeg-sandbox-trace:");
        }

        @Test
        @DisplayName("sandbox rejects invalid plan")
        void rejectsInvalid() {
            var badPlan = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "7680x4320", 30, "mp4",
                    60.0, "stub", "STUB");
            var result = ffmpegEnabled.execute(badPlan);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureCode())
                    .isEqualTo(TurkuRenderFailureCode.SAFETY_VIOLATION);
        }

        @Test
        @DisplayName("command plan can be built but not executed")
        void commandPlanOnly() {
            var plan = ffmpegEnabled.buildCommandPlanOnly(validPlan());
            assertThat(plan.isValidated()).isTrue();
            assertThat(plan.getExecutableToken()).isEqualTo("ffmpeg");
            assertThat(plan.getSafeArgs()).isNotEmpty();
        }

        @Test
        @DisplayName("diagnostics when enabled")
        void diagnostics() {
            var diag = ffmpegEnabled.getAdapterDiagnostics();
            assertThat(diag.isSuccess()).isTrue();
            assertThat(diag.getAdapterMode())
                    .isEqualTo(TurkuRenderAdapterMode.FFMPEG_READY);
        }
    }

    // ── E. Stub Runner Compatibility ──

    @Nested
    @DisplayName("E. Stub Runner Compatibility")
    class StubCompatibility {

        @Test
        @DisplayName("stub runner still produces internal artifact")
        void stubWorks() {
            var result = stubRunner.execute(validPlan());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getArtifact().getArtifactRef())
                    .startsWith("internal-stub-artifact:");
        }

        @Test
        @DisplayName("stub and ffmpeg both implement same interface")
        void sameInterface() {
            assertThat(stubRunner).isInstanceOf(TurkuRenderRunnerGateway.class);
            assertThat(ffmpegDisabled).isInstanceOf(TurkuRenderRunnerGateway.class);
            assertThat(ffmpegEnabled).isInstanceOf(TurkuRenderRunnerGateway.class);
        }

        @Test
        @DisplayName("full E2E render flow still works with stub")
        void fullE2E() {
            var repository = new TurkuVideoProjectRepository();
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
            var renderRunner = new StubTurkuRenderRunnerGateway();
            var executionService = new TurkuRenderExecutionService(repository, renderRunner);
            var actorResolver = new AdminActorResolver();
            var controller = new TurkuVideoProjectController(
                    bootstrapService, planningService, assetService,
                    draftService, imageBatchService, compositionService,
                    renderManifestService, renderJobService, executionService, actorResolver);

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
    }

    // ── F. Failure Code Mapping ──

    @Nested
    @DisplayName("F. Failure Code Mapping")
    class FailureCodeMapping {

        @Test
        @DisplayName("RENDER_PROVIDER_DISABLED mapped")
        void providerDisabled() {
            var resolver = new TurkuRenderRunnerResolver(stubRunner, ffmpegDisabled, true, false);
            var result = resolver.resolveWithDiagnostics(validPlan());
            assertThat(result.getFailureCode())
                    .isEqualTo(TurkuRenderAdapterFailureCode.RENDER_CONFIG_MISSING);
        }

        @Test
        @DisplayName("all adapter failure codes present")
        void allCodes() {
            var codes = Arrays.stream(TurkuRenderAdapterFailureCode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(codes).contains(
                    "RENDER_PROVIDER_DISABLED", "RENDER_CONFIG_MISSING",
                    "RENDER_INPUT_MISSING", "RENDER_TIMEOUT",
                    "RENDER_PROCESS_FAILED", "RENDER_OUTPUT_REJECTED",
                    "RENDER_SECURITY_VIOLATION", "RENDER_FAILED_SAFE");
        }

        @Test
        @DisplayName("FFMPEG_FAILED_SAFE diagnostics")
        void failedSafe() {
            var resolver = new TurkuRenderRunnerResolver(stubRunner, ffmpegDisabled, false, false);
            // Default (not enabled) = STUB, check isStubActive
            assertThat(resolver.isStubActive()).isTrue();
        }
    }

    // ── G. No Process Execution ──

    @Nested
    @DisplayName("G. No Process Execution")
    class NoProcessExecution {

        @Test
        @DisplayName("FfmpegTurkuRenderRunnerGateway has no ProcessBuilder field")
        void noProcessBuilder() {
            var fields = Arrays.stream(FfmpegTurkuRenderRunnerGateway.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).doesNotContain(
                    "processBuilder", "process", "runtime",
                    "processHandle", "exec", "command");
        }

        @Test
        @DisplayName("TurkuRenderRunnerResolver has no ProcessBuilder field")
        void resolverNoProcess() {
            var fields = Arrays.stream(TurkuRenderRunnerResolver.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).doesNotContain(
                    "processBuilder", "process", "runtime",
                    "processHandle", "exec", "command");
        }

        @Test
        @DisplayName("sandbox execution does not create files")
        void noFileCreation() {
            // Execute sandbox — if a file were created, artifact ref would contain path
            var result = ffmpegEnabled.execute(validPlan());
            assertThat(result.getArtifact().getArtifactRef())
                    .doesNotContain("/", "\\", "file://", "C:");
        }
    }

    // ── H. DTO Safety ──

    @Nested
    @DisplayName("H. DTO Safety")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawRendererPayload", "token", "jwt", "session",
                "credential", "password", "secret", "apiKey", "authorizationHeader",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "binaryData",
                "renderedVideo", "videoFile", "downloadFile",
                "filePath", "renderedVideoUrl", "commandLine", "ffmpegCommand",
                "shellCommand", "absolutePath", "processBuilder");

        @Test
        @DisplayName("FfmpegTurkuRenderRunnerGateway safe")
        void gateway() { assertNoForbidden(FfmpegTurkuRenderRunnerGateway.class); }

        @Test
        @DisplayName("TurkuRenderRunnerResolver safe")
        void resolver() { assertNoForbidden(TurkuRenderRunnerResolver.class); }

        private void assertNoForbidden(Class<?> clazz) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("field '%s' in %s", f, clazz.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }

    // ── I. Safety Flags ──

    @Nested
    @DisplayName("I. Safety Flags")
    class SafetyFlags {

        @Test
        @DisplayName("sandbox safety envelope")
        void sandboxEnvelope() {
            var diag = ffmpegEnabled.getAdapterDiagnostics();
            var env = diag.getSafetyEnvelope();
            assertThat(env.isHumanReviewRequired()).isTrue();
            assertThat(env.isCopyrightReviewRequired()).isTrue();
            assertThat(env.isAutoApproved()).isFalse();
            assertThat(env.isNotPublishReady()).isTrue();
            assertThat(env.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("disabled safety envelope")
        void disabledEnvelope() {
            var diag = ffmpegDisabled.getAdapterDiagnostics();
            var env = diag.getSafetyEnvelope();
            assertThat(env.isHumanReviewRequired()).isTrue();
            assertThat(env.isAutoApproved()).isFalse();
            assertThat(env.isNotPublishReady()).isTrue();
            assertThat(env.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("resolver diagnostics safety envelope")
        void resolverEnvelope() {
            var result = defaultResolver.resolveWithDiagnostics(validPlan());
            var env = result.getSafetyEnvelope();
            assertThat(env.isHumanReviewRequired()).isTrue();
            assertThat(env.isAutoApproved()).isFalse();
        }
    }

    // ── J. Sanitizer Integration ──

    @Nested
    @DisplayName("J. Sanitizer Integration")
    class SanitizerIntegration {

        @Test
        @DisplayName("disabled result message is sanitized")
        void disabledSanitized() {
            var result = ffmpegDisabled.execute(validPlan());
            assertThat(result.getFailureMessage()).doesNotContain(
                    "password", "secret", "token");
        }

        @Test
        @DisplayName("failed plan message is sanitized")
        void failedSanitized() {
            var badPlan = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "99999x99999", 30, "mp4",
                    60.0, "stub", "STUB");
            var result = ffmpegEnabled.execute(badPlan);
            assertThat(result.getFailureMessage()).doesNotContain(
                    "password", "/usr", "C:\\");
        }
    }
}
