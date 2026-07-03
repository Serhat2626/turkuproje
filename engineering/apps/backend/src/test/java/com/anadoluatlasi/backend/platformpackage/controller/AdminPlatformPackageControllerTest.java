package com.anadoluatlasi.backend.platformpackage.controller;

import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageDraftEditCommand;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageGenerationResult;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageGenerationService;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageQueryService;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageReviewResult;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageReviewService;
import com.anadoluatlasi.backend.application.platformpackage.PlatformPackageSceneUsageEditCommand;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageDraftEditRequest;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageDraftResponse;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageDraftSummaryResponse;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageGenerateResponse;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageRejectRequest;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageSceneUsageEditRequest;
import com.anadoluatlasi.backend.platformpackage.controller.dto.PlatformPackageSceneUsageResponse;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageSceneUsage;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageStatus;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-004: AdminPlatformPackageController unit testleri.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminPlatformPackageControllerTest {

    @Mock
    private PlatformPackageGenerationService generationService;
    @Mock
    private PlatformPackageQueryService queryService;
    @Mock
    private PlatformPackageReviewService reviewService;

    private AdminPlatformPackageController controller;
    private ContentProject project;
    private StoryboardDraft storyboard;

    @BeforeEach
    void setUp() {
        controller = new AdminPlatformPackageController(generationService, queryService, reviewService);
        project = ContentProject.builder().id(UUID.randomUUID()).build();
        storyboard = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .build();
    }

    private PlatformPackageDraft createDraft(PlatformPackageType type, PlatformPackageStatus status) {
        PlatformPackageDraft draft = PlatformPackageDraft.builder()
                .contentProject(project)
                .sourceStoryboardDraft(storyboard)
                .packageType(type)
                .status(status)
                .version(1)
                .title("Test Paketi")
                .captionOrDescription("Test açıklama")
                .callToAction("Abone ol!")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-v1")
                .build();
        draft.onPrePersist();
        return draft;
    }

    // ── Generate Endpoint ──────────────────────────────────

    @Nested
    @DisplayName("Generate endpoint")
    class GenerateEndpoint {

        @Test
        @DisplayName("POST generate → service çağırır ve safe response döner")
        void shouldCallServiceAndReturnSafeResponse() {
            UUID storyboardId = UUID.randomUUID();
            PlatformPackageGenerationResult result = PlatformPackageGenerationResult.builder()
                    .success(true)
                    .contentProjectId(project.getId())
                    .sourceStoryboardDraftId(storyboardId)
                    .generatedPackageIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                    .packageCount(2)
                    .message("OK")
                    .build();

            when(generationService.generatePlatformPackages(eq(storyboardId), anyString()))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<PlatformPackageGenerateResponse>> response =
                    controller.generate(storyboardId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            PlatformPackageGenerateResponse body = response.getBody().getData();
            assertThat(body.isSuccess()).isTrue();
            assertThat(body.getPackageCount()).isEqualTo(2);
            assertThat(body.getGeneratedPackageIds()).hasSize(2);
        }

        @Test
        @DisplayName("Başarısız generate → HTTP 200 döner")
        void shouldReturnOkOnFailure() {
            PlatformPackageGenerationResult result = PlatformPackageGenerationResult.builder()
                    .success(false)
                    .contentProjectId(project.getId())
                    .sourceStoryboardDraftId(UUID.randomUUID())
                    .errorCode("AI_PROVIDER_TIMEOUT")
                    .message("Timeout")
                    .build();

            when(generationService.generatePlatformPackages(any(), anyString())).thenReturn(result);

            ResponseEntity<ApiResponse<PlatformPackageGenerateResponse>> response =
                    controller.generate(UUID.randomUUID());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().isSuccess()).isFalse();
            assertThat(response.getBody().getData().getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
        }
    }

    // ── List Endpoints ─────────────────────────────────────

    @Nested
    @DisplayName("List endpoints")
    class ListEndpoints {

        @Test
        @DisplayName("List by content project → summary response döner")
        void shouldListByContentProject() {
            PlatformPackageDraft d1 = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            PlatformPackageDraft d2 = createDraft(PlatformPackageType.YOUTUBE_SHORTS,
                    PlatformPackageStatus.NEEDS_REVIEW);

            when(queryService.listByContentProject(project.getId())).thenReturn(List.of(d1, d2));

            ResponseEntity<ApiResponse<List<PlatformPackageDraftSummaryResponse>>> response =
                    controller.listByProject(project.getId());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<PlatformPackageDraftSummaryResponse> summaries = response.getBody().getData();
            assertThat(summaries).hasSize(2);
            assertThat(summaries.get(0).getPackageType()).isEqualTo("YOUTUBE_LONG_FORM");
            assertThat(summaries.get(1).getPackageType()).isEqualTo("YOUTUBE_SHORTS");
        }

        @Test
        @DisplayName("List by source storyboard → summary response döner")
        void shouldListBySourceStoryboard() {
            PlatformPackageDraft d1 = createDraft(PlatformPackageType.INSTAGRAM_REELS,
                    PlatformPackageStatus.AI_GENERATED);

            when(queryService.listBySourceStoryboard(storyboard.getId())).thenReturn(List.of(d1));

            ResponseEntity<ApiResponse<List<PlatformPackageDraftSummaryResponse>>> response =
                    controller.listByStoryboard(storyboard.getId());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
        }
    }

    // ── Detail Endpoint ────────────────────────────────────

    @Nested
    @DisplayName("Detail endpoint")
    class DetailEndpoint {

        @Test
        @DisplayName("Detail → scene usages dahil döner")
        void shouldReturnDetailWithSceneUsages() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            StoryboardScene sourceScene = StoryboardScene.builder()
                    .id(UUID.randomUUID())
                    .sceneNumber(1)
                    .build();
            PlatformPackageSceneUsage usage = PlatformPackageSceneUsage.builder()
                    .platformPackageDraft(draft)
                    .sceneNumber(1)
                    .sourceStoryboardScene(sourceScene)
                    .usageType("PRIMARY")
                    .included(true)
                    .adaptationNote("Tam kullanım")
                    .build();
            usage.onPrePersist();
            draft.getSceneUsages().add(usage);

            when(queryService.getDetail(draft.getId())).thenReturn(draft);

            ResponseEntity<ApiResponse<PlatformPackageDraftResponse>> response =
                    controller.getDetail(draft.getId());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            PlatformPackageDraftResponse body = response.getBody().getData();
            assertThat(body.getPackageType()).isEqualTo("YOUTUBE_LONG_FORM");
            assertThat(body.getSceneUsages()).hasSize(1);
            PlatformPackageSceneUsageResponse su = body.getSceneUsages().get(0);
            assertThat(su.getSceneNumber()).isEqualTo(1);
            assertThat(su.getUsageType()).isEqualTo("PRIMARY");
            assertThat(su.isIncluded()).isTrue();
            assertThat(su.getSourceStoryboardSceneId()).isEqualTo(sourceScene.getId());
        }
    }

    // ── Edit Endpoint ──────────────────────────────────────

    @Nested
    @DisplayName("Edit endpoint")
    class EditEndpoint {

        @Test
        @DisplayName("Edit → command doğru map edilir")
        void shouldMapRequestToEditCommand() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);

            PlatformPackageDraftEditRequest request = PlatformPackageDraftEditRequest.builder()
                    .title("Yeni Başlık")
                    .videoTitle("SEO Video Title")
                    .shortHook("Hook!")
                    .sceneUsages(List.of(
                            PlatformPackageSceneUsageEditRequest.builder()
                                    .sceneNumber(1)
                                    .usageType("ADAPTED")
                                    .included(true)
                                    .adaptationNote("Kısaltıldı")
                                    .build()
                    ))
                    .build();

            when(reviewService.editPlatformPackageDraft(any(), any(), anyString()))
                    .thenReturn(PlatformPackageReviewResult.builder()
                            .success(true)
                            .packageDraftId(draft.getId())
                            .contentProjectId(project.getId())
                            .status("NEEDS_REVIEW")
                            .packageType("YOUTUBE_LONG_FORM")
                            .title("Yeni Başlık")
                            .message("OK")
                            .build());
            when(queryService.getDetail(draft.getId())).thenReturn(draft);

            controller.edit(draft.getId(), request);

            ArgumentCaptor<PlatformPackageDraftEditCommand> captor =
                    ArgumentCaptor.forClass(PlatformPackageDraftEditCommand.class);
            verify(reviewService).editPlatformPackageDraft(eq(draft.getId()), captor.capture(), anyString());

            PlatformPackageDraftEditCommand cmd = captor.getValue();
            assertThat(cmd.getTitle()).isEqualTo("Yeni Başlık");
            assertThat(cmd.getVideoTitle()).isEqualTo("SEO Video Title");
            assertThat(cmd.getShortHook()).isEqualTo("Hook!");
            assertThat(cmd.getSceneUsages()).hasSize(1);
            assertThat(cmd.getSceneUsages().get(0).getSceneNumber()).isEqualTo(1);
            assertThat(cmd.getSceneUsages().get(0).getUsageType()).isEqualTo("ADAPTED");
        }
    }

    // ── Approve Endpoint ───────────────────────────────────

    @Nested
    @DisplayName("Approve endpoint")
    class ApproveEndpoint {

        @Test
        @DisplayName("Approve → explicit approve service çağırır")
        void shouldCallExplicitApprove() {
            UUID packageId = UUID.randomUUID();
            PlatformPackageReviewResult result = PlatformPackageReviewResult.builder()
                    .success(true)
                    .packageDraftId(packageId)
                    .contentProjectId(project.getId())
                    .status("APPROVED")
                    .packageType("YOUTUBE_LONG_FORM")
                    .title("Test")
                    .message("Onaylandı")
                    .build();

            when(reviewService.approvePlatformPackageDraft(eq(packageId), anyString()))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<ActionStatusResponse>> response = controller.approve(packageId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("APPROVED");
            verify(reviewService).approvePlatformPackageDraft(eq(packageId), anyString());
        }
    }

    // ── Reject Endpoint ────────────────────────────────────

    @Nested
    @DisplayName("Reject endpoint")
    class RejectEndpoint {

        @Test
        @DisplayName("Reject → reason ile reject service çağırır")
        void shouldCallRejectWithReason() {
            UUID packageId = UUID.randomUUID();
            PlatformPackageRejectRequest request = PlatformPackageRejectRequest.builder()
                    .reason("İçerik uygun değil")
                    .build();

            PlatformPackageReviewResult result = PlatformPackageReviewResult.builder()
                    .success(true)
                    .packageDraftId(packageId)
                    .contentProjectId(project.getId())
                    .status("REJECTED")
                    .packageType("YOUTUBE_LONG_FORM")
                    .title("Test")
                    .message("Reddedildi")
                    .build();

            when(reviewService.rejectPlatformPackageDraft(eq(packageId), anyString(), eq("İçerik uygun değil")))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<ActionStatusResponse>> response =
                    controller.reject(packageId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getStatus()).isEqualTo("REJECTED");
            verify(reviewService).rejectPlatformPackageDraft(eq(packageId), anyString(), eq("İçerik uygun değil"));
        }
    }

    // ── API Safety / Governance ─────────────────────────────

    @Nested
    @DisplayName("API safety ve governance")
    class ApiSafety {

        @Test
        @DisplayName("Controller @RestController ve /api/admin mapping'e sahip")
        void shouldHaveCorrectAnnotations() {
            assertThat(AdminPlatformPackageController.class.isAnnotationPresent(RestController.class)).isTrue();
            RequestMapping rm = AdminPlatformPackageController.class.getAnnotation(RequestMapping.class);
            assertThat(rm.value()).contains("/api/admin");
        }

        @Test
        @DisplayName("Generic status update endpoint yok")
        void shouldNotHaveGenericStatusEndpoint() {
            Method[] methods = AdminPlatformPackageController.class.getDeclaredMethods();
            for (Method method : methods) {
                String name = method.getName().toLowerCase();
                assertThat(name).doesNotContain("updatestatus");
                assertThat(name).doesNotContain("setstatus");
                assertThat(name).doesNotContain("changestatus");
            }
        }

        @Test
        @DisplayName("READY_FOR_PUBLISH endpoint yok")
        void shouldNotHaveReadyForPublishEndpoint() {
            Method[] methods = AdminPlatformPackageController.class.getDeclaredMethods();
            Set<String> allPaths = Arrays.stream(methods)
                    .flatMap(m -> {
                        List<String> paths = new java.util.ArrayList<>();
                        if (m.isAnnotationPresent(PostMapping.class)) {
                            paths.addAll(Arrays.asList(m.getAnnotation(PostMapping.class).value()));
                        }
                        if (m.isAnnotationPresent(PutMapping.class)) {
                            paths.addAll(Arrays.asList(m.getAnnotation(PutMapping.class).value()));
                        }
                        if (m.isAnnotationPresent(GetMapping.class)) {
                            paths.addAll(Arrays.asList(m.getAnnotation(GetMapping.class).value()));
                        }
                        return paths.stream();
                    })
                    .collect(Collectors.toSet());

            for (String path : allPaths) {
                assertThat(path.toLowerCase()).doesNotContain("ready-for-publish");
                assertThat(path.toLowerCase()).doesNotContain("readyforpublish");
            }
        }

        @Test
        @DisplayName("Edit request DTO'da status/version/provider/source alanı yok")
        void shouldNotHaveUnsafeFieldsInEditRequest() {
            Field[] fields = PlatformPackageDraftEditRequest.class.getDeclaredFields();
            Set<String> fieldNames = Arrays.stream(fields)
                    .map(f -> f.getName().toLowerCase())
                    .collect(Collectors.toSet());

            assertThat(fieldNames).doesNotContain("status");
            assertThat(fieldNames).doesNotContain("version");
            assertThat(fieldNames).doesNotContain("providertype");
            assertThat(fieldNames).doesNotContain("modelname");
            assertThat(fieldNames).doesNotContain("generatedby");
            assertThat(fieldNames).doesNotContain("contenprojectid");
            assertThat(fieldNames).doesNotContain("sourcestoryboarddraftid");
            assertThat(fieldNames).doesNotContain("rawprompt");
            assertThat(fieldNames).doesNotContain("apikey");
            assertThat(fieldNames).doesNotContain("secret");
        }

        @Test
        @DisplayName("Controller'da AI contract import yok")
        void shouldNotImportAiContracts() {
            // Controller'ın import ettiği type'ları reflection ile kontrol et
            Field[] fields = AdminPlatformPackageController.class.getDeclaredFields();
            for (Field field : fields) {
                String typeName = field.getType().getName();
                assertThat(typeName).doesNotContain("PlatformPackageAgentRequest");
                assertThat(typeName).doesNotContain("PlatformPackageAgentResponse");
                assertThat(typeName).doesNotContain("PlatformPackageOutput");
                assertThat(typeName).doesNotContain("AiPlatformPackageAgentGateway");
            }
        }

        @Test
        @DisplayName("Approve ve reject explicit endpoint'ler")
        void shouldHaveExplicitApproveRejectEndpoints() {
            boolean hasApprove = false;
            boolean hasReject = false;
            for (Method method : AdminPlatformPackageController.class.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostMapping.class)) {
                    String[] paths = method.getAnnotation(PostMapping.class).value();
                    for (String path : paths) {
                        if (path.contains("/approve")) hasApprove = true;
                        if (path.contains("/reject")) hasReject = true;
                    }
                }
            }
            assertThat(hasApprove).isTrue();
            assertThat(hasReject).isTrue();
        }

        @Test
        @DisplayName("7 endpoint mevcut (generate, 2 list, detail, edit, approve, reject)")
        void shouldHaveExactly7Endpoints() {
            long endpointCount = Arrays.stream(AdminPlatformPackageController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)
                            || m.isAnnotationPresent(GetMapping.class)
                            || m.isAnnotationPresent(PutMapping.class))
                    .count();
            assertThat(endpointCount).isEqualTo(7);
        }

        @Test
        @DisplayName("Response DTO'da rawPrompt/apiKey/secret alanı yok")
        void shouldNotHaveUnsafeFieldsInResponseDtos() {
            for (Class<?> clazz : List.of(
                    PlatformPackageDraftResponse.class,
                    PlatformPackageDraftSummaryResponse.class,
                    PlatformPackageGenerateResponse.class,
                    PlatformPackageSceneUsageResponse.class)) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    String name = field.getName().toLowerCase();
                    assertThat(name).as("DTO %s field %s", clazz.getSimpleName(), field.getName())
                            .doesNotContain("rawprompt")
                            .doesNotContain("apikey")
                            .doesNotContain("secret")
                            .doesNotContain("publishedat")
                            .doesNotContain("imageurl")
                            .doesNotContain("videourl");
                }
            }
        }
    }
}
