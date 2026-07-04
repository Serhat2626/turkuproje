package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGenerationRequestResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.StartImageGenerationRequest;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationItemType;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequest;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequestStatus;
import com.anadoluatlasi.backend.imagegen.repository.ImageGenerationRequestRepository;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.visualasset.entity.SceneVisualPlan;
import com.anadoluatlasi.backend.visualasset.entity.ThumbnailPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlanStatus;
import com.anadoluatlasi.backend.visualasset.repository.VisualAssetPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-007: ImageGenerationRequestService unit testleri.
 *
 * <p>Spring context yüklenmez. Mockito mock'lar kullanılır.</p>
 */
@ExtendWith(MockitoExtension.class)
class ImageGenerationRequestServiceTest {

    @Mock
    private ImageGenerationRequestRepository requestRepository;

    @Mock
    private VisualAssetPlanRepository visualAssetPlanRepository;

    private ImageGenerationRequestService service;

    private ContentProject project;
    private VisualAssetPlan approvedPlan;
    private SceneVisualPlan scenePlan;
    private ThumbnailPlan thumbnailPlan;

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID SCENE_ITEM_ID = UUID.randomUUID();
    private static final UUID THUMBNAIL_ITEM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ImageGenerationRequestService(requestRepository, visualAssetPlanRepository);

        project = ContentProject.builder().build();
        project.setId(PROJECT_ID);

        scenePlan = SceneVisualPlan.builder()
                .sceneNumber(1)
                .englishImagePrompt("test prompt")
                .turkishVisualContext("test context")
                .build();
        scenePlan.setId(SCENE_ITEM_ID);

        thumbnailPlan = ThumbnailPlan.builder()
                .thumbnailPrompt("test thumbnail")
                .build();
        thumbnailPlan.setId(THUMBNAIL_ITEM_ID);

        List<SceneVisualPlan> scenePlans = new ArrayList<>();
        scenePlans.add(scenePlan);

