package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RPE-007 — Image Artifact Verification + Render Package Boundary")
class TurkuArtifactVerificationTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<?>> RPE007_CLASSES = List.of(
            TurkuSceneArtifactReadiness.class, TurkuArtifactVerificationReport.class,
            TurkuRenderPackageResult.class, TurkuArtifactVerificationService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED");

    private TurkuArtifactVerificationService service;

    @BeforeEach void setUp() { service = new TurkuArtifactVerificationService(); }

    private TurkuImageGenerationExecutionResult genResult() {
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
        var prompt = new TurkuImagePromptPackageService().prepare(exec);
        return new TurkuImageGenerationExecutionService().execute(prompt);
    }

    // A. Artifact Verification
    @Nested @DisplayName("A. Artifact Verification")
    class Verification {
        @Test @DisplayName("FAKE_VERIFIED")
        void verified() {
            var r = service.verify(genResult());
            assertThat(r.getStatus()).isEqualTo(TurkuArtifactVerificationStatus.FAKE_VERIFIED);
            assertThat(r.getOpaqueVerificationRef()).startsWith("avrf:");
            assertThat(r.getVerifiedScenes()).isEqualTo(4);
        }
        @Test @DisplayName("scene readiness mapped")
        void readiness() {
            var r = service.verify(genResult());
            assertThat(r.getSceneReadiness()).hasSize(4);
            for (var s : r.getSceneReadiness()) {
                assertThat(s.isVerified()).isTrue();
                assertThat(s.getOpaqueArtifactRef()).startsWith("fimg:");
            }
        }
    }

    // B. Render Package
    @Nested @DisplayName("B. Render Package")
    class RenderPkg {
        @Test @DisplayName("WAITING_FOR_MANUAL_GATE")
        void waiting() {
            var v = service.verify(genResult());
            var r = service.prepareRenderPackage(v);
            assertThat(r.getStatus()).isEqualTo(TurkuRenderPackageStatus.WAITING_FOR_MANUAL_GATE);
            assertThat(r.getOpaqueRenderPackageRef()).startsWith("rpkg:");
        }
        @Test @DisplayName("local/manual render disabled")
        void disabled() {
            var v = service.verify(genResult());
            var r = service.prepareRenderPackage(v);
            assertThat(r.isLocalManualRenderEnabled()).isFalse();
            assertThat(r.getLocalManualRenderNote()).contains("RPE-007");
        }
        @Test @DisplayName("prerequisites listed")
        void prereqs() {
            var v = service.verify(genResult());
            var r = service.prepareRenderPackage(v);
            assertThat(r.getRenderPrerequisiteLabels()).hasSizeGreaterThanOrEqualTo(5);
        }
    }

    // C. Blocked
    @Nested @DisplayName("C. Blocked / Missing")
    class Blocked {
        @Test @DisplayName("null gen -> BLOCKED_BY_SAFETY verification")
        void nullGen() {
            var r = service.verify(null);
            assertThat(r.getStatus()).isEqualTo(TurkuArtifactVerificationStatus.BLOCKED_BY_SAFETY);
        }
        @Test @DisplayName("null verification -> BLOCKED render package")
        void nullVerf() {
            var r = service.prepareRenderPackage(null);
            assertThat(r.getStatus()).isEqualTo(TurkuRenderPackageStatus.BLOCKED_BY_SAFETY);
        }
    }

    // D. Forbidden Sweep
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : RPE007_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // E. No Forbidden Statuses
    @Nested @DisplayName("E. No Forbidden Statuses")
    class NoForbidden {
        @Test @DisplayName("verification status safe")
        void verf() {
            for (var v : TurkuArtifactVerificationStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("render package status safe")
        void pkg() {
            for (var v : TurkuRenderPackageStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("5 verification statuses")
        void vc() { assertThat(TurkuArtifactVerificationStatus.values()).hasSize(5); }
        @Test @DisplayName("5 render package statuses")
        void rc() { assertThat(TurkuRenderPackageStatus.values()).hasSize(5); }
    }

    // F. Endpoint Unchanged
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // G. ProcessBuilder / Runtime
    @Nested @DisplayName("G. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : RPE007_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
