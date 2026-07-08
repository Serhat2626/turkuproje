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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-037 — Scene Render Plan to Bundle Assembly.
 */
@DisplayName("FP-037 — Scene Render Plan to Bundle Assembly")
class TurkuSceneRenderPlanTest {

    private TurkuSceneRenderPlanService service;

    @BeforeEach
    void setUp() { service = new TurkuSceneRenderPlanService(); }

    private List<TurkuSceneRenderArtifactExpectation> primaryOnly() {
        return List.of(new TurkuSceneRenderArtifactExpectation(
                TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true));
    }

    private TurkuRenderBundleArtifactRef verifiedClip() {
        return new TurkuRenderBundleArtifactRef("clip-ref",
                TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, TurkuRenderArtifactFormat.MP4,
                TurkuRenderArtifactVerificationStatus.PASSED, 3, 24, 640, 360, 1024 * 1024);
    }

    // ── A. Valid Single Scene ──
    @Nested @DisplayName("A. Valid Single Scene Plan")
    class ValidSingle {
        @Test @DisplayName("valid plan → READY_FOR_BUNDLE_ASSEMBLY")
        void ready() {
            var plan = service.validate("scene-1", "plan-1", 1, 3, 24, 640, 360, primaryOnly());
            assertThat(plan.getStatus()).isEqualTo(TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY);
            assertThat(plan.getIssueCodes()).isEmpty();
            assertThat(plan.getSceneOrder()).isEqualTo(1);
        }
    }

    // ── B. Valid Plan → Bundle Assembly ──
    @Nested @DisplayName("B. Valid Plan + Clip → READY Bundle")
    class ValidAssembly {
        @Test @DisplayName("valid plan + verified clip → READY_FOR_INTERNAL_REVIEW_ONLY")
        void readyBundle() {
            var plan = service.validate("scene-1", "plan-1", 1, 3, 24, 640, 360, primaryOnly());
            var bundle = service.assembleBundle(plan, List.of(verifiedClip()));
            assertThat(bundle.getStatus()).isEqualTo(TurkuRenderBundleStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
            assertThat(bundle.isInternalOnly()).isTrue();
            assertThat(bundle.isPublicDeliveryAvailable()).isFalse();
        }
    }

    // ── C. Missing Primary Expectation ──
    @Nested @DisplayName("C. Missing PRIMARY_VIDEO_CLIP Expectation")
    class MissingPrimary {
        @Test @DisplayName("no primary expectation → REJECTED_UNSAFE")
        void noPrimary() {
            var plan = service.validate("s", "p", 1, 3, 24, 640, 360,
                    List.of(new TurkuSceneRenderArtifactExpectation(
                            TurkuRenderBundleArtifactRole.POSTER_FRAME, false)));
            assertThat(plan.getStatus()).isEqualTo(TurkuSceneRenderPlanStatus.REJECTED_UNSAFE);
            assertThat(plan.getIssueCodes()).contains(TurkuSceneRenderPlanIssueCode.MISSING_PRIMARY_VIDEO_EXPECTATION);
        }
    }

