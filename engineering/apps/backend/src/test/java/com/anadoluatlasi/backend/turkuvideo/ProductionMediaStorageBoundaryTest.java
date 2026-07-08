package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * PD-001 — Production Media Storage Boundary + Provider Abstraction.
 */
@DisplayName("PD-001 — Production Media Storage Boundary")
class ProductionMediaStorageBoundaryTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl",
            "bucketName", "objectKey",
            "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html");

    private static final List<Class<?>> PD001_CLASSES = List.of(
            ProductionMediaStorageObjectRef.class, ProductionMediaStorageResult.class,
            ProductionMediaStoragePolicy.class, FakeProductionMediaStorageGateway.class);

    private FakeProductionMediaStorageGateway gateway;

    @BeforeEach void setUp() { gateway = new FakeProductionMediaStorageGateway(); }

    // ── A. Fake Provider — Valid Storage ──
    @Nested @DisplayName("A. Fake Provider — Valid Storage")
    class ValidStorage {
        @Test @DisplayName("valid MP4 creates PRIVATE_OBJECT_REF_CREATED")
        void validMp4() {
            var result = gateway.storePrivate("art:123", "MP4", 10, 24, 640, 360, 1024, true, "chk:abc");
            assertThat(result.getStatus()).isEqualTo(ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED);
            assertThat(result.isPrivateOnly()).isTrue();
            assertThat(result.getObjectRef()).isNotNull();
            assertThat(result.getObjectRef().getOpaqueStorageObjectRef()).startsWith("fake-obj:");
            assertThat(result.getObjectRef().getProviderType()).isEqualTo(ProductionMediaStorageProviderType.FAKE_TEST_DOUBLE);
            assertThat(result.getObjectRef().getStorageMode()).isEqualTo(ProductionMediaStorageMode.PRIVATE_ONLY);
        }
        @Test @DisplayName("valid WEBM accepted")
        void validWebm() {
            var result = gateway.storePrivate("art:456", "WEBM", 5, 30, 1920, 1080, 2048, true, "chk:xyz");
            assertThat(result.getStatus()).isEqualTo(ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED);
        }
        @Test @DisplayName("store count increments")
        void storeCount() {
            gateway.storePrivate("a", "MP4", 1, 24, 640, 360, 100, true, "c");
            gateway.storePrivate("b", "WEBM", 1, 24, 640, 360, 100, true, "c");
            assertThat(gateway.getStoreCount()).isEqualTo(2);
        }
    }

    // ── B. Rejected/Failed ──
    @Nested @DisplayName("B. Rejected/Failed")
    class Rejected {
        @Test @DisplayName("unsupported format rejected")
        void unsupported() {
            var result = gateway.storePrivate("art:x", "AVI", 1, 24, 640, 360, 100, true, "c");
            assertThat(result.getStatus()).isEqualTo(ProductionMediaStorageObjectStatus.REJECTED_UNSAFE);
            assertThat(result.getIssueCodes()).contains(ProductionMediaStorageIssueCode.UNSUPPORTED_FORMAT);
        }
        @Test @DisplayName("over-size rejected")
        void overSize() {
            var result = gateway.storePrivate("art:x", "MP4", 1, 24, 640, 360, 600L * 1024 * 1024, true, "c");
            assertThat(result.getStatus()).isEqualTo(ProductionMediaStorageObjectStatus.REJECTED_UNSAFE);
            assertThat(result.getIssueCodes()).contains(ProductionMediaStorageIssueCode.OVER_SIZE_LIMIT);
        }
        @Test @DisplayName("missing checksum rejected")
        void noChecksum() {
            var result = gateway.storePrivate("art:x", "MP4", 1, 24, 640, 360, 100, false, null);
            assertThat(result.getStatus()).isEqualTo(ProductionMediaStorageObjectStatus.REJECTED_UNSAFE);
            assertThat(result.getIssueCodes()).contains(ProductionMediaStorageIssueCode.MISSING_CHECKSUM);
        }
        @Test @DisplayName("rejected result has no objectRef")
        void noRef() {
            var result = gateway.storePrivate("art:x", "AVI", 1, 24, 640, 360, 100, true, "c");
            assertThat(result.getObjectRef()).isNull();
        }
        @Test @DisplayName("static factory rejected/failed")
        void factory() {
            var r = ProductionMediaStorageResult.rejected(ProductionMediaStorageIssueCode.ARTIFACT_REJECTED);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageObjectStatus.REJECTED_UNSAFE);
            var f = ProductionMediaStorageResult.failed(ProductionMediaStorageIssueCode.PROVIDER_UNAVAILABLE);
            assertThat(f.getStatus()).isEqualTo(ProductionMediaStorageObjectStatus.FAILED_SAFE);
        }
    }

    // ── C. Policy ──
    @Nested @DisplayName("C. Policy Validation")
    class PolicyTests {
        private final ProductionMediaStoragePolicy policy = new ProductionMediaStoragePolicy();
        @Test @DisplayName("MP4 allowed") void mp4() { assertThat(policy.isFormatAllowed("MP4")).isTrue(); }
        @Test @DisplayName("WEBM allowed") void webm() { assertThat(policy.isFormatAllowed("webm")).isTrue(); }
        @Test @DisplayName("AVI not allowed") void avi() { assertThat(policy.isFormatAllowed("AVI")).isFalse(); }
        @Test @DisplayName("null not allowed") void nul() { assertThat(policy.isFormatAllowed(null)).isFalse(); }
        @Test @DisplayName("500MB allowed") void maxSize() { assertThat(policy.isSizeAllowed(500L * 1024 * 1024)).isTrue(); }
        @Test @DisplayName("501MB not allowed") void overSize() { assertThat(policy.isSizeAllowed(501L * 1024 * 1024)).isFalse(); }
        @Test @DisplayName("0 bytes not allowed") void zero() { assertThat(policy.isSizeAllowed(0)).isFalse(); }
    }

    // ── D. Forbidden Field Sweep ──
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("all PD-001 classes — 0 forbidden fields")
        void sweep() {
            for (var c : PD001_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── E. No Publish/Export Statuses ──
    @Nested @DisplayName("E. No READY_FOR_PUBLISH / COMPLETED / PUBLIC / CDN_READY")
    class NoPublish {
        @Test @DisplayName("object status enum safe")
        void status() {
            for (var v : ProductionMediaStorageObjectStatus.values()) {
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH");
                assertThat(v.name()).doesNotContain("COMPLETED");
                assertThat(v.name()).doesNotContain("PUBLIC");
                assertThat(v.name()).doesNotContain("CDN_READY");
                assertThat(v.name()).doesNotContain("AUTO_APPROVED");
                assertThat(v.name()).doesNotContain("DOWNLOAD_READY");
                assertThat(v.name()).doesNotContain("EXPORT_READY");
                assertThat(v.name()).doesNotContain("PUBLISHED");
            }
        }
        @Test @DisplayName("exactly 6 object statuses")
        void count() { assertThat(ProductionMediaStorageObjectStatus.values()).hasSize(6); }
        @Test @DisplayName("exactly 10 issue codes")
        void issueCount() { assertThat(ProductionMediaStorageIssueCode.values()).hasSize(10); }
        @Test @DisplayName("exactly 4 provider types")
        void providerCount() { assertThat(ProductionMediaStorageProviderType.values()).hasSize(4); }
        @Test @DisplayName("exactly 2 storage modes")
        void modeCount() { assertThat(ProductionMediaStorageMode.values()).hasSize(2); }
    }

    // ── F. Endpoint Inventory Unchanged ──
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
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

    // ── G. ProcessBuilder Boundary ──
    @Nested @DisplayName("G. ProcessBuilder Boundary")
    class ProcessBoundary {
        @Test @DisplayName("PD-001 classes no ProcessBuilder/Runtime")
        void clean() {
            for (var c : PD001_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // ── H. Gateway Interface Contract ──
    @Nested @DisplayName("H. Gateway Interface Contract")
    class GatewayContract {
        @Test @DisplayName("gateway returns FAKE_TEST_DOUBLE provider type")
        void providerType() {
            assertThat(gateway.getProviderType()).isEqualTo(ProductionMediaStorageProviderType.FAKE_TEST_DOUBLE);
        }
        @Test @DisplayName("gateway result always privateOnly")
        void privateOnly() {
            var r = gateway.storePrivate("a", "MP4", 1, 24, 640, 360, 100, true, "c");
            assertThat(r.isPrivateOnly()).isTrue();
        }
    }
}