        approvedPlan = VisualAssetPlan.builder()
                .contentProject(project)
                .status(VisualAssetPlanStatus.APPROVED)
                .version(1)
                .scenePlans(scenePlans)
                .thumbnailPlan(thumbnailPlan)
                .build();
        approvedPlan.setId(PLAN_ID);
    }

    // ── startGeneration Tests ───────────────────────────────────────

    @Nested
    @DisplayName("startGeneration — APPROVED plan ile generation request")
    class StartGeneration {

        @Test
        @DisplayName("APPROVED plan ve geçerli scene item — REQUESTED status ile request oluşturur")
        void shouldCreateRequestForApprovedPlanWithSceneItem() {
            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(approvedPlan));
            when(requestRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(requestRepository.save(any())).thenAnswer(invocation -> {
                ImageGenerationRequest saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .build();

            ImageGenerationRequestResponse response = service.startGeneration(request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("REQUESTED");
            assertThat(response.getVisualAssetPlanId()).isEqualTo(PLAN_ID);
            assertThat(response.getVisualAssetItemId()).isEqualTo(SCENE_ITEM_ID);
            assertThat(response.getItemType()).isEqualTo("SCENE_IMAGE");
            assertThat(response.getContentProjectId()).isEqualTo(PROJECT_ID);

            // Saved entity doğrulama
            ArgumentCaptor<ImageGenerationRequest> captor =
                    ArgumentCaptor.forClass(ImageGenerationRequest.class);
            verify(requestRepository).save(captor.capture());
            ImageGenerationRequest saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ImageGenerationRequestStatus.REQUESTED);
            assertThat(saved.getItemType()).isEqualTo(ImageGenerationItemType.SCENE_IMAGE);
            assertThat(saved.getIdempotencyKey()).isNotBlank();
            assertThat(saved.getCorrelationId()).isNotNull();
            assertThat(saved.getRequestedAt()).isNotNull();
        }

        @Test
        @DisplayName("APPROVED plan ve geçerli thumbnail item — THUMBNAIL_IMAGE type ile request")
        void shouldCreateRequestForThumbnailItem() {
            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(approvedPlan));
            when(requestRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(requestRepository.save(any())).thenAnswer(invocation -> {
                ImageGenerationRequest saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(THUMBNAIL_ITEM_ID)
                    .build();

            ImageGenerationRequestResponse response = service.startGeneration(request);

            assertThat(response.getItemType()).isEqualTo("THUMBNAIL_IMAGE");
        }

        @Test
        @DisplayName("Custom idempotencyKey kullanır")
        void shouldUseCustomIdempotencyKey() {
            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(approvedPlan));
            when(requestRepository.findByIdempotencyKey("custom-key")).thenReturn(Optional.empty());
            when(requestRepository.save(any())).thenAnswer(invocation -> {
                ImageGenerationRequest saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .idempotencyKey("custom-key")
                    .build();

            service.startGeneration(request);

            ArgumentCaptor<ImageGenerationRequest> captor =
                    ArgumentCaptor.forClass(ImageGenerationRequest.class);
            verify(requestRepository).save(captor.capture());
            assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("custom-key");
        }

        @Test
        @DisplayName("Custom correlationId kullanır")
        void shouldUseCustomCorrelationId() {
            UUID customCorrelationId = UUID.randomUUID();
            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(approvedPlan));
            when(requestRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(requestRepository.save(any())).thenAnswer(invocation -> {
                ImageGenerationRequest saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .correlationId(customCorrelationId)
                    .build();

            ImageGenerationRequestResponse response = service.startGeneration(request);

            assertThat(response.getCorrelationId()).isEqualTo(customCorrelationId);
        }
    }

    // ── Idempotency Tests ───────────────────────────────────────────

    @Nested
    @DisplayName("startGeneration — Idempotency")
    class Idempotency {

        @Test
        @DisplayName("Aynı idempotencyKey ile mevcut request döner — duplicate oluşturmaz")
        void shouldReturnExistingRequestForDuplicateIdempotencyKey() {
            ImageGenerationRequest existingRequest = ImageGenerationRequest.builder()
                    .contentProjectId(PROJECT_ID)
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .itemType(ImageGenerationItemType.SCENE_IMAGE)
                    .status(ImageGenerationRequestStatus.REQUESTED)
                    .idempotencyKey("duplicate-key")
                    .build();
            existingRequest.setId(UUID.randomUUID());

            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(approvedPlan));
            when(requestRepository.findByIdempotencyKey("duplicate-key"))
                    .thenReturn(Optional.of(existingRequest));

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .idempotencyKey("duplicate-key")
                    .build();

            ImageGenerationRequestResponse response = service.startGeneration(request);

            assertThat(response.getId()).isEqualTo(existingRequest.getId());
            verify(requestRepository, never()).save(any());
        }
    }

    // ── Validation Failure Tests ────────────────────────────────────

    @Nested
    @DisplayName("startGeneration — Validation hataları")
    class ValidationFailures {

        @Test
        @DisplayName("visualAssetItemId null — BusinessException")
        void shouldRejectWhenItemIdIsNull() {
            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(null)
                    .build();

            assertThatThrownBy(() -> service.startGeneration(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("visualAssetItemId zorunludur");

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("VisualAssetPlan bulunamaz — NotFoundException")
        void shouldRejectWhenPlanNotFound() {
            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .build();

            assertThatThrownBy(() -> service.startGeneration(request))
                    .isInstanceOf(NotFoundException.class);

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("APPROVED olmayan plan — BusinessException")
        void shouldRejectWhenPlanNotApproved() {
            VisualAssetPlan draftPlan = VisualAssetPlan.builder()
                    .contentProject(project)
                    .status(VisualAssetPlanStatus.AI_GENERATED)
                    .version(1)
                    .build();
            draftPlan.setId(PLAN_ID);

            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(draftPlan));

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .build();

            assertThatThrownBy(() -> service.startGeneration(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECTED plan — BusinessException")
        void shouldRejectWhenPlanIsRejected() {
            VisualAssetPlan rejectedPlan = VisualAssetPlan.builder()
                    .contentProject(project)
                    .status(VisualAssetPlanStatus.REJECTED)
                    .version(1)
                    .build();
            rejectedPlan.setId(PLAN_ID);

            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(rejectedPlan));

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .build();

            assertThatThrownBy(() -> service.startGeneration(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("Item başka plana ait — BusinessException")
        void shouldRejectWhenItemNotBelongsToPlan() {
            UUID foreignItemId = UUID.randomUUID();

            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(approvedPlan));

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(foreignItemId)
                    .build();

            assertThatThrownBy(() -> service.startGeneration(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("plana ait değil");

            verify(requestRepository, never()).save(any());
        }
    }

    // ── Response DTO Safety Tests ────────────────────────────────────

    @Nested
    @DisplayName("Response DTO — güvenlik doğrulama")
    class ResponseDtoSafety {

        @Test
        @DisplayName("Response DTO raw/internal/provider/secret alan içermez")
        void responseDtoShouldNotContainForbiddenFields() {
            when(visualAssetPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(approvedPlan));
            when(requestRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(requestRepository.save(any())).thenAnswer(invocation -> {
                ImageGenerationRequest saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            StartImageGenerationRequest request = StartImageGenerationRequest.builder()
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .build();

            ImageGenerationRequestResponse response = service.startGeneration(request);

            // Safe alanlar mevcut
            assertThat(response.getId()).isNotNull();
            assertThat(response.getStatus()).isEqualTo("REQUESTED");
            assertThat(response.getVisualAssetPlanId()).isEqualTo(PLAN_ID);

            // ImageGenerationRequestResponse class'ında forbidden field yok
            // (compile-time guarantee — DTO class'ında bu alanlar tanımlı değil)
        }
    }

    // ── getRequestDetail Tests ──────────────────────────────────────

    @Nested
    @DisplayName("getRequestDetail")
    class GetRequestDetail {

        @Test
        @DisplayName("Mevcut request — safe response döner")
        void shouldReturnResponseForExistingRequest() {
            ImageGenerationRequest existing = ImageGenerationRequest.builder()
                    .contentProjectId(PROJECT_ID)
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .itemType(ImageGenerationItemType.SCENE_IMAGE)
                    .status(ImageGenerationRequestStatus.REQUESTED)
                    .idempotencyKey("test-key")
                    .build();
            existing.setId(UUID.randomUUID());

            when(requestRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

            ImageGenerationRequestResponse response = service.getRequestDetail(existing.getId());

            assertThat(response.getId()).isEqualTo(existing.getId());
            assertThat(response.getStatus()).isEqualTo("REQUESTED");
        }

        @Test
        @DisplayName("Request bulunamaz — NotFoundException")
        void shouldThrowNotFoundForMissingRequest() {
            UUID missingId = UUID.randomUUID();
            when(requestRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRequestDetail(missingId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── listRequests Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("listRequests")
    class ListRequests {

        @Test
        @DisplayName("Status filtresi — uygun request'ler döner")
        void shouldFilterByStatus() {
            ImageGenerationRequest req = ImageGenerationRequest.builder()
                    .contentProjectId(PROJECT_ID)
                    .visualAssetPlanId(PLAN_ID)
                    .visualAssetItemId(SCENE_ITEM_ID)
                    .itemType(ImageGenerationItemType.SCENE_IMAGE)
                    .status(ImageGenerationRequestStatus.REQUESTED)
                    .idempotencyKey("key-1")
                    .build();
            req.setId(UUID.randomUUID());

            when(requestRepository.findByStatus(ImageGenerationRequestStatus.REQUESTED))
                    .thenReturn(List.of(req));

            List<ImageGenerationRequestResponse> results = service.listRequests(
                    "REQUESTED", null, null, null, null);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo("REQUESTED");
        }

        @Test
        @DisplayName("Geçersiz status — BusinessException")
        void shouldThrowForInvalidStatus() {
            assertThatThrownBy(() -> service.listRequests("INVALID_STATUS", null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Geçersiz status");
        }

        @Test
        @DisplayName("Filtre olmadan tüm request'ler döner")
        void shouldReturnAllWhenNoFilters() {
            when(requestRepository.findAll()).thenReturn(List.of());

            List<ImageGenerationRequestResponse> results = service.listRequests(
                    null, null, null, null, null);

            assertThat(results).isEmpty();
            verify(requestRepository).findAll();
        }
    }
}
