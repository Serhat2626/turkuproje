package com.anadoluatlasi.backend.visualasset.controller;

import com.anadoluatlasi.backend.application.visualasset.*;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import com.anadoluatlasi.backend.visualasset.controller.dto.*;
import com.anadoluatlasi.backend.visualasset.entity.SceneVisualPlan;
import com.anadoluatlasi.backend.visualasset.entity.ThumbnailPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlanStatus;
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
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-006: AdminVisualAssetPlanController unit testleri.
 *
 * <p>Kontrol noktaları:</p>
 * <ul>
 *   <li>Controller thin kalır — iş kuralı yok</li>
 *   <li>Internal AI contract DTO'ları expose edilmez</li>
 *   <li>Generic status update endpoint yok</li>
 *   <li>Approve/reject explicit action endpoint</li>
 *   <li>READY_FOR_PUBLISH transition yok</li>
 *   <li>Raw prompt/provider payload/API key/secret expose edilmez</li>
 *   <li>Image generation/media/upload/publish endpoint yok</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminVisualAssetPlanControllerTest {

    @Mock
    private VisualAssetPlanGenerationService generationService;
    @Mock
    private VisualAssetPlanReviewService reviewService;
    @Mock
    private VisualAssetPlanQueryService queryService;

    private AdminVisualAssetPlanController controller;
    private ContentProject project;
    private StoryboardDraft storyboard;

    @BeforeEach
    void setUp() {
        controller = new AdminVisualAssetPlanController(
                generationService, reviewService, queryService);

        project = ContentProject.builder().id(UUID.randomUUID()).build();
        storyboard = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .version(1)
                .status(StoryboardDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .scenes(new ArrayList<>())
                .build();
        storyboard.onPrePersist();
    }

    private VisualAssetPlan buildPlan(VisualAssetPlanStatus status) {
        StoryboardScene sourceScene = StoryboardScene.builder()
                .storyboardDraft(storyboard)
                .sceneNumber(1)
                .sceneTitle("Scene 1")
                .visualDescription("Visual desc")
                .build();
        sourceScene.onPrePersist();

        SceneVisualPlan scene1 = SceneVisualPlan.builder()
                .sceneNumber(1)
                .sceneTitle("Sahne 1")
                .englishImagePrompt("A cinematic shot of Anatolian village")
                .turkishVisualContext("Sinematik Anadolu köyü çekimi")
                .negativePrompt("no text, no watermark")
                .styleDirective("Cinematic")
                .aspectRatio("16:9")
                .mood("dramatic")
                .location("Anadolu")
                .sourceStoryboardScene(sourceScene)
                .build();
        scene1.onPrePersist();

        SceneVisualPlan scene2 = SceneVisualPlan.builder()
                .sceneNumber(2)
                .sceneTitle("Sahne 2")
                .englishImagePrompt("A dramatic view of mountains")
                .turkishVisualContext("Dramatik dağ görüntüsü")
                .styleDirective("Documentary")
                .aspectRatio("16:9")
                .build();
        scene2.onPrePersist();

        ThumbnailPlan thumb = ThumbnailPlan.builder()
                .thumbnailPrompt("Portrait of Anatolian storyteller")
                .negativePrompt("no text")
                .aspectRatio("16:9")
                .sourceSceneNumber(1)
                .sourceStoryboardScene(sourceScene)
                .build();
        thumb.onPrePersist();

        VisualAssetPlan plan = VisualAssetPlan.builder()
                .contentProject(project)
                .sourceStoryboardDraft(storyboard)
                .version(1)
                .status(status)
                .title("Test Görsel Planı")
                .summary("Test özet")
                .overallVisualStyle("Sinematik")
                .aspectRatioStrategy("16:9 primary")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-visual-asset-planner")
                .scenePlans(new ArrayList<>(List.of(scene1, scene2)))
                .thumbnailPlan(thumb)
                .build();
        plan.onPrePersist();
        return plan;
    }

    // ── Generate Endpoint Tests ─────────────────────────────────────

    @Nested
    @DisplayName("Generate endpoint")
    class GenerateTests {

        @Test
        @DisplayName("Generate endpoint başarılı yanıt döndürür CREATED ile")
        void generateReturnsCreatedOnSuccess() {
            UUID storyboardId = storyboard.getId();
            VisualAssetPlanGenerationResult result = VisualAssetPlanGenerationResult.builder()
                    .success(true)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboardId)
                    .visualAssetPlanId(UUID.randomUUID())
                    .version(1)
                    .status("AI_GENERATED")
                    .planTitle("Test Plan")
                    .scenePlanCount(8)
                    .hasThumbnailPlan(true)
                    .message("Başarılı")
                    .build();

            when(generationService.generateVisualAssetPlan(eq(storyboardId), anyString()))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<VisualAssetPlanGenerateResponse>> response =
                    controller.generate(storyboardId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getStatus()).isEqualTo("AI_GENERATED");
        }

        @Test
        @DisplayName("Generate response AI_GENERATED status döndürür")
        void generateReturnsAiGeneratedStatus() {
            UUID storyboardId = storyboard.getId();
            VisualAssetPlanGenerationResult result = VisualAssetPlanGenerationResult.builder()
                    .success(true)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboardId)
                    .visualAssetPlanId(UUID.randomUUID())
                    .version(1)
                    .status("AI_GENERATED")
                    .planTitle("Plan")
                    .scenePlanCount(3)
                    .hasThumbnailPlan(true)
                    .message("OK")
                    .build();

            when(generationService.generateVisualAssetPlan(eq(storyboardId), anyString()))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<VisualAssetPlanGenerateResponse>> response =
                    controller.generate(storyboardId);

            assertThat(response.getBody().getData().getStatus()).isEqualTo("AI_GENERATED");
        }

        @Test
        @DisplayName("Generate response scene plan count döndürür")
        void generateReturnsScenePlanCount() {
            UUID storyboardId = storyboard.getId();
            VisualAssetPlanGenerationResult result = VisualAssetPlanGenerationResult.builder()
                    .success(true)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboardId)
                    .visualAssetPlanId(UUID.randomUUID())
                    .version(1)
                    .status("AI_GENERATED")
                    .planTitle("Plan")
                    .scenePlanCount(5)
                    .hasThumbnailPlan(true)
                    .message("OK")
                    .build();

            when(generationService.generateVisualAssetPlan(eq(storyboardId), anyString()))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<VisualAssetPlanGenerateResponse>> response =
                    controller.generate(storyboardId);

            assertThat(response.getBody().getData().getScenePlanCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Generate response thumbnail plan bilgisi döndürür")
        void generateReturnsThumbnailInfo() {
            UUID storyboardId = storyboard.getId();
            VisualAssetPlanGenerationResult result = VisualAssetPlanGenerationResult.builder()
                    .success(true)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboardId)
                    .visualAssetPlanId(UUID.randomUUID())
                    .version(1)
                    .status("AI_GENERATED")
                    .planTitle("Plan")
                    .scenePlanCount(3)
                    .hasThumbnailPlan(true)
                    .message("OK")
                    .build();

            when(generationService.generateVisualAssetPlan(eq(storyboardId), anyString()))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<VisualAssetPlanGenerateResponse>> response =
                    controller.generate(storyboardId);

            assertThat(response.getBody().getData().getHasThumbnailPlan()).isTrue();
        }

        @Test
        @DisplayName("Generate endpoint başarısız durumda OK döndürür")
        void generateReturnsOkOnFailure() {
            UUID storyboardId = storyboard.getId();
            VisualAssetPlanGenerationResult result = VisualAssetPlanGenerationResult.builder()
                    .success(false)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboardId)
                    .status("FAILED")
                    .message("AI hatası")
                    .errorCode("PROVIDER_ERROR")
                    .build();

            when(generationService.generateVisualAssetPlan(eq(storyboardId), anyString()))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<VisualAssetPlanGenerateResponse>> response =
                    controller.generate(storyboardId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Generate endpoint storyboardDraftId'yi service'e iletir")
        void generatePassesStoryboardIdToService() {
            UUID storyboardId = storyboard.getId();
            VisualAssetPlanGenerationResult result = VisualAssetPlanGenerationResult.builder()
                    .success(true)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboardId)
                    .visualAssetPlanId(UUID.randomUUID())
                    .version(1)
                    .status("AI_GENERATED")
                    .planTitle("P")
                    .scenePlanCount(1)
                    .hasThumbnailPlan(true)
                    .message("OK")
                    .build();

            when(generationService.generateVisualAssetPlan(eq(storyboardId), anyString()))
                    .thenReturn(result);

            controller.generate(storyboardId);

            verify(generationService).generateVisualAssetPlan(eq(storyboardId), anyString());
        }

        @Test
        @DisplayName("Generate response image/media field içermez")
        void generateResponseHasNoImageFields() {
            Set<String> fields = Arrays.stream(VisualAssetPlanGenerateResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fields).doesNotContain("imageUrl", "mediaUrl", "filePath",
                    "objectKey", "bucket", "mediaAssetId", "generatedAssetId");
        }
    }

    // ── List/Detail Endpoint Tests ──────────────────────────────────

    @Nested
    @DisplayName("List/Detail endpoints")
    class ListDetailTests {

        @Test
        @DisplayName("List by storyboard planları döndürür")
        void listByStoryboardReturnsPlans() {
            UUID storyboardId = storyboard.getId();
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);

            when(queryService.listByStoryboardDraft(storyboardId))
                    .thenReturn(List.of(plan));

            ResponseEntity<ApiResponse<List<VisualAssetPlanResponse>>> response =
                    controller.listByStoryboard(storyboardId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
        }

        @Test
        @DisplayName("List by content project planları döndürür")
        void listByContentProjectReturnsPlans() {
            UUID projectId = project.getId();
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);

            when(queryService.listByContentProject(projectId))
                    .thenReturn(List.of(plan));

            ResponseEntity<ApiResponse<List<VisualAssetPlanResponse>>> response =
                    controller.listByContentProject(projectId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
        }

        @Test
        @DisplayName("Detail endpoint parent alanları döndürür")
        void detailReturnsParentFields() {
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);

            when(queryService.getDetail(plan.getId())).thenReturn(plan);

            ResponseEntity<ApiResponse<VisualAssetPlanResponse>> response =
                    controller.getDetail(plan.getId());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            VisualAssetPlanResponse data = response.getBody().getData();
            assertThat(data.getTitle()).isEqualTo("Test Görsel Planı");
            assertThat(data.getStatus()).isEqualTo("AI_GENERATED");
            assertThat(data.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Detail endpoint scene planları sıralı döndürür")
        void detailReturnsOrderedScenePlans() {
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);

            when(queryService.getDetail(plan.getId())).thenReturn(plan);

            ResponseEntity<ApiResponse<VisualAssetPlanResponse>> response =
                    controller.getDetail(plan.getId());

            List<SceneVisualPlanResponse> scenes = response.getBody().getData().getScenePlans();
            assertThat(scenes).hasSize(2);
            assertThat(scenes.get(0).getSceneNumber()).isEqualTo(1);
            assertThat(scenes.get(1).getSceneNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("Detail endpoint thumbnail döndürür")
        void detailReturnsThumbnail() {
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);

            when(queryService.getDetail(plan.getId())).thenReturn(plan);

            ResponseEntity<ApiResponse<VisualAssetPlanResponse>> response =
                    controller.getDetail(plan.getId());

            ThumbnailPlanResponse thumb = response.getBody().getData().getThumbnailPlan();
            assertThat(thumb).isNotNull();
            assertThat(thumb.getThumbnailPrompt()).isEqualTo("Portrait of Anatolian storyteller");
        }
    }

    // ── Edit Endpoint Tests ─────────────────────────────────────────

    @Nested
    @DisplayName("Edit endpoints")
    class EditTests {

        @Test
        @DisplayName("PATCH parent alanları günceller ve NEEDS_REVIEW yanıtı döndürür")
        void editPlanReturnsNeedsReview() {
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);
            plan.setStatus(VisualAssetPlanStatus.NEEDS_REVIEW);

            VisualAssetPlanReviewResult reviewResult = VisualAssetPlanReviewResult.builder()
                    .success(true)
                    .visualAssetPlanId(plan.getId())
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboard.getId())
                    .status("NEEDS_REVIEW")
                    .planTitle("Updated")
                    .message("OK")
                    .build();

            when(reviewService.updatePlan(eq(plan.getId()), any(), anyString()))
                    .thenReturn(reviewResult);
            when(queryService.getDetail(plan.getId())).thenReturn(plan);

            VisualAssetPlanEditRequest request = VisualAssetPlanEditRequest.builder()
                    .title("Updated Title")
                    .overallVisualStyle("New Style")
                    .build();

            ResponseEntity<ApiResponse<VisualAssetPlanResponse>> response =
                    controller.editPlan(plan.getId(), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("PATCH scene englishImagePrompt günceller")
        void editSceneUpdatesPrompt() {
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);
            plan.setStatus(VisualAssetPlanStatus.NEEDS_REVIEW);
            plan.getScenePlans().get(0).setEnglishImagePrompt("Updated prompt");

            VisualAssetPlanReviewResult reviewResult = VisualAssetPlanReviewResult.builder()
                    .success(true)
                    .visualAssetPlanId(plan.getId())
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboard.getId())
                    .status("NEEDS_REVIEW")
                    .planTitle("P")
                    .message("OK")
                    .build();

            when(reviewService.updateScenePlan(eq(plan.getId()), eq(1), any(), anyString()))
                    .thenReturn(reviewResult);
            when(queryService.getDetail(plan.getId())).thenReturn(plan);

            SceneVisualPlanEditRequest request = SceneVisualPlanEditRequest.builder()
                    .englishImagePrompt("Updated prompt")
                    .build();

            ResponseEntity<ApiResponse<VisualAssetPlanResponse>> response =
                    controller.editScene(plan.getId(), 1, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(reviewService).updateScenePlan(eq(plan.getId()), eq(1), any(), anyString());
        }

        @Test
        @DisplayName("PATCH scene turkishVisualContext günceller")
        void editSceneUpdatesTurkishContext() {
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);

            VisualAssetPlanReviewResult reviewResult = VisualAssetPlanReviewResult.builder()
                    .success(true)
                    .visualAssetPlanId(plan.getId())
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboard.getId())
                    .status("NEEDS_REVIEW")
                    .planTitle("P")
                    .message("OK")
                    .build();

            when(reviewService.updateScenePlan(eq(plan.getId()), eq(1), any(), anyString()))
                    .thenReturn(reviewResult);
            when(queryService.getDetail(plan.getId())).thenReturn(plan);

            SceneVisualPlanEditRequest request = SceneVisualPlanEditRequest.builder()
                    .turkishVisualContext("Yeni bağlam")
                    .build();

            controller.editScene(plan.getId(), 1, request);

            ArgumentCaptor<UpdateSceneVisualPlanCommand> captor =
                    ArgumentCaptor.forClass(UpdateSceneVisualPlanCommand.class);
            verify(reviewService).updateScenePlan(eq(plan.getId()), eq(1),
                    captor.capture(), anyString());
            assertThat(captor.getValue().getTurkishVisualContext()).isEqualTo("Yeni bağlam");
        }

        @Test
        @DisplayName("PATCH thumbnail thumbnailPrompt günceller")
        void editThumbnailUpdatesPrompt() {
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);

            VisualAssetPlanReviewResult reviewResult = VisualAssetPlanReviewResult.builder()
                    .success(true)
                    .visualAssetPlanId(plan.getId())
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboard.getId())
                    .status("NEEDS_REVIEW")
                    .planTitle("P")
                    .message("OK")
                    .build();

            when(reviewService.updateThumbnailPlan(eq(plan.getId()), any(), anyString()))
                    .thenReturn(reviewResult);
            when(queryService.getDetail(plan.getId())).thenReturn(plan);

            ThumbnailPlanEditRequest request = ThumbnailPlanEditRequest.builder()
                    .thumbnailPrompt("New thumbnail prompt")
                    .build();

            ResponseEntity<ApiResponse<VisualAssetPlanResponse>> response =
                    controller.editThumbnail(plan.getId(), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(reviewService).updateThumbnailPlan(eq(plan.getId()), any(), anyString());
        }

        @Test
        @DisplayName("Edit endpoint edit command mapper doğru çalışır")
        void editCommandMapsCorrectly() {
            VisualAssetPlan plan = buildPlan(VisualAssetPlanStatus.AI_GENERATED);

            VisualAssetPlanReviewResult reviewResult = VisualAssetPlanReviewResult.builder()
                    .success(true)
                    .visualAssetPlanId(plan.getId())
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboard.getId())
                    .status("NEEDS_REVIEW")
                    .planTitle("P")
                    .message("OK")
                    .build();

            when(reviewService.updatePlan(eq(plan.getId()), any(), anyString()))
                    .thenReturn(reviewResult);
            when(queryService.getDetail(plan.getId())).thenReturn(plan);

            VisualAssetPlanEditRequest request = VisualAssetPlanEditRequest.builder()
                    .title("New Title")
                    .colorPaletteGuidance("Warm tones")
                    .reviewNotes("Check colors")
                    .build();

            controller.editPlan(plan.getId(), request);

            ArgumentCaptor<UpdateVisualAssetPlanCommand> captor =
                    ArgumentCaptor.forClass(UpdateVisualAssetPlanCommand.class);
            verify(reviewService).updatePlan(eq(plan.getId()), captor.capture(), anyString());
            assertThat(captor.getValue().getTitle()).isEqualTo("New Title");
            assertThat(captor.getValue().getColorPaletteGuidance()).isEqualTo("Warm tones");
            assertThat(captor.getValue().getReviewNotes()).isEqualTo("Check colors");
        }
    }

    // ── Approve Endpoint Tests ──────────────────────────────────────

    @Nested
    @DisplayName("Approve endpoint")
    class ApproveTests {

        @Test
        @DisplayName("Approve endpoint APPROVED döndürür")
        void approveReturnsApprovedStatus() {
            UUID planId = UUID.randomUUID();
            VisualAssetPlanReviewResult result = VisualAssetPlanReviewResult.builder()
                    .success(true)
                    .visualAssetPlanId(planId)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboard.getId())
                    .status("APPROVED")
                    .planTitle("P")
                    .message("OK")
                    .build();

            when(reviewService.approve(eq(planId), anyString())).thenReturn(result);

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.approve(planId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Approve endpoint planId'yi service'e iletir")
        void approvePassesPlanIdToService() {
            UUID planId = UUID.randomUUID();
            when(reviewService.approve(eq(planId), anyString()))
                    .thenReturn(VisualAssetPlanReviewResult.builder()
                            .success(true)
                            .visualAssetPlanId(planId)
                            .contentProjectId(project.getId())
                            .storyboardDraftId(storyboard.getId())
                            .status("APPROVED")
                            .build());

            controller.approve(planId);

            verify(reviewService).approve(eq(planId), anyString());
        }
    }

    // ── Reject Endpoint Tests ───────────────────────────────────────

    @Nested
    @DisplayName("Reject endpoint")
    class RejectTests {

        @Test
        @DisplayName("Reject endpoint REJECTED döndürür")
        void rejectReturnsRejectedStatus() {
            UUID planId = UUID.randomUUID();
            VisualAssetPlanReviewResult result = VisualAssetPlanReviewResult.builder()
                    .success(true)
                    .visualAssetPlanId(planId)
                    .contentProjectId(project.getId())
                    .storyboardDraftId(storyboard.getId())
                    .status("REJECTED")
                    .planTitle("P")
                    .message("Reddedildi")
                    .build();

            when(reviewService.reject(eq(planId), anyString(), anyString())).thenReturn(result);

            VisualAssetPlanRejectRequest request = VisualAssetPlanRejectRequest.builder()
                    .reason("Kalite yetersiz").build();

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.reject(planId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Reject endpoint red sebebini service'e iletir")
        void rejectPassesReasonToService() {
            UUID planId = UUID.randomUUID();
            when(reviewService.reject(eq(planId), anyString(), anyString()))
                    .thenReturn(VisualAssetPlanReviewResult.builder()
                            .success(true).visualAssetPlanId(planId)
                            .contentProjectId(project.getId())
                            .storyboardDraftId(storyboard.getId())
                            .status("REJECTED").build());

            VisualAssetPlanRejectRequest request = VisualAssetPlanRejectRequest.builder()
                    .reason("Tarihsel doğruluk eksik").build();

            controller.reject(planId, request);

            verify(reviewService).reject(eq(planId), anyString(),
                    eq("Tarihsel doğruluk eksik"));
        }
    }

    // ── Governance / API Surface Tests ──────────────────────────────

    @Nested
    @DisplayName("Governance / API surface")
    class GovernanceTests {

        @Test
        @DisplayName("Generic status update endpoint yok")
        void noGenericStatusUpdateEndpoint() {
            Method[] methods = AdminVisualAssetPlanController.class.getDeclaredMethods();
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
                if (method.isAnnotationPresent(PatchMapping.class)) {
                    PatchMapping mapping = method.getAnnotation(PatchMapping.class);
                    for (String path : mapping.value()) {
                        assertThat(path).doesNotEndWith("/status");
                    }
                }
            }
        }

        @Test
        @DisplayName("READY_FOR_PUBLISH endpoint yok")
        void noReadyForPublishEndpoint() {
            Method[] methods = AdminVisualAssetPlanController.class.getDeclaredMethods();
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
        @DisplayName("Publish/upload/schedule endpoint yok")
        void noPublishUploadScheduleEndpoint() {
            Method[] methods = AdminVisualAssetPlanController.class.getDeclaredMethods();
            for (Method method : methods) {
                for (java.lang.annotation.Annotation ann : method.getAnnotations()) {
                    String annString = ann.toString().toLowerCase();
                    assertThat(annString).doesNotContain("publish", "upload", "schedule");
                }
            }
        }

        @Test
        @DisplayName("Image generation endpoint yok")
        void noImageGenerationEndpoint() {
            Method[] methods = AdminVisualAssetPlanController.class.getDeclaredMethods();
            for (Method method : methods) {
                String name = method.getName().toLowerCase();
                assertThat(name).doesNotContain("generateimage", "createimage",
                        "renderimage", "imagegen");
            }
        }

        @Test
        @DisplayName("REST DTO'lar forbidden field içermez")
        void restDtosDoNotContainForbiddenFields() {
            Set<String> allFields = new HashSet<>();
            allFields.addAll(Arrays.stream(VisualAssetPlanResponse.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet()));
            allFields.addAll(Arrays.stream(SceneVisualPlanResponse.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet()));
            allFields.addAll(Arrays.stream(ThumbnailPlanResponse.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet()));
            allFields.addAll(Arrays.stream(VisualAssetPlanGenerateResponse.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet()));

            assertThat(allFields).doesNotContain(
                    "rawPrompt", "internalPrompt", "orchestrationPrompt",
                    "providerRequest", "providerResponse", "providerPayload",
                    "apiKey", "secret", "accessToken", "authorization",
                    "imageUrl", "filePath", "bucket", "objectKey",
                    "mediaAssetId", "generatedAssetId",
                    "publishUrl", "uploadUrl");
        }

        @Test
        @DisplayName("REST DTO READY_FOR_PUBLISH status içermez")
        void noDtoContainsReadyForPublish() {
            // Verify the enum doesn't have READY_FOR_PUBLISH
            Set<String> enumValues = Arrays.stream(VisualAssetPlanStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(enumValues).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("Controller @RestController ve doğru base path kullanır")
        void controllerHasCorrectAnnotations() {
            assertThat(AdminVisualAssetPlanController.class.isAnnotationPresent(
                    RestController.class)).isTrue();
            RequestMapping mapping = AdminVisualAssetPlanController.class
                    .getAnnotation(RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin");
        }

        @Test
        @DisplayName("Edit request DTO status/version/provider alanlarını kabul etmez")
        void editRequestDoesNotAcceptForbiddenFields() {
            Set<String> fieldNames = Arrays.stream(VisualAssetPlanEditRequest.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain(
                    "status", "version", "providerType", "modelName", "generatedBy",
                    "id", "contentProjectId", "sourceStoryboardDraftId",
                    "createdAt", "updatedAt", "approvedAt", "rejectedAt",
                    "createdBy", "approvedBy", "rejectedBy");
        }

        @Test
        @DisplayName("Reject request DTO @NotBlank validasyon içerir")
        void rejectRequestHasNotBlankValidation() {
            try {
                Field reasonField = VisualAssetPlanRejectRequest.class.getDeclaredField("reason");
                assertThat(reasonField.isAnnotationPresent(
                        jakarta.validation.constraints.NotBlank.class)).isTrue();
            } catch (NoSuchFieldException e) {
                throw new AssertionError("reason field must exist in VisualAssetPlanRejectRequest", e);
            }
        }

        @Test
        @DisplayName("Controller/service path MediaAsset/GeneratedAsset referansı içermez")
        void controllerDoesNotReferenceMediaAsset() {
            Field[] fields = AdminVisualAssetPlanController.class.getDeclaredFields();
            for (Field f : fields) {
                String typeName = f.getType().getSimpleName().toLowerCase();
                assertThat(typeName).doesNotContain("mediaasset", "generatedasset");
            }
        }

        @Test
        @DisplayName("OpenAI provider sınıfı oluşturulmadı")
        void noOpenAiProviderCreated() {
            try {
                Class.forName("com.anadoluatlasi.backend.ai.gateway.openai.OpenAiVisualAssetAgentGateway");
                assertThat(false).as("OpenAI provider should not exist").isTrue();
            } catch (ClassNotFoundException e) {
                // Expected
            }
        }

        @Test
        @DisplayName("Scene edit request DTO'su rawPrompt/secret alanı içermez")
        void sceneEditRequestHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(SceneVisualPlanEditRequest.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());

            assertThat(fields).doesNotContain(
                    "rawPrompt", "providerPayload", "providerRequest", "providerResponse",
                    "apiKey", "secret", "imageUrl", "objectKey", "bucket",
                    "mediaAssetId", "generatedAssetId");
        }

        @Test
        @DisplayName("Thumbnail edit request DTO'su rawPrompt/secret alanı içermez")
        void thumbnailEditRequestHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ThumbnailPlanEditRequest.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());

            assertThat(fields).doesNotContain(
                    "rawPrompt", "providerPayload", "providerRequest", "providerResponse",
                    "apiKey", "secret", "imageUrl", "objectKey", "bucket",
                    "mediaAssetId", "generatedAssetId");
        }
    }
}
