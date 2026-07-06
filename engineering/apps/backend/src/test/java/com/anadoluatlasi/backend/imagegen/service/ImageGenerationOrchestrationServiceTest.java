package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.review.GenerationReviewHandoffAdapter;
import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGenerationResultResponse;
import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.provider.ImageGenerationProviderGateway;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationRequest;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationResult;
import com.anadoluatlasi.backend.imagegen.provider.MockImageGenerationProviderGateway;
import com.anadoluatlasi.backend.imagegen.repository.ImageGenerationRequestRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageGenerationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-007: ImageGenerationOrchestrationService unit testleri.
 *
 * <p>Mock provider ve mock repository'lerle test edilir.
 * Spring context yüklenmez.</p>
 */
@ExtendWith(MockitoExtension.class)
class ImageGenerationOrchestrationServiceTest {

    @Mock
    private ImageGenerationRequestRepository requestRepository;

    @Mock
    private ImageGenerationResultRepository resultRepository;

    @Mock
    private ImageGeneratedAssetHandoffService handoffService;

    private MockImageGenerationProviderGateway mockProvider;
    private ImageGenerationOrchestrationService orchestrationService;

    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID CORRELATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockProvider = new MockImageGenerationProviderGateway();
        ImmediateStorageHandoffService immediateHandoff = new ImmediateStorageHandoffService(
                new ImmediateStorageHandoffProperties(),
                org.mockito.Mockito.mock(ControlledStorageHandoffOrchestrationService.class));
        GenerationReviewHandoffAdapter generationReviewHandoffAdapter =
                org.mockito.Mockito.mock(GenerationReviewHandoffAdapter.class);
        orchestrationService = new ImageGenerationOrchestrationService(
                requestRepository, resultRepository, mockProvider, handoffService, immediateHandoff,
                generationReviewHandoffAdapter);

        // FP-010 Task 04: Stub handoff to return valid asset (immediate handoff needs getId())
        ImageGeneratedAsset stubAsset = new ImageGeneratedAsset();
        stubAsset.setId(UUID.randomUUID());
        stubAsset.setStatus(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
        org.mockito.Mockito.lenient().when(handoffService.createAssetHandoff(any(), any(), any()))
                .thenReturn(stubAsset);
    }

    private ImageGenerationRequest createRequestedRequest() {
        ImageGenerationRequest request = ImageGenerationRequest.builder()
                .contentProjectId(PROJECT_ID)
                .visualAssetPlanId(PLAN_ID)
                .visualAssetItemId(ITEM_ID)
                .itemType(ImageGenerationItemType.SCENE_IMAGE)
                .status(ImageGenerationRequestStatus.REQUESTED)
                .idempotencyKey("test-key")
                .correlationId(CORRELATION_ID)
                .requestedBy("admin-system")
                .requestedAt(LocalDateTime.now())
                .build();
        request.setId(REQUEST_ID);
        return request;
    }

    // ── Success Flow Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("executeGeneration — Success akışı")
    class SuccessFlow {

        @Test
        @DisplayName("REQUESTED request → GENERATING → GENERATED_PENDING_REVIEW")
        void shouldTransitionToGeneratedPendingReview() {
            ImageGenerationRequest request = createRequestedRequest();

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            ImageGenerationResultResponse response = orchestrationService.executeGeneration(REQUEST_ID);

            assertThat(response).isNotNull();
            assertThat(response.getProviderType()).isEqualTo("MOCK");
            assertThat(response.getOutputCount()).isEqualTo(1);
            assertThat(response.getMimeType()).isEqualTo("image/png");
            assertThat(response.getWidth()).isEqualTo(1024);
            assertThat(response.getHeight()).isEqualTo(1024);
            assertThat(response.getErrorCode()).isNull();
            assertThat(response.getErrorMessage()).isNull();

            // Request son status GENERATED_PENDING_REVIEW olmalı
            assertThat(request.getStatus()).isEqualTo(ImageGenerationRequestStatus.GENERATED_PENDING_REVIEW);
        }

