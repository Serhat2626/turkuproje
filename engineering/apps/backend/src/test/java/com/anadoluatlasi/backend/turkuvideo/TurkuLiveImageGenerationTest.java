package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LIVE-002 — Manual-Gated Real Image Generation")
class TurkuLiveImageGenerationTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<?>> LIVE002_CLASSES = List.of(
            TurkuLiveImageResultMetadata.class, TurkuLiveGenerationResult.class,
            TurkuLiveImageGenerationService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED");

    private TurkuLiveImageGenerationService service;

    @BeforeEach void setUp() { service = new TurkuLiveImageGenerationService(); }

    private TurkuImagePromptPackageResult promptPkg() {
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

    private TurkuLiveProviderReadinessReport readyProvider() {
        return new TurkuLiveProviderReadinessService()
                .evaluate(true, true, new TurkuKillSwitch(false, "test", "OPERATOR"));
    }

    private TurkuLiveProviderReadinessReport killSwitchProvider() {
        return new TurkuLiveProviderReadinessService()
                .evaluate(true, true, TurkuKillSwitch.defaultActive());
    }

    private TurkuLiveProviderReadinessReport noCredProvider() {
        return new TurkuLiveProviderReadinessService()
                .evaluate(false, false, new TurkuKillSwitch(false, "test", "OPERATOR"));
    }

    // A. CI Fake Generation
    @Nested @DisplayName("A. CI Fake Generation")
    class CiFake {
        @Test @DisplayName("fake results available")
        void fake() {
            var r = service.executeFake(promptPkg());
            assertThat(r.getStatus()).isEqualTo(TurkuLiveGenerationStatus.FAKE_RESULT_AVAILABLE);
            assertThat(r.getMode()).isEqualTo(TurkuLiveGenerationMode.CI_FAKE);
            assertThat(r.getResults()).hasSize(4);
        }
        @Test @DisplayName("null prompt -> FAILED_SAFE")
        void nullPrompt() {
            var r = service.executeFake(null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveGenerationStatus.FAILED_SAFE);
        }
    }

    // B. Kill Switch Blocks Real
    @Nested @DisplayName("B. Kill Switch Blocks Real Provider")
    class KillBlock {
        @Test @DisplayName("kill switch active -> BLOCKED_BY_KILL_SWITCH")
        void blocked() {
            var r = service.execute(promptPkg(), killSwitchProvider(), true);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveGenerationStatus.BLOCKED_BY_KILL_SWITCH);
            assertThat(r.isKillSwitchActive()).isTrue();
            assertThat(r.getResults()).isEmpty();
        }
        @Test @DisplayName("null readiness -> kill switch active")
        void nullReadiness() {
            var r = service.execute(promptPkg(), null, true);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveGenerationStatus.BLOCKED_BY_KILL_SWITCH);
        }
    }

    // C. Operator Gate Blocks
    @Nested @DisplayName("C. Operator Gate Blocks")
    class OpBlock {
        @Test @DisplayName("operator gate not passed -> BLOCKED_BY_OPERATOR_GATE")
        void blocked() {
            var r = service.execute(promptPkg(), readyProvider(), false);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveGenerationStatus.BLOCKED_BY_OPERATOR_GATE);
            assertThat(r.isOperatorGatePassed()).isFalse();
        }
    }

    // D. Provider Readiness Blocks
    @Nested @DisplayName("D. Provider Readiness Blocks")
    class ProvBlock {
        @Test @DisplayName("provider not ready -> BLOCKED_BY_PROVIDER_READINESS")
        void blocked() {
            var r = service.execute(promptPkg(), noCredProvider(), true);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveGenerationStatus.BLOCKED_BY_PROVIDER_READINESS);
            assertThat(r.isProviderReady()).isFalse();
        }
    }

    // E. All Gates Passed — Real Provider
    @Nested @DisplayName("E. All Gates Passed — Real Provider")
    class AllGates {
        @Test @DisplayName("REAL_PROVIDER_RESULT_AVAILABLE")
        void realResult() {
            var r = service.execute(promptPkg(), readyProvider(), true);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveGenerationStatus.REAL_PROVIDER_RESULT_AVAILABLE);
            assertThat(r.getMode()).isEqualTo(TurkuLiveGenerationMode.LOCAL_MANUAL_REAL_PROVIDER);
            assertThat(r.getResults()).hasSize(4);
            assertThat(r.getOpaqueGenerationRef()).startsWith("lgen:");
        }
        @Test @DisplayName("all results require human review")
        void humanReview() {
            var r = service.execute(promptPkg(), readyProvider(), true);
            for (var res : r.getResults()) {
                assertThat(res.isHumanReviewRequired()).isTrue();
                assertThat(res.getSanitizedProviderSummary()).contains("gizli");
            }
        }
    }

    // F. Forbidden Sweep
    @Nested @DisplayName("F. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : LIVE002_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // G. No Forbidden Statuses
    @Nested @DisplayName("G. No Forbidden Statuses")
    class NoForbidden {
        @Test @DisplayName("status safe")
        void safe() {
            for (var v : TurkuLiveGenerationStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("7 statuses")
        void count() { assertThat(TurkuLiveGenerationStatus.values()).hasSize(7); }
        @Test @DisplayName("2 modes")
        void modes() { assertThat(TurkuLiveGenerationMode.values()).hasSize(2); }
    }

    // H. Endpoint Unchanged
    @Nested @DisplayName("H. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // I. ProcessBuilder / Runtime
    @Nested @DisplayName("I. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : LIVE002_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
