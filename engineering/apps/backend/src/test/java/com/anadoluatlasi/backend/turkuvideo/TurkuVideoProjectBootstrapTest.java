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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FP-019 Task 02: Türkü Video Project Bootstrap Backend Tests.
 */
@DisplayName("FP-019 Task 02 — Türkü Video Project Bootstrap")
class TurkuVideoProjectBootstrapTest {

    private TurkuVideoProjectRepository repository;
    private TurkuVideoProjectBootstrapService service;
    private AdminActorResolver actorResolver;

    @BeforeEach
    void setUp() {
        repository = new TurkuVideoProjectRepository();
        service = new TurkuVideoProjectBootstrapService(repository);
        actorResolver = new AdminActorResolver();
    }

    // ── A. Bootstrap Happy Path ─────────────────────────────

    @Nested
    @DisplayName("A. Bootstrap Happy Path")
    class BootstrapHappyPath {

        @Test
        @DisplayName("bootstrap with minimal türkü name succeeds")
        void minimalBootstrap() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Mihriban");
            var project = service.bootstrap(req, "admin-test");

            assertThat(project.getId()).isNotNull();
            assertThat(project.getTurkuName()).isEqualTo("Mihriban");
            assertThat(project.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.DRAFT_CREATED);
            assertThat(project.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("bootstrap with all optional fields succeeds")
        void fullBootstrap() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Çanakkale Türküsü");
            req.setRegion("Çanakkale");
            req.setSourceNotes("Halk türküsü, Çanakkale Savaşı dönemine ait");
            req.setMood("hüzünlü, vatansever");
            req.setTargetDurationMinutes(5);
            req.setNarrationStyle("hikâyeci");
            req.setVisualStyle("suluboya Anadolu manzaraları");
            var project = service.bootstrap(req, "admin-full");

            assertThat(project.getTurkuName()).isEqualTo("Çanakkale Türküsü");
            assertThat(project.getRegion()).isEqualTo("Çanakkale");
            assertThat(project.getSourceNotes()).contains("Çanakkale Savaşı");
            assertThat(project.getMood()).contains("hüzünlü");
            assertThat(project.getTargetDurationMinutes()).isEqualTo(5);
            assertThat(project.getNarrationStyle()).isEqualTo("hikâyeci");
            assertThat(project.getVisualStyle()).contains("suluboya");
        }

        @Test
        @DisplayName("bootstrap project is persisted and retrievable")
        void persisted() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Uzun İnce Bir Yoldayım");
            var project = service.bootstrap(req, "admin");