        @Test
        @DisplayName("ImageGenerationResult oluşturulur ve kaydedilir")
        void shouldCreateAndSaveGenerationResult() {
            ImageGenerationRequest request = createRequestedRequest();

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            orchestrationService.executeGeneration(REQUEST_ID);

            ArgumentCaptor<ImageGenerationResult> resultCaptor =
                    ArgumentCaptor.forClass(ImageGenerationResult.class);
            verify(resultRepository).save(resultCaptor.capture());

            ImageGenerationResult saved = resultCaptor.getValue();
            assertThat(saved.getImageGenerationRequestId()).isEqualTo(REQUEST_ID);
            assertThat(saved.getVisualAssetPlanId()).isEqualTo(PLAN_ID);
            assertThat(saved.getVisualAssetItemId()).isEqualTo(ITEM_ID);
            assertThat(saved.getProviderType()).isEqualTo("MOCK");
            assertThat(saved.getCorrelationId()).isEqualTo(CORRELATION_ID);
        }

        @Test
        @DisplayName("Response providerRequestId içermez — DTO'da alan yok")
        void responseShouldNotContainProviderRequestId() {
            ImageGenerationRequest request = createRequestedRequest();

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            ImageGenerationResultResponse response = orchestrationService.executeGeneration(REQUEST_ID);

            // ImageGenerationResultResponse DTO'sında providerRequestId field yok
            // compile-time guarantee
            assertThat(response.getId()).isNotNull();
        }

        @Test
        @DisplayName("Request providerType ve modelName güncellenir")
        void shouldUpdateRequestProviderMetadata() {
            ImageGenerationRequest request = createRequestedRequest();

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            orchestrationService.executeGeneration(REQUEST_ID);

            assertThat(request.getProviderType()).isEqualTo("MOCK");
            assertThat(request.getModelName()).isEqualTo("mock-image-gen-v1");
            assertThat(request.getCompletedAt()).isNotNull();
        }
    }

    // ── Failure Flow Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("executeGeneration — Failure akışı")
    class FailureFlow {

        @Test
        @DisplayName("MOCK_FAIL trigger ile request FAILED olur")
        void shouldTransitionToFailedOnMockFailure() {
            ImageGenerationRequest request = createRequestedRequest();
            request.setSafePromptSummary("MOCK_FAIL test trigger");

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            ImageGenerationResultResponse response = orchestrationService.executeGeneration(REQUEST_ID);

            assertThat(request.getStatus()).isEqualTo(ImageGenerationRequestStatus.FAILED);
            assertThat(response.getErrorCode()).isEqualTo("PROVIDER_REJECTED_REQUEST");
            assertThat(response.getErrorMessage()).isNotBlank();
        }

        @Test
        @DisplayName("Failed result normalized error code içerir")
        void failedResultShouldContainNormalizedError() {
            ImageGenerationRequest request = createRequestedRequest();
            request.setSafePromptSummary("MOCK_FAIL");

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            orchestrationService.executeGeneration(REQUEST_ID);

            ArgumentCaptor<ImageGenerationResult> resultCaptor =
                    ArgumentCaptor.forClass(ImageGenerationResult.class);
            verify(resultRepository).save(resultCaptor.capture());

            ImageGenerationResult saved = resultCaptor.getValue();
            assertThat(saved.getErrorCode()).isEqualTo(ImageGenerationErrorCode.PROVIDER_REJECTED_REQUEST);
            assertThat(saved.getErrorMessage()).isNotBlank();
        }

        @Test
        @DisplayName("Request error metadata güncellenir")
        void requestShouldContainErrorMetadata() {
            ImageGenerationRequest request = createRequestedRequest();
            request.setSafePromptSummary("MOCK_FAIL");

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            orchestrationService.executeGeneration(REQUEST_ID);

            assertThat(request.getErrorCode()).isEqualTo(ImageGenerationErrorCode.PROVIDER_REJECTED_REQUEST);
            assertThat(request.getErrorMessage()).isNotBlank();
            assertThat(request.getCompletedAt()).isNotNull();
        }
    }

