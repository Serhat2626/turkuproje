package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-050 — Admin Internal Dry Run Endpoint & UI Safety Baseline Closure.
 * No new capability. Regression hardening only.
 */
@DisplayName("FP-050 — Dry Run Safety Baseline")
class TurkuDryRunSafetyBaselineTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "storageKey", "publicUrl", "signedUrl", "cdnUrl", "storageProvider",
            "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia");

    private static final List<Class<?>> FP049_CLASSES = List.of(
            TurkuInternalDryRunTriggerRequest.class,
            TurkuInternalDryRunTriggerResponse.class);

    // ── A. Forbidden Field Sweep ──
    @Nested @DisplayName("A. Forbidden Field Sweep (FP-049 DTOs)")
    class ForbiddenSweep {
        @Test @DisplayName("TurkuInternalDryRunTriggerRequest — no forbidden fields")
        void request() { checkFields(TurkuInternalDryRunTriggerRequest.class); }

        @Test @DisplayName("TurkuInternalDryRunTriggerResponse — no forbidden fields")
        void response() { checkFields(TurkuInternalDryRunTriggerResponse.class); }

        private void checkFields(Class<?> c) {
            Set<String> fields = Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN)
                assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
        }
    }

    // ── B. No READY_FOR_PUBLISH / COMPLETED / AUTO_APPROVED ──
    @Nested @DisplayName("B. No READY_FOR_PUBLISH / COMPLETED / AUTO_APPROVED")
    class NoPublishStatus {
        @Test @DisplayName("chain status enum — no forbidden values")
        void chainStatus() {
            for (var v : TurkuStoryboardInternalRenderChainStatus.values()) {
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH");
                assertThat(v.name()).doesNotContain("COMPLETED");
                assertThat(v.name()).doesNotContain("AUTO_APPROVED");
            }
        }
        @Test @DisplayName("chain issue enum — no forbidden values")
        void chainIssue() {
            for (var v : TurkuStoryboardInternalRenderChainIssueCode.values()) {
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH");
                assertThat(v.name()).doesNotContain("AUTO_APPROVED");
            }
        }
    }

    // ── C. ProcessBuilder Boundary ──
    @Nested @DisplayName("C. ProcessBuilder Boundary")
    class ProcessBuilderBoundary {
        @Test @DisplayName("FP-049 DTOs have no ProcessBuilder/Runtime fields")
        void clean() {
            for (var c : FP049_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName())
                        .doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // ── D. Response Always internalOnly ──
    @Nested @DisplayName("D. Response Safety")
    class ResponseSafety {
        @Test @DisplayName("valid response has internalOnly=true")
        void internalOnly() {
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(fixture));
            assertThat(response.isInternalOnly()).isTrue();
        }
        @Test @DisplayName("valid response reaches ADMIN_METADATA_VISIBLE")
        void reachesAdmin() {
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(2);
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(fixture));
            assertThat(response.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.ADMIN_METADATA_VISIBLE);
            assertThat(response.isAdminMetadataVisible()).isTrue();
        }
        @Test @DisplayName("null fixture → REJECTED_UNSAFE + internalOnly")
        void rejectedSafe() {
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(null));
            assertThat(response.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE);
            assertThat(response.isInternalOnly()).isTrue();
        }
    }

    // ── E. Endpoint Inventory Unchanged ──
    @Nested @DisplayName("E. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class EndpointInventory {
        @Test @DisplayName("controller: 17 POST + 8 GET")
        void controller() {
            long post = count(TurkuVideoProjectController.class, PostMapping.class);
            long get = count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).isEqualTo(17);
            assertThat(get).isEqualTo(8);
        }
        @Test @DisplayName("total: 46 = 26 POST + 20 GET")
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

    // ── F. Controller is Admin-only ──
    @Nested @DisplayName("F. Controller Admin-only")
    class AdminOnly {
        @Test @DisplayName("controller path is /api/admin/turku-video-projects")
        void path() {
            var mapping = TurkuVideoProjectController.class.getAnnotation(RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()[0]).isEqualTo("/api/admin/turku-video-projects");
        }
        @Test @DisplayName("no public endpoint path")
        void noPublic() {
            var mapping = TurkuVideoProjectController.class.getAnnotation(RequestMapping.class);
            assertThat(mapping.value()[0]).doesNotContain("/public");
            assertThat(mapping.value()[0]).doesNotContain("/api/v1");
        }
    }

    // ── G. Request DTO Safe Metadata Only ──
    @Nested @DisplayName("G. Request DTO Safe Metadata Only")
    class RequestSafe {
        @Test @DisplayName("request fields are opaque refs + scene metadata")
        void fieldNames() {
            Set<String> fields = Arrays.stream(TurkuInternalDryRunTriggerRequest.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).containsExactlyInAnyOrder("opaqueStoryboardRef", "opaqueStoryRef", "sceneInputs");
        }
    }

    // ── H. Response DTO Safe Metadata Only ──
    @Nested @DisplayName("H. Response DTO Safe Metadata Only")
    class ResponseFields {
        @Test @DisplayName("response has expected safe fields")
        void fieldNames() {
            Set<String> fields = Arrays.stream(TurkuInternalDryRunTriggerResponse.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).contains("opaqueChainRef", "opaqueStoryboardRef", "status",
                    "issueCodes", "sceneCount", "generatedClipCount", "stitchedArtifactPresent",
                    "reviewPackageStatus", "adminMetadataVisible", "internalOnly");
        }
    }
}
