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
 * FP-023 Task 04: Internal Artifact Metadata + Cleanup Policy tests.
 */
@DisplayName("FP-023 Task 04 — Internal Artifact Metadata + Cleanup Policy")
class TurkuRenderArtifactMetadataTest {

    private TurkuRenderArtifactService artifactService;
    private StubTurkuRenderRunnerGateway stubRunner;
    private FfmpegTurkuRenderRunnerGateway ffmpegSandbox;

    @BeforeEach
    void setUp() {
        artifactService = new TurkuRenderArtifactService();
        stubRunner = new StubTurkuRenderRunnerGateway();
        ffmpegSandbox = new FfmpegTurkuRenderRunnerGateway(true);
    }

    private TurkuRenderExecutionPlan validPlan() {
        return new TurkuRenderExecutionPlan(
                UUID.randomUUID(), 5, 120.0, "1920x1080", 30, "mp4",
                60.0, "deterministic-stub", "DETERMINISTIC_STUB");
    }

    // ── A. Endpoint Inventory ──

    @Nested
    @DisplayName("A. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("total 46 = 26 POST + 20 GET")
        void totalEndpoints() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("total POST").isEqualTo(26);
            assertThat(get).as("total GET").isEqualTo(20);
            assertThat(post + get).as("total endpoints").isEqualTo(46);
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

    // ── B. Artifact from Stub Runner ──

    @Nested
    @DisplayName("B. Artifact from Stub Runner")
    class StubArtifact {

        @Test
        @DisplayName("creates artifact from stub result")
        void fromStub() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            assertThat(artifact).isNotNull();
            assertThat(artifact.getStatus()).isEqualTo(TurkuRenderArtifactStatus.VALIDATED);
        }

        @Test
        @DisplayName("artifact has opaque ref")
        void opaqueRef() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            assertThat(artifact.getReference().getOpaqueRef()).startsWith("internal-stub-artifact:");
        }