    // ── Validation Tests ────────────────────────────────────────────

    @Nested
    @DisplayName("executeGeneration — Validation")
    class Validation {

        @Test
        @DisplayName("Request bulunamazsa NotFoundException")
        void shouldThrowNotFoundForMissingRequest() {
            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orchestrationService.executeGeneration(REQUEST_ID))
                    .isInstanceOf(NotFoundException.class);

            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("REQUESTED olmayan request reddedilir")
        void shouldRejectNonRequestedStatus() {
            ImageGenerationRequest request = createRequestedRequest();
            request.setStatus(ImageGenerationRequestStatus.GENERATING);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> orchestrationService.executeGeneration(REQUEST_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("REQUESTED");

            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("GENERATED_PENDING_REVIEW request reddedilir")
        void shouldRejectAlreadyGeneratedRequest() {
            ImageGenerationRequest request = createRequestedRequest();
            request.setStatus(ImageGenerationRequestStatus.GENERATED_PENDING_REVIEW);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> orchestrationService.executeGeneration(REQUEST_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("REQUESTED");
        }

        @Test
        @DisplayName("FAILED request reddedilir")
        void shouldRejectFailedRequest() {
            ImageGenerationRequest request = createRequestedRequest();
            request.setStatus(ImageGenerationRequestStatus.FAILED);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> orchestrationService.executeGeneration(REQUEST_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("REQUESTED");
        }
    }

    // ── Asset Creation Guard Tests ──────────────────────────────────

    @Nested
    @DisplayName("executeGeneration — Asset creation yok")
    class AssetCreationGuard {

        @Test
        @DisplayName("GeneratedAsset oluşturulmaz — repository kullanılmaz")
        void shouldNotCreateGeneratedAsset() {
            ImageGenerationRequest request = createRequestedRequest();

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            orchestrationService.executeGeneration(REQUEST_ID);

            // Only requestRepository and resultRepository should be called
            verify(requestRepository, atLeastOnce()).save(any());
            verify(resultRepository, times(1)).save(any());
            // ImageGeneratedAssetRepository and ImageMediaAssetRepository
            // are not injected into this service — compile-time guarantee
        }
    }

    // ── Status Guard Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("Status kuralları")
    class StatusGuard {

        @Test
        @DisplayName("Success akışında READY_FOR_PUBLISH kullanılmaz")
        void successShouldNotUseReadyForPublish() {
            ImageGenerationRequest request = createRequestedRequest();

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            orchestrationService.executeGeneration(REQUEST_ID);

            assertThat(request.getStatus()).isNotEqualTo(ImageGenerationRequestStatus.REQUESTED);
            assertThat(request.getStatus().name()).doesNotContain("READY_FOR_PUBLISH");
            assertThat(request.getStatus().name()).doesNotContain("COMPLETED");
            assertThat(request.getStatus().name()).doesNotContain("APPROVED");
        }
    }

    // ── Handoff Integration Tests ───────────────────────────────────

    @Nested
    @DisplayName("executeGeneration — Handoff entegrasyonu")
    class HandoffIntegration {

        @Test
        @DisplayName("Success akışında handoff service çağrılır")
        void shouldCallHandoffOnSuccess() {
            ImageGenerationRequest request = createRequestedRequest();

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            orchestrationService.executeGeneration(REQUEST_ID);

            verify(handoffService, times(1)).createAssetHandoff(
                    eq(request), any(ImageGenerationResult.class), any());
        }

        @Test
        @DisplayName("Failure akışında handoff service çağrılmaz")
        void shouldNotCallHandoffOnFailure() {
            ImageGenerationRequest request = createRequestedRequest();
            request.setSafePromptSummary("MOCK_FAIL");

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(resultRepository.save(any())).thenAnswer(inv -> {
                ImageGenerationResult result = inv.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            orchestrationService.executeGeneration(REQUEST_ID);

            verify(handoffService, never()).createAssetHandoff(any(), any(), any());
        }
    }
}
