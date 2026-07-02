package com.anadoluatlasi.backend.storyboard.controller;

import com.anadoluatlasi.backend.application.storyboard.StoryboardDraftEditCommand;
import com.anadoluatlasi.backend.application.storyboard.StoryboardGenerationResult;
import com.anadoluatlasi.backend.application.storyboard.StoryboardGenerationService;
import com.anadoluatlasi.backend.application.storyboard.StoryboardQueryService;
import com.anadoluatlasi.backend.application.storyboard.StoryboardReviewResult;
import com.anadoluatlasi.backend.application.storyboard.StoryboardReviewService;
import com.anadoluatlasi.backend.application.storyboard.StoryboardSceneEditCommand;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardDraftEditRequest;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardDraftResponse;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardDraftSummaryResponse;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardGenerateResponse;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardRejectRequest;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardSceneEditRequest;
import com.anadoluatlasi.backend.storyboard.controller.dto.StoryboardSceneResponse;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
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
 * FP-003: AdminStoryboardController unit testleri.
 *
 * <p>Kontrol noktaları:</p>
 * <ul>
 *   <li>Controller thin kalır — iş kuralı yok</li>
 *   <li>Internal AI contract DTO'ları expose edilmez</li>
 *   <li>Generic status update endpoint yok</li>
 *   <li>Approve/reject explicit action endpoint</li>
 *   <li>READY_FOR_PUBLISH transition yok</li>
 *   <li>Raw prompt/provider payload/API key/secret expose edilmez</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminStoryboardControllerTest {

    @Mock
    private StoryboardGenerationService storyboardGenerationService;
    @Mock
    private StoryboardReviewService storyboardReviewService;
    @Mock
    private StoryboardQueryService storyboardQueryService;

    private AdminStoryboardController controller;
    private ContentProject project;
    private ContentDraft sourceDraft;
    private ScenarioDraft sourceScenarioDraft;

    @BeforeEach
    void setUp() {
        controller = new AdminStoryboardController(
                storyboardGenerationService, storyboardReviewService, storyboardQueryService);

        project = ContentProject.builder().id(UUID.randomUUID()).build();
        sourceDraft = ContentDraft.builder().id(UUID.randomUUID()).build();
        sourceScenarioDraft = ScenarioDraft.builder()
                .contentProject(project)
                .sourceDraft(sourceDraft)
                .version(1)
                .build();
        sourceScenarioDraft.prePersist();
    }

    private StoryboardDraft buildDraft(StoryboardDraftStatus status) {
        StoryboardDraft draft = StoryboardDraft.builder()
                .contentProject(project)
                .sourceDraft(sourceDraft)
                .sourceScenarioDraft(sourceScenarioDraft)
                .sourceScenarioVersion(1)
                .version(1)
                .status(status)
                .title("Test Storyboard")
                .summary("Test özet")
                .targetPlatform("YouTube")
                .aspectRatio("16:9")
                .estimatedDurationSeconds(180)
                .visualStyleGuide("Sinematik")
                .narrationStyle("Anlatıcı")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-v1")
                .build();
        draft.prePersist();

        List<StoryboardScene> scenes = new ArrayList<>();
        ScenarioBeatType[] beatTypes = ScenarioBeatType.values();
        for (int i = 0; i < Math.min(beatTypes.length, 3); i++) {
            StoryboardScene scene = StoryboardScene.builder()
                    .storyboardDraft(draft)
                    .sceneNumber(i + 1)
                    .sourceBeatType(beatTypes[i])
                    .sourceBeatOrder(i)
                    .sceneTitle("Sahne " + (i + 1))
                    .visualDescription("Görsel açıklama " + (i + 1))
                    .narrationText("Narration " + (i + 1))
                    .cameraDirection("wide shot")
                    .transitionNote("fade")
                    .mood("dramatic")
                    .location("Anadolu")
                    .characters("Anlatıcı")
                    .estimatedDurationSeconds(30)
                    .build();
            scene.prePersist();
            scenes.add(scene);
        }
        draft.getScenes().addAll(scenes);
        return draft;
    }

    // ── Generate Endpoint Tests ─────────────────────────────

    @Nested
    @DisplayName("Generate endpoint")
    class GenerateTests {

        @Test
        @DisplayName("Generate endpoint başarılı storyboard yanıtı döndürür")
        void generateReturnsCreatedStoryboardResponse() {
            UUID scenarioId = sourceScenarioDraft.getId();
            StoryboardGenerationResult result = StoryboardGenerationResult.builder()
                    .success(true)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(UUID.randomUUID())
                    .version(1)
                    .status("AI_GENERATED")
                    .storyboardTitle("Test Storyboard")
                    .sceneCount(8)
                    .message("Başarılı")
                    .build();

            when(storyboardGenerationService.generateStoryboardForScenario(
                    eq(scenarioId), anyString())).thenReturn(result);

            ResponseEntity<ApiResponse<StoryboardGenerateResponse>> response =
                    controller.generate(scenarioId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getStatus()).isEqualTo("AI_GENERATED");
            assertThat(response.getBody().getData().getSceneCount()).isEqualTo(8);
        }

        @Test
        @DisplayName("Generate endpoint başarısız durumda OK döndürür")
        void generateReturnsOkOnFailure() {
            UUID scenarioId = sourceScenarioDraft.getId();
            StoryboardGenerationResult result = StoryboardGenerationResult.builder()
                    .success(false)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(UUID.randomUUID())
                    .status("FAILED")
                    .message("AI hatası")
                    .errorCode("PROVIDER_ERROR")
                    .build();

            when(storyboardGenerationService.generateStoryboardForScenario(
                    eq(scenarioId), anyString())).thenReturn(result);

            ResponseEntity<ApiResponse<StoryboardGenerateResponse>> response =
                    controller.generate(scenarioId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().isSuccess()).isFalse();
            assertThat(response.getBody().getData().getStatus()).isEqualTo("FAILED");
            assertThat(response.getBody().getData().getErrorCode()).isEqualTo("PROVIDER_ERROR");
        }

        @Test
        @DisplayName("Generate endpoint internal AI contract field expose etmez")
        void generateDoesNotExposeInternalAiFields() {
            Set<String> fieldNames = Arrays.stream(StoryboardGenerateResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("prompt", "rawPrompt", "rawPayload",
                    "providerPayload", "apiKey", "secret", "agentRequest", "agentResponse");
        }

        @Test
        @DisplayName("Generate endpoint raw prompt/provider payload expose etmez")
        void generateDoesNotExposeRawPromptOrPayload() {
            Set<String> fieldNames = Arrays.stream(StoryboardGenerateResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("prompt", "rawPrompt", "rawPayload",
                    "providerPayload", "requestBody", "responseBody");
        }

        @Test
        @DisplayName("Generate endpoint scenarioDraftId'yi service'e iletir")
        void generatePassesScenarioIdToService() {
            UUID scenarioId = sourceScenarioDraft.getId();
            StoryboardGenerationResult result = StoryboardGenerationResult.builder()
                    .success(true)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(UUID.randomUUID())
                    .version(1)
                    .status("AI_GENERATED")
                    .storyboardTitle("Test")
                    .sceneCount(3)
                    .message("OK")
                    .build();

            when(storyboardGenerationService.generateStoryboardForScenario(
                    eq(scenarioId), anyString())).thenReturn(result);

            controller.generate(scenarioId);

            verify(storyboardGenerationService).generateStoryboardForScenario(
                    eq(scenarioId), anyString());
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
            StoryboardDraft v2 = buildDraft(StoryboardDraftStatus.AI_GENERATED);
            v2.setVersion(2);
            StoryboardDraft v1 = buildDraft(StoryboardDraftStatus.REJECTED);
            v1.setVersion(1);

            when(storyboardQueryService.listByContentProject(projectId))
                    .thenReturn(List.of(v2, v1));

            ResponseEntity<ApiResponse<List<StoryboardDraftSummaryResponse>>> response =
                    controller.list(projectId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<StoryboardDraftSummaryResponse> data = response.getBody().getData();
            assertThat(data).hasSize(2);
            assertThat(data.get(0).getVersion()).isEqualTo(2);
            assertThat(data.get(1).getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("List endpoint scene count döndürür")
        void listReturnsSceneCount() {
            UUID projectId = project.getId();
            StoryboardDraft draft = buildDraft(StoryboardDraftStatus.AI_GENERATED);

            when(storyboardQueryService.listByContentProject(projectId))
                    .thenReturn(List.of(draft));

            ResponseEntity<ApiResponse<List<StoryboardDraftSummaryResponse>>> response =
                    controller.list(projectId);

            List<StoryboardDraftSummaryResponse> data = response.getBody().getData();
            assertThat(data).hasSize(1);
            assertThat(data.get(0).getSceneCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Detail endpoint scene'lerle storyboard döndürür")
        void detailReturnsStoryboardWithScenes() {
            StoryboardDraft draft = buildDraft(StoryboardDraftStatus.AI_GENERATED);
            when(storyboardQueryService.getDetail(draft.getId())).thenReturn(draft);

            ResponseEntity<ApiResponse<StoryboardDraftResponse>> response =
                    controller.getDetail(draft.getId());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            StoryboardDraftResponse data = response.getBody().getData();
            assertThat(data.getScenes()).hasSize(3);
            assertThat(data.getStatus()).isEqualTo("AI_GENERATED");
        }

        @Test
        @DisplayName("Detail endpoint source referanslarını döndürür")
        void detailReturnsSourceReferences() {
            StoryboardDraft draft = buildDraft(StoryboardDraftStatus.AI_GENERATED);
            when(storyboardQueryService.getDetail(draft.getId())).thenReturn(draft);

            ResponseEntity<ApiResponse<StoryboardDraftResponse>> response =
                    controller.getDetail(draft.getId());

            StoryboardDraftResponse data = response.getBody().getData();
            assertThat(data.getContentProjectId()).isEqualTo(project.getId());
            assertThat(data.getSourceDraftId()).isEqualTo(sourceDraft.getId());
            assertThat(data.getSourceScenarioDraftId()).isEqualTo(sourceScenarioDraft.getId());
            assertThat(data.getSourceScenarioVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Detail endpoint scene alanlarını doğru eşler")
        void detailMapsSceneFieldsCorrectly() {
            StoryboardDraft draft = buildDraft(StoryboardDraftStatus.AI_GENERATED);
            when(storyboardQueryService.getDetail(draft.getId())).thenReturn(draft);

            ResponseEntity<ApiResponse<StoryboardDraftResponse>> response =
                    controller.getDetail(draft.getId());

            StoryboardSceneResponse scene = response.getBody().getData().getScenes().get(0);
            assertThat(scene.getSceneNumber()).isEqualTo(1);
            assertThat(scene.getSceneTitle()).isEqualTo("Sahne 1");
            assertThat(scene.getVisualDescription()).isEqualTo("Görsel açıklama 1");
            assertThat(scene.getNarrationText()).isEqualTo("Narration 1");
            assertThat(scene.getCameraDirection()).isEqualTo("wide shot");
            assertThat(scene.getTransitionNote()).isEqualTo("fade");
            assertThat(scene.getMood()).isEqualTo("dramatic");
            assertThat(scene.getLocation()).isEqualTo("Anadolu");
            assertThat(scene.getCharacters()).isEqualTo("Anlatıcı");
            assertThat(scene.getEstimatedDurationSeconds()).isEqualTo(30);
            assertThat(scene.getSourceBeatType()).isNotNull();
        }

        @Test
        @DisplayName("Detail endpoint storyboard metadata alanlarını döndürür")
        void detailReturnsStoryboardMetadataFields() {
            StoryboardDraft draft = buildDraft(StoryboardDraftStatus.AI_GENERATED);
            when(storyboardQueryService.getDetail(draft.getId())).thenReturn(draft);

            ResponseEntity<ApiResponse<StoryboardDraftResponse>> response =
                    controller.getDetail(draft.getId());

            StoryboardDraftResponse data = response.getBody().getData();
            assertThat(data.getTargetPlatform()).isEqualTo("YouTube");
            assertThat(data.getAspectRatio()).isEqualTo("16:9");
            assertThat(data.getEstimatedDurationSeconds()).isEqualTo(180);
            assertThat(data.getVisualStyleGuide()).isEqualTo("Sinematik");
            assertThat(data.getNarrationStyle()).isEqualTo("Anlatıcı");
            assertThat(data.getProviderType()).isEqualTo("MOCK");
            assertThat(data.getModelName()).isEqualTo("mock-v1");
        }
    }

    // ── Edit Endpoint Tests ─────────────────────────────────

    @Nested
    @DisplayName("Edit endpoint")
    class EditTests {

        @Test
        @DisplayName("Edit endpoint review service üzerinden günceller")
        void editUpdatesViaReviewService() {
            StoryboardDraft draft = buildDraft(StoryboardDraftStatus.NEEDS_REVIEW);
            UUID storyboardId = draft.getId();

            when(storyboardReviewService.editStoryboardDraft(eq(storyboardId), any(), anyString()))
                    .thenReturn(StoryboardReviewResult.builder()
                            .success(true).storyboardDraftId(storyboardId)
                            .contentProjectId(project.getId())
                            .status("NEEDS_REVIEW").build());
            when(storyboardQueryService.getDetail(storyboardId)).thenReturn(draft);

            StoryboardDraftEditRequest request = StoryboardDraftEditRequest.builder()
                    .title("Yeni Başlık")
                    .build();

            ResponseEntity<ApiResponse<StoryboardDraftResponse>> response =
                    controller.edit(storyboardId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(storyboardReviewService).editStoryboardDraft(eq(storyboardId), any(), anyString());
        }

        @Test
        @DisplayName("Edit scene düzenlemelerini doğru eşler")
        void editMapsSceneEditsCorrectly() {
            StoryboardDraft draft = buildDraft(StoryboardDraftStatus.NEEDS_REVIEW);
            UUID storyboardId = draft.getId();

            when(storyboardReviewService.editStoryboardDraft(eq(storyboardId), any(), anyString()))
                    .thenReturn(StoryboardReviewResult.builder()
                            .success(true).storyboardDraftId(storyboardId)
                            .contentProjectId(project.getId())
                            .status("NEEDS_REVIEW").build());
            when(storyboardQueryService.getDetail(storyboardId)).thenReturn(draft);

            StoryboardSceneEditRequest sceneEdit = StoryboardSceneEditRequest.builder()
                    .sceneNumber(1)
                    .sceneTitle("Düzenlenmiş Sahne")
                    .visualDescription("Yeni görsel")
                    .mood("melancholic")
                    .build();

            StoryboardDraftEditRequest request = StoryboardDraftEditRequest.builder()
                    .title("Düzenlenmiş Başlık")
                    .scenes(List.of(sceneEdit))
                    .build();

            controller.edit(storyboardId, request);

            ArgumentCaptor<StoryboardDraftEditCommand> captor =
                    ArgumentCaptor.forClass(StoryboardDraftEditCommand.class);
            verify(storyboardReviewService).editStoryboardDraft(eq(storyboardId), captor.capture(), anyString());

            StoryboardDraftEditCommand command = captor.getValue();
            assertThat(command.getTitle()).isEqualTo("Düzenlenmiş Başlık");
            assertThat(command.getScenes()).hasSize(1);

            StoryboardSceneEditCommand sceneCmd = command.getScenes().get(0);
            assertThat(sceneCmd.getSceneNumber()).isEqualTo(1);
            assertThat(sceneCmd.getSceneTitle()).isEqualTo("Düzenlenmiş Sahne");
            assertThat(sceneCmd.getVisualDescription()).isEqualTo("Yeni görsel");
            assertThat(sceneCmd.getMood()).isEqualTo("melancholic");
        }

        @Test
        @DisplayName("Edit endpoint storyboard'u onaylamaz")
        void editDoesNotApproveStoryboard() {
            StoryboardDraft draft = buildDraft(StoryboardDraftStatus.NEEDS_REVIEW);
            UUID storyboardId = draft.getId();

            when(storyboardReviewService.editStoryboardDraft(eq(storyboardId), any(), anyString()))
                    .thenReturn(StoryboardReviewResult.builder()
                            .success(true).storyboardDraftId(storyboardId)
                            .contentProjectId(project.getId())
                            .status("NEEDS_REVIEW").build());
            when(storyboardQueryService.getDetail(storyboardId)).thenReturn(draft);

            StoryboardDraftEditRequest request = StoryboardDraftEditRequest.builder()
                    .title("Test").build();

            controller.edit(storyboardId, request);

            // editStoryboardDraft çağrıldı, approveStoryboardDraft çağrılmadı
            verify(storyboardReviewService, never()).approveStoryboardDraft(any(), anyString());
        }

        @Test
        @DisplayName("Edit request DTO status alanı içermez")
        void editRequestDoesNotContainStatusField() {
            Set<String> fieldNames = Arrays.stream(StoryboardDraftEditRequest.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("status", "version", "providerType",
                    "modelName", "generatedBy", "prompt", "rawPrompt", "apiKey");
        }

        @Test
        @DisplayName("Edit scene request DTO unsafe field içermez")
        void editSceneRequestDoesNotContainUnsafeFields() {
            Set<String> fieldNames = Arrays.stream(StoryboardSceneEditRequest.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("status", "id", "storyboardDraftId",
                    "prompt", "rawPrompt", "apiKey", "secret");
        }
    }

    // ── Approve Endpoint Tests ──────────────────────────────

    @Nested
    @DisplayName("Approve endpoint")
    class ApproveTests {

        @Test
        @DisplayName("Approve endpoint explicit storyboard onayı yapar")
        void approveExplicitlyApprovesStoryboard() {
            UUID storyboardId = UUID.randomUUID();
            when(storyboardReviewService.approveStoryboardDraft(eq(storyboardId), anyString()))
                    .thenReturn(StoryboardReviewResult.builder()
                            .success(true).storyboardDraftId(storyboardId)
                            .contentProjectId(project.getId())
                            .status("APPROVED").build());

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.approve(storyboardId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("APPROVED");
            verify(storyboardReviewService).approveStoryboardDraft(eq(storyboardId), anyString());
        }

        @Test
        @DisplayName("Approve endpoint READY_FOR_PUBLISH yapmaz")
        void approveDoesNotSetReadyForPublish() {
            UUID storyboardId = UUID.randomUUID();
            when(storyboardReviewService.approveStoryboardDraft(eq(storyboardId), anyString()))
                    .thenReturn(StoryboardReviewResult.builder()
                            .success(true).storyboardDraftId(storyboardId)
                            .contentProjectId(project.getId())
                            .status("APPROVED").build());

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.approve(storyboardId);

            assertThat(response.getBody().getData().getStatus()).isNotEqualTo("READY_FOR_PUBLISH");
        }
    }

    // ── Reject Endpoint Tests ───────────────────────────────

    @Nested
    @DisplayName("Reject endpoint")
    class RejectTests {

        @Test
        @DisplayName("Reject endpoint explicit storyboard reddi yapar")
        void rejectExplicitlyRejectsStoryboard() {
            UUID storyboardId = UUID.randomUUID();
            when(storyboardReviewService.rejectStoryboardDraft(eq(storyboardId), anyString(), anyString()))
                    .thenReturn(StoryboardReviewResult.builder()
                            .success(true).storyboardDraftId(storyboardId)
                            .contentProjectId(project.getId())
                            .status("REJECTED").build());

            StoryboardRejectRequest request = StoryboardRejectRequest.builder()
                    .reason("Sahne görsel açıklamaları yetersiz").build();

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.reject(storyboardId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Reject endpoint red sebebini service'e iletir")
        void rejectPassesReasonToService() {
            UUID storyboardId = UUID.randomUUID();
            when(storyboardReviewService.rejectStoryboardDraft(eq(storyboardId), anyString(), anyString()))
                    .thenReturn(StoryboardReviewResult.builder()
                            .success(true).storyboardDraftId(storyboardId)
                            .contentProjectId(project.getId())
                            .status("REJECTED").build());

            StoryboardRejectRequest request = StoryboardRejectRequest.builder()
                    .reason("Tarihsel doğruluk eksik").build();

            controller.reject(storyboardId, request);

            verify(storyboardReviewService).rejectStoryboardDraft(
                    eq(storyboardId), anyString(), eq("Tarihsel doğruluk eksik"));
        }
    }

    // ── Governance / API Surface Tests ──────────────────────

    @Nested
    @DisplayName("Governance / API surface")
    class GovernanceTests {

        @Test
        @DisplayName("Generic status update endpoint yok")
        void noGenericStatusUpdateEndpoint() {
            Method[] methods = AdminStoryboardController.class.getDeclaredMethods();
            for (Method method : methods) {
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
        @DisplayName("REST DTO StoryboardAgentRequest expose etmez")
        void restDtoDoesNotExposeStoryboardAgentRequest() {
            Set<String> fieldNames = Arrays.stream(StoryboardDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("agentRequest", "storyboardAgentRequest");
        }

        @Test
        @DisplayName("REST DTO StoryboardAgentResponse expose etmez")
        void restDtoDoesNotExposeStoryboardAgentResponse() {
            Set<String> fieldNames = Arrays.stream(StoryboardDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("agentResponse", "storyboardAgentResponse");
        }

        @Test
        @DisplayName("REST DTO raw provider payload/prompt/secret field expose etmez")
        void restDtoDoesNotExposeRawFields() {
            Set<String> allFields = Arrays.stream(StoryboardDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            allFields.addAll(Arrays.stream(StoryboardGenerateResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet()));

            allFields.addAll(Arrays.stream(StoryboardSceneResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet()));

            allFields.addAll(Arrays.stream(StoryboardDraftSummaryResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet()));

            assertThat(allFields).doesNotContain(
                    "prompt", "rawPrompt", "rawPayload", "providerPayload",
                    "requestBody", "responseBody", "apiKey", "secret",
                    "accessToken", "authorization");
        }

        @Test
        @DisplayName("REST DTO StoryboardSourceBeatInput expose etmez")
        void restDtoDoesNotExposeStoryboardSourceBeatInput() {
            Set<String> fieldNames = Arrays.stream(StoryboardDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("sourceBeatInput", "storyboardSourceBeatInput",
                    "beatInput", "beatInputs");
        }

        @Test
        @DisplayName("REST DTO StoryboardSceneOutput expose etmez")
        void restDtoDoesNotExposeStoryboardSceneOutput() {
            Set<String> fieldNames = Arrays.stream(StoryboardDraftResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("sceneOutput", "storyboardSceneOutput",
                    "sceneOutputs");
        }

        @Test
        @DisplayName("READY_FOR_PUBLISH endpoint yok")
        void noReadyForPublishEndpoint() {
            Method[] methods = AdminStoryboardController.class.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(PostMapping.class)) {
                    PostMapping mapping = method.getAnnotation(PostMapping.class);
                    for (String path : mapping.value()) {
                        assertThat(path).doesNotContain("publish");
                        assertThat(path).doesNotContain("ready");
                    }
                }
            }
        }

        @Test
        @DisplayName("Controller @RestController ve doğru base path kullanır")
        void controllerHasCorrectAnnotations() {
            assertThat(AdminStoryboardController.class.isAnnotationPresent(RestController.class)).isTrue();
            RequestMapping mapping = AdminStoryboardController.class.getAnnotation(RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin");
        }

        @Test
        @DisplayName("Edit request DTO'su status/version/provider alanlarını kabul etmez")
        void editRequestDoesNotAcceptStatusOrVersionOrProviderFields() {
            Set<String> fieldNames = Arrays.stream(StoryboardDraftEditRequest.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain(
                    "status", "version", "providerType", "modelName", "generatedBy",
                    "id", "contentProjectId", "sourceDraftId", "sourceScenarioDraftId",
                    "createdAt", "updatedAt", "approvedAt", "rejectedAt",
                    "createdBy", "approvedBy", "rejectedBy");
        }

        @Test
        @DisplayName("Reject request DTO @NotBlank validasyon içerir")
        void rejectRequestHasNotBlankValidation() {
            try {
                Field reasonField = StoryboardRejectRequest.class.getDeclaredField("reason");
                assertThat(reasonField.isAnnotationPresent(
                        jakarta.validation.constraints.NotBlank.class)).isTrue();
            } catch (NoSuchFieldException e) {
                throw new AssertionError("reason field must exist in StoryboardRejectRequest", e);
            }
        }
    }
}