            var retrieved = service.getById(project.getId());
            assertThat(retrieved.getId()).isEqualTo(project.getId());
            assertThat(retrieved.getTurkuName()).isEqualTo("Uzun İnce Bir Yoldayım");
        }
    }

    // ── B. Safe Defaults ────────────────────────────────────

    @Nested
    @DisplayName("B. Safe Defaults")
    class SafeDefaults {

        @Test
        @DisplayName("draft stage by default")
        void draftStage() {
            var project = bootstrapSimple("Test Türkü");
            assertThat(project.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.DRAFT_CREATED);
        }

        @Test
        @DisplayName("needsHumanReview=true by default")
        void humanReview() {
            var project = bootstrapSimple("Test Türkü");
            assertThat(project.isNeedsHumanReview()).isTrue();
        }

        @Test
        @DisplayName("copyrightReviewRequired=true by default")
        void copyrightReview() {
            var project = bootstrapSimple("Test Türkü");
            assertThat(project.isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("sourceConfidence=UNKNOWN when no source notes")
        void unknownConfidence() {
            var project = bootstrapSimple("Test Türkü");
            assertThat(project.getSourceConfidence()).isEqualTo(TurkuSourceConfidence.UNKNOWN);
        }

        @Test
        @DisplayName("sourceConfidence=LOW when source notes provided")
        void lowConfidenceWithNotes() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Mihriban");
            req.setSourceNotes("Atfedilen kaynak mevcut");
            var project = service.bootstrap(req, "admin");
            assertThat(project.getSourceConfidence()).isEqualTo(TurkuSourceConfidence.LOW);
        }

        @Test
        @DisplayName("actor reference stored safely")
        void actorReference() {
            var project = bootstrapSimple("Test");
            assertThat(project.getCreatedByActorReference()).isEqualTo("admin-test");
        }
    }

    // ── C. Validation ───────────────────────────────────────

    @Nested
    @DisplayName("C. Validation")
    class Validation {

        @Test
        @DisplayName("blank türkü name rejected")
        void blankName() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("   ");
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("Türkü adı");
        }

        @Test
        @DisplayName("null türkü name rejected")
        void nullName() {
            var req = new TurkuVideoBootstrapRequest();
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("too long türkü name rejected (>200 chars)")
        void tooLongName() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("a".repeat(201));
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("uzun");
        }

        @Test
        @DisplayName("HTML tags stripped from türkü name")
        void htmlStripped() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Test <b>Bold</b> Türkü");
            var project = service.bootstrap(req, "admin");
            assertThat(project.getTurkuName()).doesNotContain("<b>");
            assertThat(project.getTurkuName()).doesNotContain("</b>");
        }

        @Test
        @DisplayName("script injection rejected")
        void scriptRejected() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("<script>alert('xss')</script>");
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("Güvenli olmayan");
        }

        @Test
        @DisplayName("credential pattern rejected")
        void credentialRejected() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("password=secret123");
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("credential");
        }

        @Test
        @DisplayName("too long region rejected")
        void longRegion() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Mihriban");
            req.setRegion("a".repeat(101));
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("uzun");
        }

        @Test
        @DisplayName("too long source notes rejected")
        void longSourceNotes() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Mihriban");
            req.setSourceNotes("a".repeat(501));
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("duration out of range rejected (0)")
        void durationTooLow() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Mihriban");
            req.setTargetDurationMinutes(0);
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("duration out of range rejected (31)")
        void durationTooHigh() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Mihriban");
            req.setTargetDurationMinutes(31);
            assertThatThrownBy(() -> service.bootstrap(req, "admin"))
                    .isInstanceOf(TurkuVideoProjectException.class);
        }

        @Test
        @DisplayName("unknown project ID returns safe error")
        void unknownId() {
            assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
                    .isInstanceOf(TurkuVideoProjectException.class)
                    .hasMessageContaining("bulunamadı");
        }
    }

    // ── D. Response DTOs ────────────────────────────────────

    @Nested
    @DisplayName("D. Response DTOs")
    class ResponseDTOs {

        @Test
        @DisplayName("bootstrap response has safe fields")
        void bootstrapResponse() {
            var project = bootstrapSimple("Mihriban");
            var resp = TurkuVideoBootstrapResponse.from(project);
            assertThat(resp.getId()).isEqualTo(project.getId());
            assertThat(resp.getTurkuName()).isEqualTo("Mihriban");
            assertThat(resp.getWorkflowStage()).isEqualTo(TurkuVideoWorkflowStage.DRAFT_CREATED);
            assertThat(resp.getCreatedAt()).isNotNull();
            assertThat(resp.getSafeMessage()).isNotBlank();
        }

        @Test
        @DisplayName("project response has safe fields")
        void projectResponse() {
            var req = new TurkuVideoBootstrapRequest();
            req.setTurkuName("Çanakkale");
            req.setRegion("Çanakkale");
            req.setMood("hüzünlü");
            var project = service.bootstrap(req, "admin");
            var resp = TurkuVideoProjectResponse.from(project);

            assertThat(resp.getId()).isEqualTo(project.getId());
            assertThat(resp.getTurkuName()).isEqualTo("Çanakkale");
            assertThat(resp.getRegion()).isEqualTo("Çanakkale");
            assertThat(resp.getMood()).isEqualTo("hüzünlü");
            assertThat(resp.isNeedsHumanReview()).isTrue();
            assertThat(resp.isCopyrightReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("progress response has safe fields")
        void progressResponse() {
            var project = bootstrapSimple("Mihriban");
            var resp = TurkuVideoProgressResponse.from(project);

            assertThat(resp.getId()).isEqualTo(project.getId());
            assertThat(resp.getCurrentStage()).isEqualTo(TurkuVideoWorkflowStage.DRAFT_CREATED);
            assertThat(resp.getNextAvailableAction()).isEqualTo("prepare-story");
            assertThat(resp.getSafeProgressSummary()).isNotBlank();
        }
    }

    // ── E. Forbidden States ─────────────────────────────────

    @Nested
    @DisplayName("E. Forbidden Workflow States")
    class ForbiddenStates {

        @Test
        @DisplayName("no PUBLISHED state exists")
        void noPublished() {
            Set<String> names = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED");
        }

        @Test
        @DisplayName("no EXPORTED state exists")
        void noExported() {
            Set<String> names = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("EXPORTED");
        }

        @Test
        @DisplayName("no PUBLICLY_DELIVERED state exists")
        void noPubliclyDelivered() {
            Set<String> names = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLICLY_DELIVERED");
        }

        @Test
        @DisplayName("no AUTO_APPROVED state exists")
        void noAutoApproved() {
            Set<String> names = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("AUTO_APPROVED");
        }

        @Test
        @DisplayName("no COMPLETED_AS_PUBLIC_VIDEO state exists")
        void noCompletedAsPublic() {
            Set<String> names = Arrays.stream(TurkuVideoWorkflowStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("COMPLETED_AS_PUBLIC_VIDEO");
        }

        @Test
        @DisplayName("exactly 12 allowed stages")
        void exactCount() {
            assertThat(TurkuVideoWorkflowStage.values()).hasSize(12);
        }
    }

    // ── F. No Forbidden DTO Fields ──────────────────────────

    @Nested
    @DisplayName("F. No Forbidden DTO Fields")
    class NoForbiddenFields {

        private static final List<String> FORBIDDEN = List.of(
                "token", "jwt", "session", "credential", "password",
                "secret", "apiKey", "accessKey", "principalRaw",
                "authorizationHeader", "publicUrl", "signedUrl",
                "cdnUrl", "providerUrl", "storageKey", "bucket",
                "objectPath", "rawPayload", "base64", "lyrics");

        @Test
        @DisplayName("TurkuVideoBootstrapRequest no forbidden fields")
        void bootstrapRequest() {
            checkFields(TurkuVideoBootstrapRequest.class);
        }

        @Test
        @DisplayName("TurkuVideoBootstrapResponse no forbidden fields")
        void bootstrapResponse() {
            checkFields(TurkuVideoBootstrapResponse.class);
        }

        @Test
        @DisplayName("TurkuVideoProjectResponse no forbidden fields")
        void projectResponse() {
            checkFields(TurkuVideoProjectResponse.class);
        }

        @Test
        @DisplayName("TurkuVideoProgressResponse no forbidden fields")
        void progressResponse() {
            checkFields(TurkuVideoProgressResponse.class);
        }

        @Test
        @DisplayName("TurkuVideoProject no forbidden fields")
        void projectDomain() {
            checkFields(TurkuVideoProject.class);
        }

        @Test
        @DisplayName("TurkuVideoProjectException no forbidden fields")
        void exception() {
            checkFields(TurkuVideoProjectException.class);
        }

        private void checkFields(Class<?> c) {
            Set<String> fields = Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }

    // ── G. Actor Boundary ───────────────────────────────────

    @Nested
    @DisplayName("G. Actor Boundary")
    class ActorBoundary {

        @Test
        @DisplayName("ADMIN allowed for bootstrap")
        void adminAllowed() {
            var actor = actorResolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.TURKU_PROJECT_BOOTSTRAP, actor)).isTrue();
        }

        @Test
        @DisplayName("REVIEWER allowed for bootstrap")
        void reviewerAllowed() {
            var actor = actorResolver.resolveTestFallback(ReviewActorRole.REVIEWER, "rev");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.TURKU_PROJECT_BOOTSTRAP, actor)).isTrue();
        }

        @Test
        @DisplayName("CREATOR allowed for bootstrap")
        void creatorAllowed() {
            var actor = actorResolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.TURKU_PROJECT_BOOTSTRAP, actor)).isTrue();
        }

        @Test
        @DisplayName("SYSTEM blocked for bootstrap")
        void systemBlocked() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.TURKU_PROJECT_BOOTSTRAP))
                    .doesNotContain(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("SYSTEM_INTERNAL mode blocked for bootstrap")
        void systemInternalBlocked() {
            var actor = actorResolver.resolveSystemInternal("TEST");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.TURKU_PROJECT_BOOTSTRAP, actor)).isFalse();
        }

        @Test
        @DisplayName("actorRole resolves as TEST_FALLBACK for bootstrap")
        void testFallback() {
            var actor = actorResolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
        }

        @Test
        @DisplayName("enforce passes for ADMIN bootstrap")
        void enforceAdmin() {
            var actor = actorResolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.TURKU_PROJECT_BOOTSTRAP, actor);
        }

        @Test
        @DisplayName("enforce throws for SYSTEM bootstrap attempt")
        void enforceSystem() {
            var actor = actorResolver.resolveSystemInternal("SYSTEM");
            assertThatThrownBy(() -> AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.TURKU_PROJECT_BOOTSTRAP, actor))
                    .isInstanceOf(AdminActorResolutionException.class);
        }
    }

    // ── H. Exception Safety ─────────────────────────────────

    @Nested
    @DisplayName("H. Exception Safety")
    class ExceptionSafety {

        @Test
        @DisplayName("exception toString is safe")
        void safeToString() {
            var ex = TurkuVideoProjectException.turkuNameRequired();
            String str = ex.toString();
            assertThat(str).doesNotContain("stackTrace");
            assertThat(str).doesNotContain("token");
            assertThat(str).doesNotContain("jwt");
            assertThat(str).contains("TURKU_NAME_REQUIRED");
        }

        @Test
        @DisplayName("exception does not leak credential data")
        void noCredentialLeak() {
            var ex = TurkuVideoProjectException.unsafeInput("test");
            assertThat(ex.getMessage()).doesNotContain("password");
            assertThat(ex.getMessage()).doesNotContain("secret");
        }
    }

    // ── I. Endpoint Inventory ───────────────────────────────

    @Nested
    @DisplayName("I. Endpoint Inventory (30 = 16 POST + 14 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("TurkuVideoProjectController: 7 POST + 2 GET")
        void turkuVideoController() {
            long post = countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).isEqualTo(7);
            assertThat(get).isEqualTo(2);
        }

        @Test
        @DisplayName("AdminReviewController: 9 POST + 8 GET (unchanged)")
        void reviewController() {
            assertThat(countMethods(AdminReviewController.class, PostMapping.class)).isEqualTo(9);
            assertThat(countMethods(AdminReviewController.class, GetMapping.class)).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminPipelineController: 0 POST + 2 GET (unchanged)")
        void pipelineController() {
            assertThat(countMethods(AdminPipelineController.class, PostMapping.class)).isZero();
            assertThat(countMethods(AdminPipelineController.class, GetMapping.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("AdminWorkflowObservabilityController: 0 POST + 2 GET (unchanged)")
        void observabilityController() {
            assertThat(countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)).isZero();
            assertThat(countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("total 30 = 16 POST + 14 GET")
        void total() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + countMethods(TurkuVideoProjectController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + countMethods(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("total POST").isEqualTo(16);
            assertThat(get).as("total GET").isEqualTo(14);
            assertThat(post + get).as("total endpoints").isEqualTo(30);
        }

        @Test
        @DisplayName("no PUT on türkü controller")
        void noPut() {
            assertThat(countMethods(TurkuVideoProjectController.class, PutMapping.class)).isZero();
        }

        @Test
        @DisplayName("no DELETE on türkü controller")
        void noDelete() {
            assertThat(countMethods(TurkuVideoProjectController.class, DeleteMapping.class)).isZero();
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── J. Source Confidence Enum ────────────────────────────

    @Nested
    @DisplayName("J. Source Confidence Enum")
    class SourceConfidenceTests {

        @Test
        @DisplayName("has exactly 4 values")
        void exactValues() {
            assertThat(TurkuSourceConfidence.values()).hasSize(4);
        }

        @Test
        @DisplayName("contains UNKNOWN, LOW, MEDIUM, HIGH")
        void expectedValues() {
            Set<String> names = Arrays.stream(TurkuSourceConfidence.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).containsExactlyInAnyOrder("UNKNOWN", "LOW", "MEDIUM", "HIGH");
        }
    }

    // ── K. Controller Dependencies ──────────────────────────

    @Nested
    @DisplayName("K. Controller Dependencies")
    class ControllerDeps {

        @Test
        @DisplayName("controller has AdminActorResolver")
        void hasResolver() {
            assertThat(Arrays.stream(TurkuVideoProjectController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet()))
                    .contains(AdminActorResolver.class);
        }

        @Test
        @DisplayName("controller has TurkuVideoProjectBootstrapService")
        void hasService() {
            assertThat(Arrays.stream(TurkuVideoProjectController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet()))
                    .contains(TurkuVideoProjectBootstrapService.class);
        }
    }

    // ── Helper ──────────────────────────────────────────────

    private TurkuVideoProject bootstrapSimple(String turkuName) {
        var req = new TurkuVideoBootstrapRequest();
        req.setTurkuName(turkuName);
        return service.bootstrap(req, "admin-test");
    }
}
