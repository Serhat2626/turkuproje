package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LIVE-001 — Real Provider Credential Enablement + Manual Kill Switch")
class TurkuLiveProviderReadinessTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<?>> LIVE001_CLASSES = List.of(
            TurkuKillSwitch.class, TurkuLiveProviderReadinessReport.class,
            TurkuLiveProviderReadinessService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED",
            "PROVIDER_ENABLED", "IMAGE_GENERATION_READY");

    private TurkuLiveProviderReadinessService service;

    @BeforeEach void setUp() { service = new TurkuLiveProviderReadinessService(); }

    // A. Kill Switch Blocks
    @Nested @DisplayName("A. Kill Switch Blocks")
    class KillSwitch {
        @Test @DisplayName("default kill switch -> BLOCKED_BY_KILL_SWITCH")
        void defaultKill() {
            var r = service.evaluate(true, true, TurkuKillSwitch.defaultActive());
            assertThat(r.getStatus()).isEqualTo(TurkuLiveProviderStatus.BLOCKED_BY_KILL_SWITCH);
            assertThat(r.getKillSwitch().isActive()).isTrue();
            assertThat(r.getIssues()).contains(TurkuLiveProviderIssueCode.KILL_SWITCH_ACTIVE);
        }
        @Test @DisplayName("null kill switch -> default active")
        void nullKill() {
            var r = service.evaluate(true, true, null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveProviderStatus.BLOCKED_BY_KILL_SWITCH);
        }
    }

    // B. Disabled by Default
    @Nested @DisplayName("B. Disabled by Default")
    class DisabledDefault {
        @Test @DisplayName("no credential -> DISABLED_BY_DEFAULT")
        void noCred() {
            var r = service.evaluate(false, false, new TurkuKillSwitch(false, "test", "OPERATOR"));
            assertThat(r.getStatus()).isEqualTo(TurkuLiveProviderStatus.DISABLED_BY_DEFAULT);
            assertThat(r.isCredentialPresent()).isFalse();
        }
    }

    // C. Credential Present Only
    @Nested @DisplayName("C. Credential Present — No Secret")
    class CredentialPresence {
        @Test @DisplayName("presence label, no value")
        void presence() {
            var r = service.evaluate(true, true, new TurkuKillSwitch(false, "test", "OPERATOR"));
            assertThat(r.isCredentialPresent()).isTrue();
            assertThat(r.getCredentialSourceLabel()).contains("mevcut");
            assertThat(r.getCredentialSourceLabel()).contains("gizli");
        }
        @Test @DisplayName("CREDENTIAL_PRESENT when all flags true and kill switch off")
        void ready() {
            var r = service.evaluate(true, true, new TurkuKillSwitch(false, "test", "OPERATOR"));
            assertThat(r.getStatus()).isEqualTo(TurkuLiveProviderStatus.CREDENTIAL_PRESENT);
        }
    }

    // D. Operator Enablement
    @Nested @DisplayName("D. Operator Enablement")
    class OpEnable {
        @Test @DisplayName("not operator enabled -> WAITING_FOR_OPERATOR_ENABLEMENT")
        void waiting() {
            var r = service.evaluate(true, false, new TurkuKillSwitch(false, "test", "OPERATOR"));
            assertThat(r.getStatus()).isEqualTo(TurkuLiveProviderStatus.WAITING_FOR_OPERATOR_ENABLEMENT);
            assertThat(r.getIssues()).contains(TurkuLiveProviderIssueCode.OPERATOR_ENABLEMENT_REQUIRED);
        }
    }

    // E. Forbidden Sweep
    @Nested @DisplayName("E. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : LIVE001_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // F. No Forbidden Statuses
    @Nested @DisplayName("F. No Forbidden Statuses")
    class NoForbidden {
        @Test @DisplayName("status safe")
        void safe() {
            for (var v : TurkuLiveProviderStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("exactly 5 statuses")
        void count() { assertThat(TurkuLiveProviderStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 5 issue codes")
        void issues() { assertThat(TurkuLiveProviderIssueCode.values()).hasSize(5); }
    }

    // G. Endpoint Unchanged
    @Nested @DisplayName("G. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // H. ProcessBuilder / Runtime
    @Nested @DisplayName("H. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : LIVE001_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
