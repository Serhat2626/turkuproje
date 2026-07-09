package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LIVE-003 — Real Image Artifact Storage + Verification")
class TurkuLiveArtifactStorageTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<?>> LIVE003_CLASSES = List.of(
            TurkuLiveStoredArtifact.class, TurkuLiveStorageResult.class,
            TurkuLiveArtifactStorageService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED");

    private TurkuLiveArtifactStorageService service;

    @BeforeEach void setUp() { service = new TurkuLiveArtifactStorageService(); }

    private TurkuLiveGenerationResult fakeGen() {
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
        return new TurkuLiveImageGenerationService().executeFake(prompt);
    }

    // A. CI Fake Storage
    @Nested @DisplayName("A. CI Fake Storage")
    class FakeStorage {
        @Test @DisplayName("FAKE_STORED")
        void stored() {
            var r = service.storeFake(fakeGen());
            assertThat(r.getStatus()).isEqualTo(TurkuLiveStorageStatus.FAKE_STORED);
            assertThat(r.getOpaqueStorageRef()).startsWith("lstr:");
            assertThat(r.getStoredCount()).isEqualTo(4);
            assertThat(r.getVerifiedCount()).isEqualTo(4);
        }
        @Test @DisplayName("all artifacts verified")
        void verified() {
            var r = service.storeFake(fakeGen());
            for (var a : r.getStoredArtifacts()) {
                assertThat(a.isVerified()).isTrue();
                assertThat(a.getOpaqueInternalRef()).startsWith("sart:");
            }
        }
    }

    // B. Real Storage Manual Gate
    @Nested @DisplayName("B. Real Storage Manual Gate")
    class RealGate {
        @Test @DisplayName("gate not passed -> WAITING_FOR_MANUAL_GATE")
        void waiting() {
            var r = service.storeReal(fakeGen(), false);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveStorageStatus.WAITING_FOR_MANUAL_GATE);
            assertThat(r.getStoredArtifacts()).isEmpty();
        }
        @Test @DisplayName("gate passed -> REAL_STORED_INTERNAL")
        void stored() {
            var r = service.storeReal(fakeGen(), true);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveStorageStatus.REAL_STORED_INTERNAL);
            assertThat(r.getStoredCount()).isEqualTo(4);
            for (var a : r.getStoredArtifacts())
                assertThat(a.getOpaqueInternalRef()).startsWith("rsrt:");
        }
    }

    // C. Blocked / Missing
    @Nested @DisplayName("C. Blocked / Missing")
    class Blocked {
        @Test @DisplayName("null gen -> BLOCKED_BY_SAFETY")
        void nullGen() {
            var r = service.storeFake(null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveStorageStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getIssues()).contains(TurkuLiveStorageIssueCode.GENERATION_RESULT_MISSING);
        }
    }

    // D. Scene-to-Artifact Mapping
    @Nested @DisplayName("D. Scene-to-Artifact Mapping")
    class Mapping {
        @Test @DisplayName("4 scenes mapped")
        void mapped() {
            var r = service.storeFake(fakeGen());
            assertThat(r.getStoredArtifacts()).hasSize(4);
            assertThat(r.getTotalScenes()).isEqualTo(4);
        }
    }

    // E. Forbidden Sweep
    @Nested @DisplayName("E. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : LIVE003_CLASSES) {
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
        @Test @DisplayName("storage status safe")
        void safe() {
            for (var v : TurkuLiveStorageStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("6 statuses")
        void count() { assertThat(TurkuLiveStorageStatus.values()).hasSize(6); }
        @Test @DisplayName("5 issue codes")
        void issues() { assertThat(TurkuLiveStorageIssueCode.values()).hasSize(5); }
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
            for (var c : LIVE003_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
