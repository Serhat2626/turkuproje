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
 * FP-024 Task 03: Internal Render Workspace + File Safety tests.
 */
@DisplayName("FP-024 Task 03 — Internal Render Workspace + File Safety")
class TurkuRenderWorkspaceFileSafetyTest {

    private TurkuRenderWorkspaceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TurkuRenderWorkspaceValidator();
    }

    // ── A. Endpoint Inventory ──

    @Nested
    @DisplayName("A. Endpoint Inventory (44 = 25 POST + 19 GET)")
    class Endpoints {
        @Test
        @DisplayName("44 = 25 POST + 19 GET")
        void total() {
            long post = count(AdminReviewController.class, PostMapping.class)
                    + count(AdminPipelineController.class, PostMapping.class)
                    + count(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + count(TurkuVideoProjectController.class, PostMapping.class);
            long get = count(AdminReviewController.class, GetMapping.class)
                    + count(AdminPipelineController.class, GetMapping.class)
                    + count(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post + get).isEqualTo(44);
            assertThat(post).isEqualTo(25);
            assertThat(get).isEqualTo(19);
        }

        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }

    // ── B. Opaque Workspace Refs ──

    @Nested
    @DisplayName("B. Opaque Workspace Refs")
    class OpaqueWorkspaceRefs {
        @Test
        @DisplayName("default workspace ref is opaque")
        void defaultRef() {
            var ref = new TurkuRenderWorkspaceRef();
            assertThat(ref.getOpaqueRef()).startsWith("internal-workspace:");
            assertThat(ref.getWorkspaceId()).isNotBlank();
        }

        @Test
        @DisplayName("workspace ref validates OK")
        void validatesOk() {
            var ref = new TurkuRenderWorkspaceRef();
            var result = validator.validateWorkspaceRef(ref);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("null workspace ref fails")
        void nullFails() {
            var result = validator.validateWorkspaceRef(null);
            assertThat(result.isValid()).isFalse();
        }
    }

    // ── C. Opaque File Refs ──

    @Nested
    @DisplayName("C. Opaque File Refs")
    class OpaqueFileRefs {
        @Test
        @DisplayName("default file ref is opaque")
        void defaultRef() {
            var ref = new TurkuRenderSafeFileRef("mp4", "output");
            assertThat(ref.getOpaqueRef()).startsWith("internal-file:");
            assertThat(ref.getExtension()).isEqualTo("mp4");
        }

        @Test
        @DisplayName("file ref validates OK")
        void validatesOk() {
            var ref = new TurkuRenderSafeFileRef("mp4", "output");
            var result = validator.validateFileRef(ref);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("null file ref fails")
        void nullFails() {
            var result = validator.validateFileRef(null);
            assertThat(result.isValid()).isFalse();
        }
    }

    // ── D. Path Traversal Rejection ──

    @Nested
    @DisplayName("D. Path Traversal Rejection")
    class PathTraversal {
        @Test
        @DisplayName(".. detected")
        void dotDot() { assertThat(validator.isPathTraversal("../../etc/passwd")).isTrue(); }

        @Test
        @DisplayName("/../ detected")
        void slashDotDot() { assertThat(validator.isPathTraversal("/usr/../root")).isTrue(); }

        @Test
        @DisplayName("URL-encoded detected")
        void urlEncoded() { assertThat(validator.isPathTraversal("%2e%2e/etc")).isTrue(); }

        @Test
        @DisplayName("safe value passes")
        void safe() { assertThat(validator.isPathTraversal("internal-workspace:abc")).isFalse(); }

        @Test
        @DisplayName("traversal in opaque ref rejected")
        void inRef() {
            var result = validator.validateOpaqueRef("../../etc/passwd", "ref");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues().stream().map(i -> i.getCode()))
                    .contains(TurkuRenderWorkspaceIssue.Code.PATH_TRAVERSAL);
        }
    }

    // ── E. Absolute/Relative Path Rejection ──

    @Nested
    @DisplayName("E. Absolute/Relative Path Rejection")
    class AbsoluteRelative {
        @Test
        @DisplayName("Unix absolute path")
        void unix() { assertThat(validator.isAbsolutePath("/usr/bin/ffmpeg")).isTrue(); }

        @Test
        @DisplayName("Windows absolute path")
        void windows() { assertThat(validator.isWindowsPath("C:\\Users\\video.mp4")).isTrue(); }

        @Test
        @DisplayName("relative with slash")
        void relative() { assertThat(validator.isRelativePath("dir/file.mp4")).isTrue(); }

        @Test
        @DisplayName("relative with backslash")
        void relativeBack() { assertThat(validator.isRelativePath("dir\\file.mp4")).isTrue(); }

        @Test
        @DisplayName("opaque ref is not a path")
        void opaqueNotPath() {
            assertThat(validator.isAbsolutePath("internal-workspace:abc")).isFalse();
            assertThat(validator.isWindowsPath("internal-workspace:abc")).isFalse();
        }
    }

    // ── F. URL/Storage/Base64/Secret/Command Rejection ──

    @Nested
    @DisplayName("F. Pattern Rejection")
    class PatternRejection {
        @Test
        @DisplayName("URLs rejected")
        void urls() {
            assertThat(validator.isUrl("https://cdn.example.com/video.mp4")).isTrue();
            assertThat(validator.isUrl("s3://bucket/key")).isTrue();
            assertThat(validator.isUrl("file:///tmp/video.mp4")).isTrue();
        }

        @Test
        @DisplayName("storage keys rejected")
        void storage() {
            assertThat(validator.isStorageKey("storageKey=abc")).isTrue();
            assertThat(validator.isStorageKey("publicUrl=x")).isTrue();
            assertThat(validator.isStorageKey("cdnUrl=y")).isTrue();
        }

        @Test
        @DisplayName("base64 rejected")
        void base64() {
            assertThat(validator.isBase64("data:video/mp4;base64,abc")).isTrue();
            assertThat(validator.isBase64("data:image/png;base64,xyz")).isTrue();
        }

        @Test
        @DisplayName("secrets rejected")
        void secrets() {
            assertThat(validator.isSecret("password=abc")).isTrue();
            assertThat(validator.isSecret("Bearer token123")).isTrue();
            assertThat(validator.isSecret("Authorization: Bearer x")).isTrue();
        }

        @Test
        @DisplayName("commands rejected")
        void commands() {
            assertThat(validator.isCommand("commandLine=/bin/ffmpeg")).isTrue();
            assertThat(validator.isCommand("shellCommand=rm -rf")).isTrue();
            assertThat(validator.isCommand("/bin/bash -c cmd")).isTrue();
        }

        @Test
        @DisplayName("shell metacharacters rejected")
        void shellMeta() {
            assertThat(validator.isShellMeta("ref;rm -rf /")).isTrue();
            assertThat(validator.isShellMeta("ref&&cat /etc")).isTrue();
            assertThat(validator.isShellMeta("$(cmd)")).isTrue();
        }

        @Test
        @DisplayName("safe values pass all checks")
        void safeValue() {
            String safe = "internal-workspace:abc-123";
            assertThat(validator.isUrl(safe)).isFalse();
            assertThat(validator.isStorageKey(safe)).isFalse();
            assertThat(validator.isBase64(safe)).isFalse();
            assertThat(validator.isSecret(safe)).isFalse();
            assertThat(validator.isCommand(safe)).isFalse();
            assertThat(validator.isShellMeta(safe)).isFalse();
        }
    }

    // ── G. Extension Allowlist ──

    @Nested
    @DisplayName("G. Extension Allowlist")
    class ExtensionAllowlist {
        @Test
        @DisplayName("mp4 allowed")
        void mp4() {
            var policy = new TurkuRenderWorkspacePolicy();
            assertThat(policy.isExtensionAllowed("mp4")).isTrue();
        }

        @Test
        @DisplayName("webm allowed")
        void webm() {
            assertThat(new TurkuRenderWorkspacePolicy().isExtensionAllowed("webm")).isTrue();
        }

        @Test
        @DisplayName("png allowed")
        void png() {
            assertThat(new TurkuRenderWorkspacePolicy().isExtensionAllowed("png")).isTrue();
        }

        @Test
        @DisplayName("exe rejected")
        void exe() {
            assertThat(new TurkuRenderWorkspacePolicy().isExtensionAllowed("exe")).isFalse();
        }

        @Test
        @DisplayName("sh rejected")
        void sh() {
            assertThat(new TurkuRenderWorkspacePolicy().isExtensionAllowed("sh")).isFalse();
        }

        @Test
        @DisplayName("dot prefix handled")
        void dotPrefix() {
            assertThat(new TurkuRenderWorkspacePolicy().isExtensionAllowed(".mp4")).isTrue();
        }

        @Test
        @DisplayName("bad extension in file ref fails validation")
        void badExtInFileRef() {
            var ref = new TurkuRenderSafeFileRef("id", "internal-file:abc", "exe", "output");
            var result = validator.validateFileRef(ref);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues().stream().map(i -> i.getCode()))
                    .contains(TurkuRenderWorkspaceIssue.Code.EXTENSION_NOT_ALLOWED);
        }
    }

    // ── H. Workspace Model Safety ──

    @Nested
    @DisplayName("H. Workspace Model Safety")
    class WorkspaceModel {
        @Test
        @DisplayName("workspace has safe defaults")
        void defaults() {
            var ws = new TurkuRenderWorkspace(
                    new TurkuRenderWorkspaceRef(), new TurkuRenderWorkspacePolicy(),
                    UUID.randomUUID(), UUID.randomUUID());
            assertThat(ws.isHumanReviewRequired()).isTrue();
            assertThat(ws.isAutoApproved()).isFalse();
            assertThat(ws.isNotPublishReady()).isTrue();
            assertThat(ws.isNotExportReady()).isTrue();
            assertThat(ws.isCleanupEligible()).isTrue();
        }

        @Test
        @DisplayName("workspace policy bounded")
        void policyBounds() {
            var policy = new TurkuRenderWorkspacePolicy(null, 99999, 99999, false);
            assertThat(policy.getMaxOutputSizeMb()).isEqualTo(1000);
            assertThat(policy.getMaxFilesPerJob()).isEqualTo(50);
        }
    }

    // ── I. File Safety Report ──

    @Nested
    @DisplayName("I. File Safety Report")
    class FileSafetyReportTest {
        @Test
        @DisplayName("report immutable flags")
        void flags() {
            var report = new TurkuRenderFileSafetyReport("file-123", true);
            assertThat(report.isHumanReviewRequired()).isTrue();
            assertThat(report.isCopyrightReviewRequired()).isTrue();
            assertThat(report.isAutoApproved()).isFalse();
            assertThat(report.isNotPublishReady()).isTrue();
            assertThat(report.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("safety flags list")
        void flagsList() {
            var report = new TurkuRenderFileSafetyReport("file-123", true);
            assertThat(report.getSafetyFlags()).contains("NO_PUBLISH", "NO_EXPORT", "INTERNAL_ONLY");
        }
    }

    // ── J. DTO Safety ──

    @Nested
    @DisplayName("J. DTO Safety — Forbidden Fields")
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
                TurkuRenderWorkspace.class,
                TurkuRenderWorkspaceRef.class,
                TurkuRenderWorkspacePolicy.class,
                TurkuRenderWorkspaceValidationResult.class,
                TurkuRenderWorkspaceIssue.class,
                TurkuRenderSafeFileRef.class,
                TurkuRenderFileSafetyReport.class,
                TurkuRenderWorkspaceValidator.class);

        @Test
        @DisplayName("8 classes x 28 forbidden fields = 0 violations")
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

    // ── K. Issue Code Enum ──

    @Nested
    @DisplayName("K. Issue Code Enum")
    class IssueCodeEnum {
        @Test
        @DisplayName("13 codes")
        void count() {
            assertThat(TurkuRenderWorkspaceIssue.Code.values()).hasSize(13);
        }

        @Test
        @DisplayName("key codes present")
        void keyCodes() {
            var names = Arrays.stream(TurkuRenderWorkspaceIssue.Code.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).contains(
                    "PATH_TRAVERSAL", "ABSOLUTE_PATH", "WINDOWS_PATH",
                    "URL_DETECTED", "STORAGE_KEY_DETECTED", "BASE64_DETECTED",
                    "SECRET_DETECTED", "COMMAND_DETECTED", "EXTENSION_NOT_ALLOWED");
        }
    }
}