        @Test
        @DisplayName("artifact ref has no forbidden content")
        void refSafe() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            assertThat(artifact.getReference().getOpaqueRef()).doesNotContain(
                    "http", "cdn", "public", "signed", "storage",
                    "file://", ".mp4", "C:\\", "/usr", "base64");
        }
    }

    // ── C. Artifact from FFmpeg Sandbox ──

    @Nested
    @DisplayName("C. Artifact from FFmpeg Sandbox")
    class FfmpegArtifact {

        @Test
        @DisplayName("creates artifact from sandbox result")
        void fromSandbox() {
            var result = ffmpegSandbox.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.FFMPEG_READY);
            assertThat(artifact).isNotNull();
            assertThat(artifact.getStatus()).isEqualTo(TurkuRenderArtifactStatus.VALIDATED);
        }

        @Test
        @DisplayName("sandbox artifact has opaque ref")
        void sandboxRef() {
            var result = ffmpegSandbox.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.FFMPEG_READY);
            assertThat(artifact.getReference().getOpaqueRef()).startsWith("internal-ffmpeg-sandbox:");
        }

        @Test
        @DisplayName("renderer mode preserved")
        void rendererMode() {
            var result = ffmpegSandbox.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.FFMPEG_READY);
            assertThat(artifact.getReference().getRendererMode())
                    .isEqualTo(TurkuRenderAdapterMode.FFMPEG_READY);
        }
    }

    // ── D. Unsafe Ref Rejection ──

    @Nested
    @DisplayName("D. Unsafe Ref Rejection")
    class UnsafeRefRejection {

        @Test
        @DisplayName("rejects file path")
        void filePath() {
            assertThat(artifactService.isValidOpaqueRef("/usr/local/render/video.mp4")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("C:\\Users\\render\\video.mp4")).isFalse();
        }

        @Test
        @DisplayName("rejects public URL")
        void publicUrl() {
            assertThat(artifactService.isValidOpaqueRef("https://cdn.example.com/video.mp4")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("http://storage.example.com/key")).isFalse();
        }

        @Test
        @DisplayName("rejects signed URL")
        void signedUrl() {
            assertThat(artifactService.isValidOpaqueRef("signedUrl=abc123")).isFalse();
        }

        @Test
        @DisplayName("rejects CDN URL")
        void cdnUrl() {
            assertThat(artifactService.isValidOpaqueRef("cdnUrl=example.com")).isFalse();
        }

        @Test
        @DisplayName("rejects storage key")
        void storageKey() {
            assertThat(artifactService.isValidOpaqueRef("storageKey=bucket/key")).isFalse();
        }

        @Test
        @DisplayName("rejects base64")
        void base64() {
            assertThat(artifactService.isValidOpaqueRef("base64:aGVsbG8=")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("data:video/mp4;base64,abc")).isFalse();
        }

        @Test
        @DisplayName("rejects S3/GCS/Azure URLs")
        void cloudUrls() {
            assertThat(artifactService.isValidOpaqueRef("s3://bucket/key")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("gs://bucket/key")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("az://container/blob")).isFalse();
        }

        @Test
        @DisplayName("rejects secrets/tokens")
        void secrets() {
            assertThat(artifactService.isValidOpaqueRef("Bearer token123")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("Authorization: basic abc")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("password=abc123")).isFalse();
        }

        @Test
        @DisplayName("rejects shell commands")
        void shellCommands() {
            assertThat(artifactService.isValidOpaqueRef("$(rm -rf /)")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("ref && cat /etc/passwd")).isFalse();
        }

        @Test
        @DisplayName("accepts safe opaque refs")
        void acceptsSafe() {
            assertThat(artifactService.isValidOpaqueRef("internal-stub-artifact:" + UUID.randomUUID())).isTrue();
            assertThat(artifactService.isValidOpaqueRef("internal-ffmpeg-sandbox:" + UUID.randomUUID())).isTrue();
            assertThat(artifactService.isValidOpaqueRef("internal-render:" + UUID.randomUUID())).isTrue();
        }

        @Test
        @DisplayName("rejects null/blank")
        void nullBlank() {
            assertThat(artifactService.isValidOpaqueRef(null)).isFalse();
            assertThat(artifactService.isValidOpaqueRef("")).isFalse();
            assertThat(artifactService.isValidOpaqueRef("   ")).isFalse();
        }
    }

    // ── E. Cleanup Plan ──

    @Nested
    @DisplayName("E. Cleanup Plan")
    class CleanupPlanTests {

        @Test
        @DisplayName("creates cleanup plan for validated artifact")
        void validatedCleanup() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            var plan = artifactService.createCleanupPlan(artifact);
            assertThat(plan).isNotNull();
            assertThat(plan.isEligible()).isTrue();
        }

        @Test
        @DisplayName("cleanup plan has opaque ref")
        void cleanupRef() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            var plan = artifactService.createCleanupPlan(artifact);
            assertThat(plan.getOpaqueRef()).startsWith("internal-stub-artifact:");
        }

        @Test
        @DisplayName("cleanup plan has retention policy")
        void retentionPolicy() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            var plan = artifactService.createCleanupPlan(artifact);
            assertThat(plan.getRetentionPolicy().getRetentionHours()).isEqualTo(24);
        }

        @Test
        @DisplayName("null artifact returns null plan")
        void nullArtifact() {
            assertThat(artifactService.createCleanupPlan(null)).isNull();
        }
    }

    // ── F. Retention Policy ──

    @Nested
    @DisplayName("F. Retention Policy")
    class RetentionPolicyTests {

        @Test
        @DisplayName("default retention 24h")
        void defaults() {
            var policy = new TurkuRenderArtifactRetentionPolicy();
            assertThat(policy.getRetentionHours()).isEqualTo(24);
            assertThat(policy.isAutoCleanupEnabled()).isFalse();
            assertThat(policy.getPolicyLabel()).isEqualTo("default-24h");
        }

        @Test
        @DisplayName("clamps to max 168h")
        void clampsMax() {
            var policy = new TurkuRenderArtifactRetentionPolicy(9999, true, "test");
            assertThat(policy.getRetentionHours()).isEqualTo(168);
        }

        @Test
        @DisplayName("clamps to min 1h")
        void clampsMin() {
            var policy = new TurkuRenderArtifactRetentionPolicy(-5, false, "test");
            assertThat(policy.getRetentionHours()).isEqualTo(1);
        }
    }

    // ── G. Safety Report ──

    @Nested
    @DisplayName("G. Safety Report")
    class SafetyReportTests {

        @Test
        @DisplayName("immutable safety flags")
        void safetyFlags() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            var report = artifactService.getSafetyReport(artifact);
            assertThat(report.isHumanReviewRequired()).isTrue();
            assertThat(report.isCopyrightReviewRequired()).isTrue();
            assertThat(report.isAutoApproved()).isFalse();
            assertThat(report.isNotPublishReady()).isTrue();
            assertThat(report.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("safety flags list")
        void flagsList() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            var report = artifactService.getSafetyReport(artifact);
            assertThat(report.getSafetyFlags()).contains(
                    "NO_DOWNLOAD", "NO_EXPORT", "NO_PUBLISH",
                    "NO_PUBLIC_DELIVERY", "OPAQUE_REF_ONLY");
            assertThat(report.getReviewFlags()).contains(
                    "HUMAN_REVIEW_REQUIRED", "NOT_PUBLISH_READY");
        }
    }

    // ── H. Artifact Status ──

    @Nested
    @DisplayName("H. Artifact Status")
    class ArtifactStatusTests {

        @Test
        @DisplayName("enum has 7 values")
        void statusCount() {
            assertThat(TurkuRenderArtifactStatus.values()).hasSize(7);
        }

        @Test
        @DisplayName("no forbidden statuses")
        void noForbidden() {
            var statuses = Arrays.stream(TurkuRenderArtifactStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain(
                    "PUBLISHED", "EXPORTED", "DOWNLOADED",
                    "PUBLIC_READY", "AUTO_APPROVED");
        }

        @Test
        @DisplayName("status transitions")
        void transitions() {
            var result = stubRunner.execute(validPlan());
            var artifact = artifactService.createFromExecutionResult(result, TurkuRenderAdapterMode.STUB);
            assertThat(artifact.getStatus()).isEqualTo(TurkuRenderArtifactStatus.VALIDATED);
            artifact.markCleanupEligible();
            assertThat(artifact.getStatus()).isEqualTo(TurkuRenderArtifactStatus.CLEANUP_ELIGIBLE);
        }
    }

    // ── I. DTO Safety ──

    @Nested
    @DisplayName("I. DTO Safety — Forbidden Fields")
    class DtoSafety {

        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "rawRendererPayload", "token", "jwt", "session",
                "credential", "password", "secret", "apiKey", "authorizationHeader",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "binaryData",
                "renderedVideo", "videoFile", "downloadFile",
                "filePath", "renderedVideoUrl", "commandLine", "ffmpegCommand",
                "shellCommand", "absolutePath", "processBuilder");

        @Test
        @DisplayName("TurkuInternalRenderArtifact safe")
        void internalArtifact() { assertNoForbidden(TurkuInternalRenderArtifact.class); }

        @Test
        @DisplayName("TurkuRenderArtifactReference safe")
        void reference() { assertNoForbidden(TurkuRenderArtifactReference.class); }

        @Test
        @DisplayName("TurkuRenderArtifactSafetyReport safe")
        void safetyReport() { assertNoForbidden(TurkuRenderArtifactSafetyReport.class); }

        @Test
        @DisplayName("TurkuRenderArtifactRetentionPolicy safe")
        void retentionPolicy() { assertNoForbidden(TurkuRenderArtifactRetentionPolicy.class); }

        @Test
        @DisplayName("TurkuRenderArtifactCleanupPlan safe")
        void cleanupPlan() { assertNoForbidden(TurkuRenderArtifactCleanupPlan.class); }

        @Test
        @DisplayName("TurkuRenderArtifactService safe")
        void service() { assertNoForbidden(TurkuRenderArtifactService.class); }

        private void assertNoForbidden(Class<?> clazz) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("field '%s' in %s", f, clazz.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }
}
