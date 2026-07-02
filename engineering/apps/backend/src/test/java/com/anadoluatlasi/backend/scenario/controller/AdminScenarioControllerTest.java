package com.anadoluatlasi.backend.scenario.controller;

import com.anadoluatlasi.backend.application.scenario.ScenarioBeatEditCommand;
import com.anadoluatlasi.backend.application.scenario.ScenarioDraftEditCommand;
import com.anadoluatlasi.backend.application.scenario.ScenarioGenerationResult;
import com.anadoluatlasi.backend.application.scenario.ScenarioGenerationService;
import com.anadoluatlasi.backend.application.scenario.ScenarioQueryService;
import com.anadoluatlasi.backend.application.scenario.ScenarioReviewResult;
import com.anadoluatlasi.backend.application.scenario.ScenarioReviewService;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioBeatEditRequest;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioDraftEditRequest;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioDraftResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioDraftSummaryResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioGenerateResponse;
import com.anadoluatlasi.backend.scenario.controller.dto.ScenarioRejectRequest;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeat;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-002: AdminScenarioController unit testleri.
 *
 * <p>Kontrol noktaları:</p>
 * <ul>
 *   <li>Controller thin kalır — iş kuralı yok</li>
 *   <li>Internal AI contract DTO'ları expose edilmez</li>
 *   <li>Generic status update endpoint yok</li>
 *   <li>Approve/reject explicit action endpoint</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminScenarioControllerTest {

    @Mock
    private ScenarioGenerationService scenarioGenerationService;
    @Mock
    private ScenarioReviewService scenarioReviewService;
    @Mock
    private ScenarioQueryService scenarioQueryService;

    private AdminScenarioController controller;
    private ContentProject project;
    private ContentDraft sourceDraft;

    @BeforeEach
    void setUp() {
        controller = new AdminScenarioController(
                scenarioGenerationService, scenarioReviewService, scenarioQueryService);

        project = ContentProject.builder().id(UUID.randomUUID()).build();
        sourceDraft = ContentDraft.builder().id(UUID.randomUUID()).build();
    }

    private ScenarioDraft buildDraft(ScenarioDraftStatus status) {
        ScenarioDraft draft = ScenarioDraft.builder()
                .contentProject(project)
                .sourceDraft(sourceDraft)
                .version(1)
                .status(status)
                .title("Test Senaryo")
                .summary("Test özet")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-v1")
                .build();
        draft.prePersist();

        List<ScenarioBeat> beats = new ArrayList<>();
        for (ScenarioBeatType type : ScenarioBeatType.values()) {
            ScenarioBeat beat = ScenarioBeat.builder()
                    .scenarioDraft(draft)
                    .type(type)
                    .label(type.getLabel())
                    .body("Beat: " + type.name())
                    .orderIndex(type.getOrder())
                    .estimatedDurationSeconds(30)
                    .build();
            beat.prePersist();
            beats.add(beat);
        }
        draft.getBeats().addAll(beats);
        return draft;
    }

    // ── Generate Endpoint Tests ─────────────────────────────

    @Nested
    @DisplayName("Generate endpoint")
    class GenerateTests {

        @Test
        @DisplayName("Generate endpoint başarılı senaryo yanıtı döndürür")
        void generateReturnsCreatedScenarioResponse() {
            UUID projectId = project.getId();
            ScenarioGenerationResult result = ScenarioGenerationResult.builder()
                    .success(true)
                    .contentProjectId(projectId)
                    .scenarioDraftId(UUID.randomUUID())
                    .version(1)
                    .status("AI_GENERATED")
                    .scenarioTitle("Test Senaryo")
                    .message("Başarılı")
                    .build();

            when(scenarioGenerationService.generateScenarioForContentProject(
                    eq(projectId), anyString())).thenReturn(result);

            ResponseEntity<ApiResponse<ScenarioGenerateResponse>> response =
                    controller.generate(projectId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getStatus()).isEqualTo("AI_GENERATED");
        }

        @Test
        @DisplayName("Generate endpoint internal AI contract field expose etmez")
        void generateDoesNotExposeInternalAiFields() {
            Set<String> fieldNames = Arrays.stream(ScenarioGenerateResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            // Internal AI contract fields olmamalı
            assertThat(fieldNames).doesNotContain("prompt", "rawPrompt", "rawPayload",
                    "providerPayload", "apiKey", "secret", "agentRequest", "agentResponse");
        }

        @Test
        @DisplayName("Generate endpoint raw prompt/provider payload expose etmez")
        void generateDoesNotExposeRawPromptOrPayload() {
            Set<String> fieldNames = Arrays.stream(ScenarioGenerateResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("prompt", "rawPrompt", "rawPayload",
                    "providerPayload", "requestBody", "responseBody");
        }
    }

    // ── List/Detail Endpoint Tests ──────────────────────────

    @Nested
    @DisplayName("List/Detail endpoints")
    class ListDetailTests {

        @Test
        @DisplayName("List endpoint version desc sıralı döndürür")
        void listReturnsOrderedByVersionDesc() {
            UUID projectId = project.getId();
            ScenarioDraft v2 = buildDraft(ScenarioDraftStatus.AI_GENERATED);
            v2.setVersion(2);
            ScenarioDraft v1 = buildDraft(ScenarioDraftStatus.REJECTED);
            v1.setVersion(1);

            when(scenarioQueryService.listByContentProject(projectId))
                    .thenReturn(List.of(v2, v1));

            ResponseEntity<ApiResponse<List<ScenarioDraftSummaryResponse>>> response =
                    controller.list(projectId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<ScenarioDraftSummaryResponse> data = response.getBody().getData();
            assertThat(data).hasSize(2);
            assertThat(data.get(0).getVersion()).isEqualTo(2);
            assertThat(data.get(1).getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Detail endpoint 6 beat ile senaryo döndürür")
        void detailReturnsScenarioWith6Beats() {
            ScenarioDraft draft = buildDraft(ScenarioDraftStatus.AI_GENERATED);
            when(scenarioQueryService.getDetail(draft.getId())).thenReturn(draft);

            ResponseEntity<ApiResponse<ScenarioDraftResponse>> response =
                    controller.getDetail(draft.getId());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            ScenarioDraftResponse data = response.getBody().getData();
            assertThat(data.getBeats()).hasSize(6);
            assertThat(data.getStatus()).isEqualTo("AI_GENERATED");
        }
    }

    // ── Edit Endpoint Tests ─────────────────────────────────

    @Nested
    @DisplayName("Edit endpoint")
    class EditTests {

        @Test
        @DisplayName("Edit endpoint review service üzerinden günceller")
        void editUpdatesViaReviewService() {
            ScenarioDraft draft = buildDraft(ScenarioDraftStatus.NEEDS_REVIEW);
            UUID scenarioId = draft.getId();

            when(scenarioReviewService.editScenarioDraft(eq(scenarioId), any(), anyString()))
                    .thenReturn(ScenarioReviewResult.builder()
                            .success(true).scenarioDraftId(scenarioId)
                            .contentProjectId(project.getId())
                            .status("NEEDS_REVIEW").build());
            when(scenarioQueryService.getDetail(scenarioId)).thenReturn(draft);

            ScenarioDraftEditRequest request = ScenarioDraftEditRequest.builder()
                    .title("Yeni Başlık")
                    .build();

            ResponseEntity<ApiResponse<ScenarioDraftResponse>> response =
                    controller.edit(scenarioId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(scenarioReviewService).editScenarioDraft(eq(scenarioId), any(), anyString());
        }

        @Test
        @DisplayName("Edit AI_GENERATED senaryoda NEEDS_REVIEW döndürür")
        void editReturnsNeedsReviewForAiGenerated() {
            ScenarioDraft draft = buildDraft(ScenarioDraftStatus.NEEDS_REVIEW);
            UUID scenarioId = draft.getId();

            when(scenarioReviewService.editScenarioDraft(eq(scenarioId), any(), anyString()))
                    .thenReturn(ScenarioReviewResult.builder()
                            .success(true).scenarioDraftId(scenarioId)
                            .contentProjectId(project.getId())
                            .status("NEEDS_REVIEW").build());
            when(scenarioQueryService.getDetail(scenarioId)).thenReturn(draft);

            ScenarioDraftEditRequest request = ScenarioDraftEditRequest.builder()
                    .title("Düzenlenmiş").build();

            ResponseEntity<ApiResponse<ScenarioDraftResponse>> response =
                    controller.edit(scenarioId, request);

            assertThat(response.getBody().getData().getStatus()).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("Edit endpoint senaryoyu onaylamaz")
        void editDoesNotApproveScenario() {
            ScenarioDraft draft = buildDraft(ScenarioDraftStatus.NEEDS_REVIEW);
            UUID scenarioId = draft.getId();

            when(scenarioReviewService.editScenarioDraft(eq(scenarioId), any(), anyString()))
                    .thenReturn(ScenarioReviewResult.builder()
                            .success(true).scenarioDraftId(scenarioId)
                            .contentProjectId(project.getId())
                            .status("NEEDS_REVIEW").build());
            when(scenarioQueryService.getDetail(scenarioId)).thenReturn(draft);

            ScenarioDraftEditRequest request = ScenarioDraftEditRequest.builder()
                    .title("Test").build();

            controller.edit(scenarioId, request);

            ArgumentCaptor<ScenarioDraftEditCommand> captor =
                    ArgumentCaptor.forClass(ScenarioDraftEditCommand.class);
            verify(scenarioReviewService).editScenarioDraft(eq(scenarioId), captor.capture(), anyString());

            // editScenarioDraft çağrıldı, approveScenarioDraft çağrılmadı
            verify(scenarioReviewService, never()).approveScenarioDraft(any(), anyString());
        }

        @Test
        @DisplayName("Edit endpoint internal AI DTO expose etmez")
        void editDoesNotExposeInternalAiDto() {
            Set<String> fieldNames = Arrays.stream(ScenarioDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("agentRequest", "agentResponse",
                    "prompt", "rawPrompt", "rawPayload", "apiKey", "secret");
        }
    }

    // ── Approve Endpoint Tests ──────────────────────────────

    @Nested
    @DisplayName("Approve endpoint")
    class ApproveTests {

        @Test
        @DisplayName("Approve endpoint explicit senaryo onayı yapar")
        void approveExplicitlyApprovesScenario() {
            UUID scenarioId = UUID.randomUUID();
            when(scenarioReviewService.approveScenarioDraft(eq(scenarioId), anyString()))
                    .thenReturn(ScenarioReviewResult.builder()
                            .success(true).scenarioDraftId(scenarioId)
                            .contentProjectId(project.getId())
                            .status("APPROVED").build());

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.approve(scenarioId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("APPROVED");
            verify(scenarioReviewService).approveScenarioDraft(eq(scenarioId), anyString());
        }

        @Test
        @DisplayName("Approve endpoint READY_FOR_PUBLISH yapmaz")
        void approveDoesNotSetReadyForPublish() {
            UUID scenarioId = UUID.randomUUID();
            when(scenarioReviewService.approveScenarioDraft(eq(scenarioId), anyString()))
                    .thenReturn(ScenarioReviewResult.builder()
                            .success(true).scenarioDraftId(scenarioId)
                            .contentProjectId(project.getId())
                            .status("APPROVED").build());

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.approve(scenarioId);

            assertThat(response.getBody().getData().getStatus()).isNotEqualTo("READY_FOR_PUBLISH");
        }
    }

    // ── Reject Endpoint Tests ───────────────────────────────

    @Nested
    @DisplayName("Reject endpoint")
    class RejectTests {

        @Test
        @DisplayName("Reject endpoint explicit senaryo reddi yapar")
        void rejectExplicitlyRejectsScenario() {
            UUID scenarioId = UUID.randomUUID();
            when(scenarioReviewService.rejectScenarioDraft(eq(scenarioId), anyString(), anyString()))
                    .thenReturn(ScenarioReviewResult.builder()
                            .success(true).scenarioDraftId(scenarioId)
                            .contentProjectId(project.getId())
                            .status("REJECTED").build());

            ScenarioRejectRequest request = ScenarioRejectRequest.builder()
                    .reason("İçerik yetersiz").build();

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.reject(scenarioId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Reject endpoint red sebebini service'e iletir")
        void rejectPassesReasonToService() {
            UUID scenarioId = UUID.randomUUID();
            when(scenarioReviewService.rejectScenarioDraft(eq(scenarioId), anyString(), anyString()))
                    .thenReturn(ScenarioReviewResult.builder()
                            .success(true).scenarioDraftId(scenarioId)
                            .contentProjectId(project.getId())
                            .status("REJECTED").build());

            ScenarioRejectRequest request = ScenarioRejectRequest.builder()
                    .reason("Tarihsel doğruluk yetersiz").build();

            controller.reject(scenarioId, request);

            verify(scenarioReviewService).rejectScenarioDraft(
                    eq(scenarioId), anyString(), eq("Tarihsel doğruluk yetersiz"));
        }
    }

    // ── Governance / API Surface Tests ──────────────────────

    @Nested
    @DisplayName("Governance / API surface")
    class GovernanceTests {

        @Test
        @DisplayName("Generic status update endpoint yok")
        void noGenericStatusUpdateEndpoint() {
            Method[] methods = AdminScenarioController.class.getDeclaredMethods();
            for (Method method : methods) {
                // PATCH/PUT/POST .../status endpoint olmamalı
                if (method.isAnnotationPresent(PostMapping.class)) {
                    PostMapping mapping = method.getAnnotation(PostMapping.class);
                    for (String path : mapping.value()) {
                        assertThat(path).doesNotEndWith("/status");
                    }
                }
                if (method.isAnnotationPresent(PutMapping.class)) {
                    PutMapping mapping = method.getAnnotation(PutMapping.class);
                    for (String path : mapping.value()) {
                        assertThat(path).doesNotEndWith("/status");
                    }
                }
            }
        }

        @Test
        @DisplayName("REST DTO ScenarioAgentRequest expose etmez")
        void restDtoDoesNotExposeScenarioAgentRequest() {
            Set<String> fieldNames = Arrays.stream(ScenarioDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("agentRequest", "scenarioAgentRequest");
        }

        @Test
        @DisplayName("REST DTO ScenarioAgentResponse expose etmez")
        void restDtoDoesNotExposeScenarioAgentResponse() {
            Set<String> fieldNames = Arrays.stream(ScenarioDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("agentResponse", "scenarioAgentResponse");
        }

        @Test
        @DisplayName("REST DTO raw provider payload/prompt/secret field expose etmez")
        void restDtoDoesNotExposeRawFields() {
            Set<String> allFields = Arrays.stream(ScenarioDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            allFields.addAll(Arrays.stream(ScenarioGenerateResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet()));

            assertThat(allFields).doesNotContain(
                    "prompt", "rawPrompt", "rawPayload", "providerPayload",
                    "requestBody", "responseBody", "apiKey", "secret",
                    "accessToken", "authorization");
        }

        @Test
        @DisplayName("Controller @RestController ve doğru base path kullanır")
        void controllerHasCorrectAnnotations() {
            assertThat(AdminScenarioController.class.isAnnotationPresent(RestController.class)).isTrue();
            RequestMapping mapping = AdminScenarioController.class.getAnnotation(RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin");
        }
    }
}
