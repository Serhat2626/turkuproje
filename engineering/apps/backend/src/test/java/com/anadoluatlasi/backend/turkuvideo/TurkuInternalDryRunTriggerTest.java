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
 * FP-049 — Admin Internal Story-to-Video Dry Run Trigger + Visibility.
 */
@DisplayName("FP-049 — Admin Dry Run Trigger")
class TurkuInternalDryRunTriggerTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "storageKey", "publicUrl", "signedUrl", "cdnUrl",
            "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "lyrics", "scriptBody", "promptText", "copyrightedContent");

    // ── A. Valid Trigger ──
    @Nested @DisplayName("A. Valid Trigger")
    class ValidTrigger {
        @Test @DisplayName("valid request → ADMIN_METADATA_VISIBLE response")
        void valid() {
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
            var summary = harness.execute(fixture);
            var response = new TurkuInternalDryRunTriggerResponse(summary);
            assertThat(response.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.ADMIN_METADATA_VISIBLE);
            assertThat(response.isInternalOnly()).isTrue();
            assertThat(response.isAdminMetadataVisible()).isTrue();
            assertThat(response.getSceneCount()).isEqualTo(3);
        }
    }

    // ── B. Unsafe Request Rejects ──
    @Nested @DisplayName("B. Unsafe Request Rejects")
    class Unsafe {
        @Test @DisplayName("null fixture → REJECTED_UNSAFE")
        void nullFix() {
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var summary = harness.execute(null);
            var response = new TurkuInternalDryRunTriggerResponse(summary);
            assertThat(response.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE);
        }
    }

    // ── C. Request DTO Safe ──
    @Nested @DisplayName("C. Request DTO Safe")
    class RequestSafe {
        @Test @DisplayName("request has no forbidden fields")
        void fields() {
            Set<String> fields = Arrays.stream(TurkuInternalDryRunTriggerRequest.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN)
                assertThat(fields).as(f).doesNotContain(f);
        }
    }

    // ── D. Response DTO Safe ──
    @Nested @DisplayName("D. Response DTO Safe")
    class ResponseSafe {
        @Test @DisplayName("response has no forbidden fields")
        void fields() {
            Set<String> fields = Arrays.stream(TurkuInternalDryRunTriggerResponse.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN)
                assertThat(fields).as(f).doesNotContain(f);
        }
        @Test @DisplayName("response internalOnly=true")
        void internalOnly() {
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(2);
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(fixture));
            assertThat(response.isInternalOnly()).isTrue();
        }
    }

    // ── E. Endpoint Inventory ──
    @Nested @DisplayName("E. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
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

    // ── F. ProcessBuilder Inventory ──
    @Nested @DisplayName("F. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("DTOs no ProcessBuilder")
        void clean() {
            for (var c : List.of(TurkuInternalDryRunTriggerRequest.class,
                    TurkuInternalDryRunTriggerResponse.class)) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName()).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // ── G. Endpoint is Internal/Admin-only ──
    @Nested @DisplayName("G. Endpoint Admin-only")
    class AdminOnly {
        @Test @DisplayName("endpoint path contains admin")
        void path() {
            var mapping = TurkuVideoProjectController.class.getAnnotation(RequestMapping.class);
            assertThat(mapping.value()[0]).contains("admin");
        }
    }
}
