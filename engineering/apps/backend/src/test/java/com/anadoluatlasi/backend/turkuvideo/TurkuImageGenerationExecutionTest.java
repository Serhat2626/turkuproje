package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RPE-006 — Manual-Gated Image Generation Execution Boundary")
class TurkuImageGenerationExecutionTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<?>> RPE006_CLASSES = List.of(
            TurkuFakeImageResultMetadata.class, TurkuImageGenerationExecutionResult.class,
            TurkuImageGenerationExecutionService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED");

    private TurkuImageGenerationExecutionService service;

    @BeforeEach void setUp() { service = new TurkuImageGenerationExecutionService(); }

    private TurkuImagePromptPackageResult promptPackage() {
        var req = new TurkuNameStoryboardRequestService().createRequest(
                new TurkuNameStoryboardRequest("Canakkale", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
        var e = new TurkuManualEnablementService().evaluate(r);
        var wo = new TurkuWorkOrderPreviewService().generate(e);
        var gate = new TurkuExecutionGateService().evaluate(wo);
        var prov = new TurkuProviderConfigService().evaluate(gate);
        var render = new TurkuRenderPipelineConfigService().evaluate(prov);
        var exec = new TurkuExecutionPackageService().assemble(wo, prov, render);
        return new TurkuImagePromptPackageService().prepare(exec);
    }

    // A. CI_FAKE Execution
    @Nested @DisplayName("A. CI_FAKE Execution")
    class CiFake {
        @Test @DisplayName("FAKE_RESULT_AVAILABLE")
        void status() {
            var r = service.execute(promptPackage());
            assertThat(r.getStatus()).isEqualTo(TurkuImageGenerationStatus.FAKE_RESULT_AVAILABLE);
            assertThat(r.getOpaqueGenerationRef()).startsWith("igen:");
            assertThat(r.getExecutionMode()).isEqualTo(TurkuImageGenerationExecutionMode.CI_FAKE);
        }
        @Test @DisplayName("4 fake results")
        void results() {
            var r = service.execute(promptPackage());
            assertThat(r.getFakeResults()).hasSize(4);
            assertThat(r.getFakeResultCount()).isEqualTo(4);
            assertThat(r.getTotalScenes()).isEqualTo(4);
        }
        @Test @DisplayName("manual gate not passed")
        void gate() {
            var r = service.execute(promptPackage());
            assertThat(r.isManualGatePassed()).isFalse();
        }
    }

    // B. Fake Result Metadata
    @Nested @DisplayName("B. Fake Result Metadata")
    class FakeMetadata {
        @Test @DisplayName("opaque refs only")
        void refs() {
            var r = service.execute(promptPackage());
            for (var f : r.getFakeResults()) {
                assertThat(f.getOpaqueArtifactRef()).startsWith("fimg:");
                assertThat(f.getArtifactLabel()).isNotBlank();
                assertThat(f.getFormatHint()).isEqualTo("PNG");
                assertThat(f.getWidthHint()).isEqualTo(1920);
            }
        }
        @Test @DisplayName("no URL/key/path/base64 in metadata")
        void noForbidden() {
            var r = service.execute(promptPackage());
            for (var f : r.getFakeResults())
                for (var fb : FORBIDDEN)
                    assertThat(f.getGenerationNote()).doesNotContain(fb);
        }
    }

    // C. Provider Disabled
    @Nested @DisplayName("C. Provider Disabled")
    class Disabled {
        @Test @DisplayName("provider disabled reason")
        void reason() {
            var r = service.execute(promptPackage());
            assertThat(r.getProviderDisabledReason()).contains("devre disi");
            assertThat(r.getIssues()).contains(TurkuImageGenerationIssueCode.PROVIDER_EXECUTION_DISABLED);
        }
    }

    // D. Blocked
    @Nested @DisplayName("D. Blocked / Missing")
    class Blocked {
        @Test @DisplayName("null prompt -> BLOCKED_BY_SAFETY")
        void nullPrompt() {
            var r = service.execute(null);
            assertThat(r.getStatus()).isEqualTo(TurkuImageGenerationStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getExecutionMode()).isEqualTo(TurkuImageGenerationExecutionMode.DISABLED);
            assertThat(r.getFakeResults()).isEmpty();
        }
    }

    // E. Forbidden Sweep
    @Nested @DisplayName("E. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : RPE006_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // F. No Forbidden Statuses
    @Nested @DisplayName("F. No Forbidden Statuses")
    class NoForbidden {
        @Test @DisplayName("status safe")
        void safe() {
            for (var v : TurkuImageGenerationStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("exactly 5 statuses")
        void count() { assertThat(TurkuImageGenerationStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 6 issue codes")
        void issues() { assertThat(TurkuImageGenerationIssueCode.values()).hasSize(6); }
        @Test @DisplayName("exactly 3 execution modes")
        void modes() { assertThat(TurkuImageGenerationExecutionMode.values()).hasSize(3); }
    }

    // G. Endpoint Unchanged
    @Nested @DisplayName("G. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("total unchanged")
        void total() {
            long post = count(AdminReviewController.class, PostMapping.class)
                    + count(AdminPipelineController.class, PostMapping.class)
                    + count(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + count(TurkuVideoProjectController.class, PostMapping.class);
            long get = count(AdminReviewController.class, GetMapping.class)
                    + count(AdminPipelineController.class, GetMapping.class)
                    + count(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post + get).isEqualTo(46);
        }
        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }

    // H. ProcessBuilder / Runtime
    @Nested @DisplayName("H. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : RPE006_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
