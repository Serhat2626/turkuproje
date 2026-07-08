package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RPE-003 \u2014 Render Pipeline Configuration Boundary")
class TurkuRenderPipelineConfigTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "promptText", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<?>> RPE003_CLASSES = List.of(
            TurkuRenderEnvironmentBoundary.class, TurkuFfmpegCapabilityMetadata.class,
            TurkuOutputFormatPolicy.class, TurkuRenderPipelineConfigResult.class,
            TurkuRenderPipelineConfigService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED");

    private TurkuRenderPipelineConfigService service;

    @BeforeEach void setUp() { service = new TurkuRenderPipelineConfigService(); }

    private TurkuProviderConfigResult providerNotConfigured() {
        var req = new TurkuNameStoryboardRequestService().createRequest(
                new TurkuNameStoryboardRequest("\u00c7anakkale", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
        var e = new TurkuManualEnablementService().evaluate(r);
        var wo = new TurkuWorkOrderPreviewService().generate(e);
        var gate = new TurkuExecutionGateService().evaluate(wo);
        return new TurkuProviderConfigService().evaluate(gate);
    }

    // A. Default NOT_CONFIGURED
    @Nested @DisplayName("A. Default NOT_CONFIGURED")
    class Default {
        @Test @DisplayName("provider config -> NOT_CONFIGURED")
        void notConfigured() {
            var r = service.evaluate(providerNotConfigured());
            assertThat(r.getStatus()).isEqualTo(TurkuRenderConfigStatus.NOT_CONFIGURED);
            assertThat(r.getOpaqueRenderConfigRef()).startsWith("rcfg:");
            assertThat(r.isRenderExecutionEnabled()).isFalse();
        }
        @Test @DisplayName("environment not configured")
        void env() {
            var r = service.evaluate(providerNotConfigured());
            assertThat(r.getEnvironmentBoundary()).isNotNull();
            assertThat(r.getEnvironmentBoundary().isEnvironmentConfigured()).isFalse();
        }
    }

    // B. FFmpeg Capabilities Metadata
    @Nested @DisplayName("B. FFmpeg Capabilities Metadata")
    class Ffmpeg {
        @Test @DisplayName("5 capabilities listed")
        void caps() {
            var r = service.evaluate(providerNotConfigured());
            assertThat(r.getFfmpegCapabilities()).hasSize(5);
        }
        @Test @DisplayName("all unavailable")
        void unavailable() {
            var r = service.evaluate(providerNotConfigured());
            for (var c : r.getFfmpegCapabilities()) {
                assertThat(c.isAvailable()).isFalse();
                assertThat(c.getAvailabilityNote()).contains("RPE-003");
            }
        }
        @Test @DisplayName("labels are strings")
        void labels() {
            var r = service.evaluate(providerNotConfigured());
            for (var c : r.getFfmpegCapabilities())
                assertThat(c.getCapabilityLabel()).isInstanceOf(String.class).isNotBlank();
        }
    }

    // C. Output Format Policy
    @Nested @DisplayName("C. Output Format Policy")
    class Output {
        @Test @DisplayName("default MP4 1920x1080")
        void policy() {
            var r = service.evaluate(providerNotConfigured());
            assertThat(r.getOutputFormatPolicy()).isNotNull();
            assertThat(r.getOutputFormatPolicy().getWidth()).isEqualTo(1920);
            assertThat(r.getOutputFormatPolicy().getHeight()).isEqualTo(1080);
            assertThat(r.getOutputFormatPolicy().getFrameRate()).isEqualTo(24);
        }
    }

    // D. Safety Prerequisites
    @Nested @DisplayName("D. Safety Prerequisites")
    class Prerequisites {
        @Test @DisplayName("5+ prerequisites")
        void count() {
            var r = service.evaluate(providerNotConfigured());
            assertThat(r.getSafetyPrerequisiteLabels()).hasSizeGreaterThanOrEqualTo(5);
        }
        @Test @DisplayName("labels are strings")
        void labels() {
            var r = service.evaluate(providerNotConfigured());
            for (var s : r.getSafetyPrerequisiteLabels())
                assertThat(s).isInstanceOf(String.class).isNotBlank();
        }
    }

    // E. Blocked
    @Nested @DisplayName("E. Blocked / Missing")
    class Blocked {
        @Test @DisplayName("null provider -> BLOCKED_BY_SAFETY")
        void nullProvider() {
            var r = service.evaluate(null);
            assertThat(r.getStatus()).isEqualTo(TurkuRenderConfigStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getIssues()).contains(TurkuRenderConfigIssueCode.INTERNAL_ERROR);
        }
    }

    // F. Forbidden Sweep
    @Nested @DisplayName("F. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : RPE003_CLASSES) {
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
        @Test @DisplayName("render config status safe")
        void safe() {
            for (var v : TurkuRenderConfigStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("exactly 5 statuses")
        void count() { assertThat(TurkuRenderConfigStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 7 issue codes")
        void issues() { assertThat(TurkuRenderConfigIssueCode.values()).hasSize(7); }
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
        @Test @DisplayName("no ProcessBuilder/Runtime in RPE-003 classes")
        void clean() {
            for (var c : RPE003_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // J. Validation Summary
    @Nested @DisplayName("J. Validation Summary Safety")
    class Validation {
        @Test @DisplayName("summary no forbidden text")
        void safe() {
            var r = service.evaluate(providerNotConfigured());
            for (var f : FORBIDDEN) assertThat(r.getValidationSummary()).doesNotContain(f);
        }
    }
}
