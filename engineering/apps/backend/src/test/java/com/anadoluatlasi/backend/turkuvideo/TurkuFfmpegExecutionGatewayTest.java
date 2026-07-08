package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-024 Task 04: Controlled FFmpeg Execution Boundary tests.
 */
@DisplayName("FP-024 Task 04 — Controlled FFmpeg Execution Behind Disabled Gate")
class TurkuFfmpegExecutionGatewayTest {

    private TurkuFfmpegExecutionGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new TurkuFfmpegExecutionGateway();
    }

    private TurkuRenderSecureConfig validConfig() {
        return new TurkuRenderSecureConfig(true, "ffmpeg", "workspace", "temp",
                120, 600, 500, "1920x1080", 30,
                Set.of("mp4", "webm"), Set.of("h264", "vp9"));
    }

    private TurkuRenderExecutionPlan validPlan() {
        return new TurkuRenderExecutionPlan(
                UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "mp4",
                60.0, "deterministic-stub", "DETERMINISTIC_STUB");
    }

    private TurkuFfmpegExecutionRequest validRequest() {
        var config = validConfig();
        var wsRef = new TurkuRenderWorkspaceRef();
        var builder = new TurkuFfmpegCommandBuilder(new TurkuRenderAdapterPolicy());
        var cmdPlan = builder.buildPlan(validPlan(), new TurkuFfmpegExecutionLimits());
        return new TurkuFfmpegExecutionRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                config, wsRef, cmdPlan, validPlan());
    }

    // ── A. Endpoint Inventory ──

    @Nested
    @DisplayName("A. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test
        @DisplayName("46 = 26 POST + 20 GET")
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
            assertThat(post).isEqualTo(26);
            assertThat(get).isEqualTo(20);
        }

        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }

    // ── B. Default Mode ──

    @Nested
    @DisplayName("B. Default Mode — Disabled")
    class DefaultMode {
        @Test
        @DisplayName("default mode is DISABLED")
        void disabled() {
            assertThat(gateway.getDefaultMode()).isEqualTo(TurkuFfmpegExecutionMode.DISABLED);
        }
    }

    // ── C. Disabled/Config-Missing Execution ──

    @Nested
    @DisplayName("C. Disabled / Config-Missing Execution")
    class DisabledExecution {
        @Test
        @DisplayName("null request fails safe")
        void nullRequest() {
            var result = gateway.execute(null);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureCode()).isEqualTo(TurkuFfmpegExecutionFailureCode.GENERAL_FAILURE);
        }

        @Test
        @DisplayName("null config returns CONFIG_MISSING")
        void nullConfig() {
            var req = new TurkuFfmpegExecutionRequest(
                    UUID.randomUUID(), UUID.randomUUID(), null,
                    new TurkuRenderWorkspaceRef(), null, null);
            var result = gateway.execute(req);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getExecutionMode()).isEqualTo(TurkuFfmpegExecutionMode.CONFIG_MISSING);
        }

        @Test
        @DisplayName("disabled config returns DISABLED")
        void disabledConfig() {
            var req = new TurkuFfmpegExecutionRequest(
                    UUID.randomUUID(), UUID.randomUUID(),
                    new TurkuRenderSecureConfig(),
                    new TurkuRenderWorkspaceRef(), null, null);
            var result = gateway.execute(req);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getExecutionMode()).isEqualTo(TurkuFfmpegExecutionMode.DISABLED);
        }
    }

    // ── D. Config Invalid ──

    @Nested
    @DisplayName("D. Invalid Config Fails Safely")
    class ConfigInvalid {
        @Test
        @DisplayName("bad timeout → CONFIG_INVALID")
        void badTimeout() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    5, 600, 500, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var req = new TurkuFfmpegExecutionRequest(
                    UUID.randomUUID(), UUID.randomUUID(), config,
                    new TurkuRenderWorkspaceRef(), null, null);
            var result = gateway.execute(req);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getExecutionMode()).isEqualTo(TurkuFfmpegExecutionMode.CONFIG_INVALID);
        }

        @Test
        @DisplayName("shell in executable → CONFIG_INVALID")
        void shellExec() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg;rm", "ws", "tmp",
                    120, 600, 500, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var req = new TurkuFfmpegExecutionRequest(
                    UUID.randomUUID(), UUID.randomUUID(), config,
                    new TurkuRenderWorkspaceRef(), null, null);
            var result = gateway.execute(req);
            assertThat(result.isSuccess()).isFalse();
        }
    }

    // ── E. Workspace Invalid ──

    @Nested
    @DisplayName("E. Invalid Workspace/File Refs Fail Safely")
    class WorkspaceInvalid {
        @Test
        @DisplayName("null workspace ref fails")
        void nullWs() {
            var req = new TurkuFfmpegExecutionRequest(
                    UUID.randomUUID(), UUID.randomUUID(), validConfig(),
                    null, null, null);
            var result = gateway.execute(req);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureCode()).isEqualTo(TurkuFfmpegExecutionFailureCode.WORKSPACE_INVALID);
        }

        @Test
        @DisplayName("traversal in workspace ref fails")
        void traversalWs() {
            var ws = new TurkuRenderWorkspaceRef("id", "../../etc/passwd");
            var req = new TurkuFfmpegExecutionRequest(
                    UUID.randomUUID(), UUID.randomUUID(), validConfig(),
                    ws, null, null);
            var result = gateway.execute(req);
            assertThat(result.isSuccess()).isFalse();
        }
    }

    // ── F. Command Plan Validation ──

    @Nested
    @DisplayName("F. Command Plan Validation")
    class CommandPlanValidation {
        @Test
        @DisplayName("null command plan fails")
        void nullPlan() {
            var req = new TurkuFfmpegExecutionRequest(
                    UUID.randomUUID(), UUID.randomUUID(), validConfig(),
                    new TurkuRenderWorkspaceRef(), null, null);
            var result = gateway.execute(req);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureCode()).isEqualTo(TurkuFfmpegExecutionFailureCode.COMMAND_PLAN_INVALID);
        }

        @Test
        @DisplayName("valid command plan dry-run succeeds")
        void validPlan() {
            var result = gateway.execute(validRequest());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getExecutionMode()).isEqualTo(TurkuFfmpegExecutionMode.DRY_RUN);
        }
    }

    // ── G. Dry-Run ──

    @Nested
    @DisplayName("G. Dry-Run Behavior")
    class DryRun {
        @Test
        @DisplayName("dry-run returns opaque artifact ref")
        void opaqueRef() {
            var result = gateway.execute(validRequest());
            assertThat(result.getOpaqueArtifactRef()).startsWith("internal-dryrun-artifact:");
        }

        @Test
        @DisplayName("dry-run returns trace ref")
        void traceRef() {
            var result = gateway.execute(validRequest());
            assertThat(result.getSafeTraceRef()).startsWith("ffmpeg-dryrun-trace:");
        }

        @Test
        @DisplayName("dry-run validation notes present")
        void notes() {
            var result = gateway.execute(validRequest());
            assertThat(result.getSafetyEnvelope().getValidationNotes()).isNotEmpty();
            assertThat(result.getSafetyEnvelope().getValidationNotes())
                    .anyMatch(n -> n.contains("DRY_RUN"));
        }
    }

    // ── H. Simulated Execution ──

    @Nested
    @DisplayName("H. Simulated Execution")
    class SimulatedExecution {
        @Test
        @DisplayName("simulated success returns SIMULATED_SUCCESS")
        void simulatedOk() {
            var result = gateway.simulatedExecution(validRequest());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getExecutionMode()).isEqualTo(TurkuFfmpegExecutionMode.SIMULATED_SUCCESS);
        }

        @Test
        @DisplayName("simulated with bad config fails")
        void simulatedBadConfig() {
            var req = new TurkuFfmpegExecutionRequest(
                    UUID.randomUUID(), UUID.randomUUID(), null,
                    new TurkuRenderWorkspaceRef(), null, null);
            var result = gateway.simulatedExecution(req);
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("simulated returns opaque artifact ref")
        void opaqueRef() {
            var result = gateway.simulatedExecution(validRequest());
            assertThat(result.getOpaqueArtifactRef()).startsWith("internal-simulated-artifact:");
        }
    }

    // ── I. Safety Envelope ──

    @Nested
    @DisplayName("I. Safety Envelope Flags")
    class SafetyFlags {
        @Test
        @DisplayName("all envelopes have correct flags")
        void allEnvelopes() {
            List<TurkuFfmpegExecutionResult> results = List.of(
                    TurkuFfmpegExecutionResult.disabled(),
                    TurkuFfmpegExecutionResult.configMissing(),
                    TurkuFfmpegExecutionResult.configInvalid(List.of()),
                    TurkuFfmpegExecutionResult.dryRunSuccess(List.of(), 0),
                    TurkuFfmpegExecutionResult.simulatedSuccess(0),
                    TurkuFfmpegExecutionResult.failedSafe(
                            TurkuFfmpegExecutionFailureCode.GENERAL_FAILURE, "test"));

            for (var r : results) {
                var e = r.getSafetyEnvelope();
                assertThat(e.isHumanReviewRequired()).as("humanReview for %s", r.getExecutionMode()).isTrue();
                assertThat(e.isCopyrightReviewRequired()).as("copyrightReview for %s", r.getExecutionMode()).isTrue();
                assertThat(e.isAutoApproved()).as("autoApproved for %s", r.getExecutionMode()).isFalse();
                assertThat(e.isNotPublishReady()).as("notPublishReady for %s", r.getExecutionMode()).isTrue();
                assertThat(e.isNotExportReady()).as("notExportReady for %s", r.getExecutionMode()).isTrue();
            }
        }

        @Test
        @DisplayName("NO_PROCESS_EXECUTION in safety flags")
        void noProcessFlag() {
            var envelope = new TurkuFfmpegExecutionSafetyEnvelope(TurkuFfmpegExecutionMode.DISABLED);
            assertThat(envelope.getSafetyFlags()).contains("NO_PROCESS_EXECUTION");
        }
    }

    // ── J. No Process Execution ──

    @Nested
    @DisplayName("J. No Process Execution Fields")
    class NoProcess {
        private static final Set<String> PROCESS_FIELDS = Set.of(
                "processBuilder", "process", "runtime", "processHandle", "exec", "command");

        @Test
        @DisplayName("gateway has no process fields")
        void gatewayClean() {
            var fields = Arrays.stream(TurkuFfmpegExecutionGateway.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : PROCESS_FIELDS) {
                assertThat(fields).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("result has no process fields")
        void resultClean() {
            var fields = Arrays.stream(TurkuFfmpegExecutionResult.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : PROCESS_FIELDS) {
                assertThat(fields).doesNotContain(f);
            }
        }
    }

    // ── K. DTO Safety ──

    @Nested
    @DisplayName("K. DTO Safety — Forbidden Fields")
    class DtoSafety {
        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "token", "jwt", "session", "credential",
                "password", "secret", "apiKey", "authorizationHeader",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "binaryData",
                "renderedVideo", "videoFile", "downloadFile",
                "filePath", "absolutePath", "renderedVideoUrl",
                "commandLine", "ffmpegCommand", "shellCommand", "processBuilder");

        private static final List<Class<?>> CLASSES = List.of(
                TurkuFfmpegExecutionMode.class,
                TurkuFfmpegExecutionFailureCode.class,
                TurkuFfmpegExecutionRequest.class,
                TurkuFfmpegExecutionResult.class,
                TurkuFfmpegExecutionSafetyEnvelope.class,
                TurkuFfmpegExecutionGateway.class);

        @Test
        @DisplayName("6 classes x 29 forbidden fields = 0 violations")
        void allSafe() {
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

    // ── L. Enum Values ──

    @Nested
    @DisplayName("L. Enum Values")
    class Enums {
        @Test
        @DisplayName("execution mode has 6 values")
        void modeCount() {
            assertThat(TurkuFfmpegExecutionMode.values()).hasSize(6);
        }

        @Test
        @DisplayName("failure code has 13 values")
        void failureCount() {
            assertThat(TurkuFfmpegExecutionFailureCode.values()).hasSize(13);
        }

        @Test
        @DisplayName("no forbidden modes")
        void noForbiddenModes() {
            var names = Arrays.stream(TurkuFfmpegExecutionMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED", "EXPORTED", "AUTO_APPROVED", "REAL_EXECUTION");
        }
    }

    // ── M. Stub Backward Compatibility ──

    @Nested
    @DisplayName("M. Stub Runner Backward Compatibility")
    class StubCompat {
        @Test
        @DisplayName("stub runner still works")
        void stubWorks() {
            var stub = new StubTurkuRenderRunnerGateway();
            var result = stub.execute(validPlan());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getArtifact()).isNotNull();
        }

        @Test
        @DisplayName("stub is default via resolver")
        void stubDefault() {
            var resolver = new TurkuRenderRunnerResolver(new StubTurkuRenderRunnerGateway());
            assertThat(resolver.isStubActive()).isTrue();
            assertThat(resolver.isFfmpegActive()).isFalse();
        }
    }
}