    // ── D. Duplicate Primary Expectation ──
    @Nested @DisplayName("D. Duplicate PRIMARY_VIDEO_CLIP Expectation")
    class DupPrimary {
        @Test @DisplayName("two primary expectations → REJECTED_UNSAFE")
        void dupPrimary() {
            var plan = service.validate("s", "p", 1, 3, 24, 640, 360, List.of(
                    new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true),
                    new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true)));
            assertThat(plan.getStatus()).isEqualTo(TurkuSceneRenderPlanStatus.REJECTED_UNSAFE);
            assertThat(plan.getIssueCodes()).contains(TurkuSceneRenderPlanIssueCode.DUPLICATE_PRIMARY_VIDEO_EXPECTATION);
        }
    }

    // ── E. Unknown Role ──
    @Nested @DisplayName("E. UNKNOWN Artifact Role")
    class UnknownRole {
        @Test @DisplayName("UNKNOWN role → REJECTED_UNSAFE")
        void unknownRole() {
            var plan = service.validate("s", "p", 1, 3, 24, 640, 360, List.of(
                    new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true),
                    new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.UNKNOWN, false)));
            assertThat(plan.getStatus()).isEqualTo(TurkuSceneRenderPlanStatus.REJECTED_UNSAFE);
            assertThat(plan.getIssueCodes()).contains(TurkuSceneRenderPlanIssueCode.UNSUPPORTED_ARTIFACT_ROLE);
        }
    }

    // ── F. Over-Duration ──
    @Nested @DisplayName("F. Over-Duration")
    class Duration {
        @Test @DisplayName("6s → REJECTED_UNSAFE")
        void overDuration() {
            var plan = service.validate("s", "p", 1, 6, 24, 640, 360, primaryOnly());
            assertThat(plan.getStatus()).isEqualTo(TurkuSceneRenderPlanStatus.REJECTED_UNSAFE);
            assertThat(plan.getIssueCodes()).contains(TurkuSceneRenderPlanIssueCode.DURATION_EXCEEDS_LIMIT);
        }
    }

    // ── G. Over-FPS ──
    @Nested @DisplayName("G. Over-FPS")
    class Fps {
        @Test @DisplayName("31fps → REJECTED_UNSAFE")
        void overFps() {
            var plan = service.validate("s", "p", 1, 3, 31, 640, 360, primaryOnly());
            assertThat(plan.getStatus()).isEqualTo(TurkuSceneRenderPlanStatus.REJECTED_UNSAFE);
            assertThat(plan.getIssueCodes()).contains(TurkuSceneRenderPlanIssueCode.FPS_EXCEEDS_LIMIT);
        }
    }

    // ── H. Over-Resolution ──
    @Nested @DisplayName("H. Over-Resolution")
    class Resolution {
        @Test @DisplayName("1281 width → REJECTED_UNSAFE")
        void overWidth() {
            var plan = service.validate("s", "p", 1, 3, 24, 1281, 360, primaryOnly());
            assertThat(plan.getIssueCodes()).contains(TurkuSceneRenderPlanIssueCode.WIDTH_EXCEEDS_LIMIT);
        }
        @Test @DisplayName("721 height → REJECTED_UNSAFE")
        void overHeight() {
            var plan = service.validate("s", "p", 1, 3, 24, 640, 721, primaryOnly());
            assertThat(plan.getIssueCodes()).contains(TurkuSceneRenderPlanIssueCode.HEIGHT_EXCEEDS_LIMIT);
        }
    }

    // ── I. Batch Validation ──
    @Nested @DisplayName("I. Batch Validation")
    class Batch {
        @Test @DisplayName("duplicate sceneOrder → SCENE_ORDER_DUPLICATE")
        void dupOrder() {
            var issues = service.validateBatch(List.of(1, 2, 2, 3));
            assertThat(issues).contains(TurkuSceneRenderPlanIssueCode.SCENE_ORDER_DUPLICATE);
        }
        @Test @DisplayName(">20 scenes → TOO_MANY_SCENES")
        void tooMany() {
            var orders = IntStream.rangeClosed(1, 21).boxed().toList();
            var issues = service.validateBatch(orders);
            assertThat(issues).contains(TurkuSceneRenderPlanIssueCode.TOO_MANY_SCENES);
        }
        @Test @DisplayName("valid batch → no issues")
        void validBatch() {
            assertThat(service.validateBatch(List.of(1, 2, 3))).isEmpty();
        }
    }

    // ── J. Missing Required Artifact in Assembly ──
    @Nested @DisplayName("J. Missing Required Artifact")
    class MissingArtifact {
        @Test @DisplayName("no clip provided → REJECTED bundle")
        void missingClip() {
            var plan = service.validate("s", "p", 1, 3, 24, 640, 360, primaryOnly());
            var bundle = service.assembleBundle(plan, List.of());
            assertThat(bundle.getStatus()).isEqualTo(TurkuRenderBundleStatus.REJECTED_UNSAFE);
            assertThat(bundle.getIssueCodes()).contains(TurkuRenderBundleIssueCode.MISSING_PRIMARY_VIDEO);
        }
    }

    // ── K. Rejected Lifecycle Artifact ──
    @Nested @DisplayName("K. Rejected Lifecycle Artifact → Rejected Bundle")
    class RejectedArtifact {
        @Test @DisplayName("rejected artifact → REJECTED_UNSAFE bundle")
        void rejectedBundle() {
            var plan = service.validate("s", "p", 1, 3, 24, 640, 360, primaryOnly());
            var rejected = new TurkuRenderBundleArtifactRef("bad-ref",
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, TurkuRenderArtifactFormat.MP4,
                    TurkuRenderArtifactVerificationStatus.REJECTED_FORMAT, 3, 24, 640, 360, 1024);
            var bundle = service.assembleBundle(plan, List.of(rejected));
            assertThat(bundle.getStatus()).isEqualTo(TurkuRenderBundleStatus.REJECTED_UNSAFE);
        }
    }

    // ── L. Failed Lifecycle Artifact ──
    @Nested @DisplayName("L. Failed Lifecycle Artifact → Failed Bundle")
    class FailedArtifact {
        @Test @DisplayName("failed artifact → FAILED_SAFE bundle")
        void failedBundle() {
            var plan = service.validate("s", "p", 1, 3, 24, 640, 360, primaryOnly());
            var failed = new TurkuRenderBundleArtifactRef("fail-ref",
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, TurkuRenderArtifactFormat.MP4,
                    TurkuRenderArtifactVerificationStatus.FAILED_SAFE, 3, 24, 640, 360, 1024);
            var bundle = service.assembleBundle(plan, List.of(failed));
            assertThat(bundle.getStatus()).isEqualTo(TurkuRenderBundleStatus.FAILED_SAFE);
        }
    }

    // ── M. toString Safety ──
    @Nested @DisplayName("M. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("plan toString safe")
        void safe() {
            var plan = service.validate("s", "p", 1, 3, 24, 640, 360, primaryOnly());
            var str = plan.toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal");
        }
    }

    // ── N. Enum Inventory ──
    @Nested @DisplayName("N. Enum Inventory")
    class Enums {
        @Test @DisplayName("5 plan statuses")
        void statuses() { assertThat(TurkuSceneRenderPlanStatus.values()).hasSize(5); }
        @Test @DisplayName("17 plan issue codes")
        void issues() { assertThat(TurkuSceneRenderPlanIssueCode.values()).hasSize(17); }
    }

    // ── O. ProcessBuilder Inventory ──
    @Nested @DisplayName("O. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("plan service no ProcessBuilder")
        void serviceClean() {
            Set<String> types = Arrays.stream(TurkuSceneRenderPlanService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("plan no ProcessBuilder")
        void planClean() {
            Set<String> types = Arrays.stream(TurkuSceneRenderPlan.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── P. Forbidden Exposure ──
    @Nested @DisplayName("P. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "lyrics", "scriptBody", "promptText", "copyrightedContent");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuSceneRenderPlanStatus.class, TurkuSceneRenderPlanIssueCode.class,
                TurkuSceneRenderArtifactExpectation.class, TurkuSceneRenderPlan.class,
                TurkuSceneRenderPlanService.class);

        @Test @DisplayName("5 classes x 28 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── Q. Endpoint Inventory ──
    @Nested @DisplayName("Q. Endpoint Inventory (44 = 25 POST + 19 GET)")
    class Endpoints {
        @Test @DisplayName("44 = 25 POST + 19 GET")
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
}
