package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RPE-008 — Real Production Execution MVP Final Closure")
class TurkuRpePhaseClosureTest {

    private static final List<String> FORBIDDEN_FIELDS = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED",
            "PROVIDER_ENABLED", "IMAGE_GENERATION_READY");

    private static final List<Class<?>> ALL_RPE_CLASSES = List.of(
            // RPE-001
            TurkuExecutionGateResult.class, TurkuExecutionGateService.class,
            // RPE-002
            TurkuProviderConfigResult.class, TurkuProviderConfigService.class,
            // RPE-003
            TurkuRenderEnvironmentBoundary.class, TurkuFfmpegCapabilityMetadata.class,
            TurkuOutputFormatPolicy.class, TurkuRenderPipelineConfigResult.class, TurkuRenderPipelineConfigService.class,
            // RPE-004
            TurkuManualGate.class, TurkuExecutionPackageResult.class, TurkuExecutionPackageService.class,
            // RPE-005
            TurkuSceneImagePrompt.class, TurkuImagePromptPackageResult.class, TurkuImagePromptPackageService.class,
            // RPE-006
            TurkuFakeImageResultMetadata.class, TurkuImageGenerationExecutionResult.class, TurkuImageGenerationExecutionService.class,
            // RPE-007
            TurkuSceneArtifactReadiness.class, TurkuArtifactVerificationReport.class,
            TurkuRenderPackageResult.class, TurkuArtifactVerificationService.class);

    @SuppressWarnings("rawtypes")
    private static final List<Class<? extends Enum>> ALL_RPE_ENUMS = List.of(
            TurkuExecutionGateStatus.class, TurkuExecutionGateIssueCode.class,
            TurkuProviderConfigStatus.class, TurkuProviderConfigIssueCode.class,
            TurkuRenderConfigStatus.class, TurkuRenderConfigIssueCode.class,
            TurkuExecutionPackageStatus.class, TurkuExecutionPackageIssueCode.class,
            TurkuImagePromptPackageStatus.class, TurkuImagePromptPackageIssueCode.class,
            TurkuImageGenerationExecutionMode.class, TurkuImageGenerationStatus.class, TurkuImageGenerationIssueCode.class,
            TurkuArtifactVerificationStatus.class, TurkuRenderPackageStatus.class);

    // ── Full E2E chain helper ──
    private TurkuRenderPackageResult fullChain() {
        var req = new TurkuNameStoryboardRequestService().createRequest(
                new TurkuNameStoryboardRequest("Canakkale", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
        var e = new TurkuManualEnablementService().evaluate(r);
        var wo = new TurkuWorkOrderPreviewService().generate(e);
        // RPE-001
        var gate = new TurkuExecutionGateService().evaluate(wo);
        // RPE-002
        var prov = new TurkuProviderConfigService().evaluate(gate);
        // RPE-003
        var render = new TurkuRenderPipelineConfigService().evaluate(prov);
        // RPE-004
        var exec = new TurkuExecutionPackageService().assemble(wo, prov, render);
        // RPE-005
        var prompt = new TurkuImagePromptPackageService().prepare(exec);
        // RPE-006
        var gen = new TurkuImageGenerationExecutionService().execute(prompt);
        // RPE-007
        var verf = new TurkuArtifactVerificationService();
        var report = verf.verify(gen);
        return verf.prepareRenderPackage(report);
    }

    // A. Full E2E Chain
    @Nested @DisplayName("A. Full RPE E2E Chain")
    class E2EChain {
        @Test @DisplayName("chain produces render package with WAITING_FOR_MANUAL_GATE")
        void chain() {
            var r = fullChain();
            assertThat(r).isNotNull();
            assertThat(r.getStatus()).isEqualTo(TurkuRenderPackageStatus.WAITING_FOR_MANUAL_GATE);
            assertThat(r.getOpaqueRenderPackageRef()).startsWith("rpkg:");
        }
        @Test @DisplayName("local/manual render disabled")
        void renderDisabled() {
            assertThat(fullChain().isLocalManualRenderEnabled()).isFalse();
        }
        @Test @DisplayName("render prerequisites present")
        void prereqs() {
            assertThat(fullChain().getRenderPrerequisiteLabels()).hasSizeGreaterThanOrEqualTo(5);
        }
    }

    // B. Forbidden Field Sweep — All RPE Classes
    @Nested @DisplayName("B. Forbidden Field Sweep (all RPE classes)")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields across 22 RPE classes")
        void sweep() {
            for (var c : ALL_RPE_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN_FIELDS)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // C. Forbidden Status Sweep — All RPE Enums
    @Nested @DisplayName("C. Forbidden Status Sweep (all RPE enums)")
    class ForbiddenStatusSweep {
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Test @DisplayName("0 forbidden statuses across 15 RPE enums")
        void sweep() {
            for (Class<? extends Enum> ec : ALL_RPE_ENUMS) {
                for (Enum<?> v : ec.getEnumConstants())
                    for (String f : FORBIDDEN_STATUSES)
                        assertThat(v.name()).as("%s.%s", ec.getSimpleName(), v.name()).isNotEqualTo(f);
            }
        }
    }

    // D. ProcessBuilder / Runtime Sweep
    @Nested @DisplayName("D. ProcessBuilder / Runtime Sweep")
    class ProcessSweep {
        @Test @DisplayName("no ProcessBuilder/Runtime in RPE classes")
        void clean() {
            for (var c : ALL_RPE_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName()).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // E. Endpoint Inventory Unchanged
    @Nested @DisplayName("E. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("POST count = 26")
        void post() {
            long post = count(AdminReviewController.class, PostMapping.class)
                    + count(AdminPipelineController.class, PostMapping.class)
                    + count(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + count(TurkuVideoProjectController.class, PostMapping.class);
            assertThat(post).isEqualTo(26);
        }
        @Test @DisplayName("GET count = 20")
        void get() {
            long get = count(AdminReviewController.class, GetMapping.class)
                    + count(AdminPipelineController.class, GetMapping.class)
                    + count(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(get).isEqualTo(20);
        }
        @Test @DisplayName("total = 46")
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

    // F. Execution Mode Default
    @Nested @DisplayName("F. Execution Mode Defaults")
    class ModeDefaults {
        @Test @DisplayName("CI_FAKE is default generation mode")
        void genMode() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", "Test", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
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
            var gen = new TurkuImageGenerationExecutionService().execute(prompt);
            assertThat(gen.getExecutionMode()).isEqualTo(TurkuImageGenerationExecutionMode.CI_FAKE);
            assertThat(gen.isManualGatePassed()).isFalse();
        }
    }

    // G. Manual Gates Never Auto-Approved
    @Nested @DisplayName("G. Manual Gates Never Auto-Approved")
    class ManualGates {
        @Test @DisplayName("execution package gates all unpassed")
        void execGates() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", "Test", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
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
            for (var g : exec.getManualGates()) assertThat(g.isPassed()).isFalse();
        }
        @Test @DisplayName("prompt package approval not granted")
        void promptApproval() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", "Test", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
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
            assertThat(prompt.isManualApprovalGranted()).isFalse();
        }
    }
}
