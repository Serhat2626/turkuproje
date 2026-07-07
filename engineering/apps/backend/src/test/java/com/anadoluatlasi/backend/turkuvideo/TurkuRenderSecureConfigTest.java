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
 * FP-024 Task 02: Secure Render Config Model + Validation tests.
 */
@DisplayName("FP-024 Task 02 — Secure Render Config Model + Validation")
class TurkuRenderSecureConfigTest {

    private TurkuRenderConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TurkuRenderConfigValidator();
    }

    private TurkuRenderSecureConfig validConfig() {
        return new TurkuRenderSecureConfig(true, "ffmpeg", "workspace", "temp",
                120, 600, 500, "1920x1080", 30,
                Set.of("mp4", "webm"), Set.of("h264", "vp9"));
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
        @DisplayName("no PUT/DELETE")
        void noUnsafe() {
            assertThat(countMethods(TurkuVideoProjectController.class, PutMapping.class)).isEqualTo(0);
            assertThat(countMethods(TurkuVideoProjectController.class, DeleteMapping.class)).isEqualTo(0);
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── B. Default Config ──

    @Nested
    @DisplayName("B. Default Config")
    class DefaultConfig {

        @Test
        @DisplayName("default config is disabled")
        void defaultDisabled() {
            var config = new TurkuRenderSecureConfig();
            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("default config validates as disabled")
        void defaultValidation() {
            var result = validator.validate(new TurkuRenderSecureConfig());
            assertThat(result.isValid()).isFalse();
            assertThat(result.getResolvedMode()).isEqualTo(TurkuRenderConfigMode.FFMPEG_DISABLED);
        }
    }

    // ── C. Missing Config Fails Closed ──

    @Nested
    @DisplayName("C. Missing Config Fails Closed")
    class MissingConfig {

        @Test
        @DisplayName("null config returns CONFIG_MISSING")
        void nullConfig() {
            var result = validator.validate(null);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getResolvedMode()).isEqualTo(TurkuRenderConfigMode.FFMPEG_CONFIG_MISSING);
        }

        @Test
        @DisplayName("missing has issue")
        void missingHasIssue() {
            var result = validator.validate(null);
            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getCode())
                    .isEqualTo(TurkuRenderConfigFailureCode.CONFIG_MISSING);
        }
    }

    // ── D. Valid Config ──

    @Nested
    @DisplayName("D. Valid Config")
    class ValidConfig {

        @Test
        @DisplayName("valid config passes")
        void valid() {
            var result = validator.validate(validConfig());
            assertThat(result.isValid()).isTrue();
            assertThat(result.getResolvedMode()).isEqualTo(TurkuRenderConfigMode.FFMPEG_CONFIG_VALID);
        }

        @Test
        @DisplayName("no issues on valid config")
        void noIssues() {
            var result = validator.validate(validConfig());
            assertThat(result.getIssues()).isEmpty();
        }
    }

    // ── E. Bounds Validation ──

    @Nested
    @DisplayName("E. Bounds Validation")
    class BoundsValidation {

        @Test
        @DisplayName("timeout below min rejected")
        void timeoutLow() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    5, 600, 500, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues().stream().map(i -> i.getCode()))
                    .contains(TurkuRenderConfigFailureCode.TIMEOUT_OUT_OF_RANGE);
        }

        @Test
        @DisplayName("timeout above max rejected")
        void timeoutHigh() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    999, 600, 500, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("duration out of range rejected")
        void durationBad() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    120, 9999, 500, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("size out of range rejected")
        void sizeBad() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    120, 600, 9999, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("fps out of range rejected")
        void fpsBad() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    120, 600, 500, "1920x1080", 120, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("resolution over max rejected")
        void resolutionBad() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    120, 600, 500, "7680x4320", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("invalid resolution format rejected")
        void resolutionFormat() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    120, 600, 500, "not-a-resolution", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("disallowed format rejected")
        void formatBad() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "tmp",
                    120, 600, 500, "1920x1080", 30, Set.of("avi"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }
    }

    // ── F. Shell/Path/URL/Secret Rejection ──

    @Nested
    @DisplayName("F. Dangerous Pattern Rejection")
    class PatternRejection {

        @Test
        @DisplayName("shell metacharacters detected")
        void shell() {
            assertThat(validator.containsShellMetacharacters("ffmpeg;rm -rf /")).isTrue();
            assertThat(validator.containsShellMetacharacters("ffmpeg&&cat")).isTrue();
            assertThat(validator.containsShellMetacharacters("$(cmd)")).isTrue();
            assertThat(validator.containsShellMetacharacters("ffmpeg")).isFalse();
        }

        @Test
        @DisplayName("path traversal detected")
        void pathTraversal() {
            assertThat(validator.containsPathTraversal("../../etc/passwd")).isTrue();
            assertThat(validator.containsPathTraversal("/usr/../../../root")).isTrue();
            assertThat(validator.containsPathTraversal("workspace")).isFalse();
        }

        @Test
        @DisplayName("URL detected")
        void url() {
            assertThat(validator.containsUrl("https://evil.com/cmd")).isTrue();
            assertThat(validator.containsUrl("s3://bucket/key")).isTrue();
            assertThat(validator.containsUrl("workspace")).isFalse();
        }

        @Test
        @DisplayName("secret detected")
        void secret() {
            assertThat(validator.containsSecret("password=abc")).isTrue();
            assertThat(validator.containsSecret("Bearer token")).isTrue();
            assertThat(validator.containsSecret("ffmpeg")).isFalse();
        }

        @Test
        @DisplayName("storage key detected")
        void storageKey() {
            assertThat(validator.containsStorageKey("storageKey=abc")).isTrue();
            assertThat(validator.containsStorageKey("publicUrl=x")).isTrue();
            assertThat(validator.containsStorageKey("workspace")).isFalse();
        }

        @Test
        @DisplayName("base64 detected")
        void base64() {
            assertThat(validator.containsBase64("data:video/mp4;base64,abc")).isTrue();
            assertThat(validator.containsBase64("workspace")).isFalse();
        }

        @Test
        @DisplayName("shell in executable token rejected")
        void shellInExec() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg;rm", "ws", "tmp",
                    120, 600, 500, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues().stream().map(i -> i.getCode()))
                    .contains(TurkuRenderConfigFailureCode.SHELL_METACHARACTER_DETECTED);
        }

        @Test
        @DisplayName("traversal in workspace rejected")
        void traversalInWs() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "../../etc", "tmp",
                    120, 600, 500, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("URL in temp rejected")
        void urlInTemp() {
            var config = new TurkuRenderSecureConfig(true, "ffmpeg", "ws", "https://evil.com",
                    120, 600, 500, "1920x1080", 30, Set.of("mp4"), Set.of("h264"));
            var result = validator.validate(config);
            assertThat(result.isValid()).isFalse();
        }
    }

    // ── G. Safety Envelope ──

    @Nested
    @DisplayName("G. Safety Envelope")
    class SafetyEnvelopeTests {

        @Test
        @DisplayName("envelope immutable flags")
        void flags() {
            var envelope = new TurkuRenderConfigSafetyEnvelope(TurkuRenderConfigMode.STUB_ONLY);
            assertThat(envelope.isHumanReviewRequired()).isTrue();
            assertThat(envelope.isCopyrightReviewRequired()).isTrue();
            assertThat(envelope.isAutoApproved()).isFalse();
            assertThat(envelope.isNotPublishReady()).isTrue();
            assertThat(envelope.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("safety flags list")
        void flagsList() {
            var envelope = new TurkuRenderConfigSafetyEnvelope(TurkuRenderConfigMode.FFMPEG_CONFIG_VALID);
            assertThat(envelope.getSafetyFlags()).contains(
                    "NO_PUBLISH", "NO_EXPORT", "NO_DOWNLOAD", "INTERNAL_ONLY");
        }
    }

    // ── H. Config Mode Enum ──

    @Nested
    @DisplayName("H. Config Mode Enum")
    class ConfigModeEnum {

        @Test
        @DisplayName("6 values")
        void count() {
            assertThat(TurkuRenderConfigMode.values()).hasSize(6);
        }

        @Test
        @DisplayName("no forbidden modes")
        void noForbidden() {
            var names = Arrays.stream(TurkuRenderConfigMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED", "EXPORTED", "AUTO_APPROVED");
        }
    }

    // ── I. Failure Code Enum ──

    @Nested
    @DisplayName("I. Failure Code Enum")
    class FailureCodeEnum {

        @Test
        @DisplayName("19 values")
        void count() {
            assertThat(TurkuRenderConfigFailureCode.values()).hasSize(19);
        }

        @Test
        @DisplayName("key codes present")
        void keyCodes() {
            var names = Arrays.stream(TurkuRenderConfigFailureCode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).contains(
                    "CONFIG_MISSING", "SHELL_METACHARACTER_DETECTED",
                    "PATH_TRAVERSAL_DETECTED", "URL_DETECTED",
                    "SECRET_DETECTED", "STORAGE_KEY_DETECTED");
        }
    }

    // ── J. DTO Safety ──

    @Nested
    @DisplayName("J. DTO Safety — Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawRendererPayload", "token", "jwt", "session",
                "credential", "password", "secret", "apiKey", "authorizationHeader",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "binaryData",
                "renderedVideo", "videoFile", "downloadFile",
                "filePath", "renderedVideoUrl", "commandLine", "ffmpegCommand",
                "shellCommand", "absolutePath", "processBuilder");

        private static final List<Class<?>> CLASSES = List.of(
                TurkuRenderSecureConfig.class,
                TurkuRenderConfigValidator.class,
                TurkuRenderConfigValidationResult.class,
                TurkuRenderConfigValidationIssue.class,
                TurkuRenderConfigSafetyEnvelope.class,
                TurkuRenderConfigMode.class,
                TurkuRenderConfigFailureCode.class);

        @Test
        @DisplayName("7 classes x 29 forbidden fields = 0 violations")
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

    // ── K. Stub-Only Factory ──

    @Nested
    @DisplayName("K. Stub-Only Factory")
    class StubFactory {

        @Test
        @DisplayName("stubOnly result")
        void stubOnly() {
            var result = TurkuRenderConfigValidationResult.stubOnly();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getResolvedMode()).isEqualTo(TurkuRenderConfigMode.STUB_ONLY);
            assertThat(result.getIssues()).isEmpty();
        }
    }
}
