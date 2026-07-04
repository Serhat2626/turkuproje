package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationResult;
import com.anadoluatlasi.backend.imagegen.repository.ImageGeneratedAssetRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-007: ImageGeneratedAssetHandoffService unit testleri.
 */
@ExtendWith(MockitoExtension.class)
class ImageGeneratedAssetHandoffServiceTest {

    @Mock
    private ImageMediaAssetRepository mediaAssetRepository;

    @Mock
    private ImageGeneratedAssetRepository generatedAssetRepository;

    private ImageGeneratedAssetHandoffService handoffService;

    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID CORRELATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handoffService = new ImageGeneratedAssetHandoffService(
                mediaAssetRepository, generatedAssetRepository);
    }

    private ImageGenerationRequest createRequest() {
        ImageGenerationRequest request = ImageGenerationRequest.builder()
                .contentProjectId(PROJECT_ID)
                .visualAssetPlanId(PLAN_ID)
                .visualAssetItemId(ITEM_ID)
                .itemType(ImageGenerationItemType.SCENE_IMAGE)
                .status(ImageGenerationRequestStatus.GENERATING)
                .idempotencyKey("test-key")
                .correlationId(CORRELATION_ID)
                .safePromptSummary("Test prompt summary")
                .requestedBy("admin-system")
                .requestedAt(LocalDateTime.now())
                .build();
        request.setId(REQUEST_ID);
        return request;
    }

    private ImageGenerationResult createResult() {
        ImageGenerationResult result = ImageGenerationResult.builder()
                .imageGenerationRequestId(REQUEST_ID)
                .visualAssetPlanId(PLAN_ID)
                .visualAssetItemId(ITEM_ID)
                .providerType("MOCK")
                .modelName("mock-image-gen-v1")
                .outputCount(1)
                .mimeType("image/png")
                .width(1024)
                .height(1024)
                .fileSizeBytes(204800L)
                .correlationId(CORRELATION_ID)
                .build();
        result.setId(RESULT_ID);
        return result;
    }

    private ImageProviderGenerationResult createProviderResult() {
        return ImageProviderGenerationResult.builder()
                .success(true)
                .providerAlias("MOCK")
                .modelName("mock-image-gen-v1")
                .outputCount(1)
                .generationDurationMs(5L)
                .mimeType("image/png")
                .width(1024)
                .height(1024)
                .fileSizeBytes(204800L)
                .correlationId(CORRELATION_ID)
                .completedAt(LocalDateTime.now())
                .qualitySignalsSummary("Mock quality")
                .safetyFlagsSummary("Mock safety")
                .build();
    }

    // ── MediaAsset Creation ─────────────────────────────────────────

    @Nested
    @DisplayName("ImageMediaAsset oluşturma")
    class MediaAssetCreation {

        @Test
        @DisplayName("Başarılı handoff ImageMediaAsset oluşturur")
        void shouldCreateMediaAsset() {
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                ImageMediaAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> {
                ImageGeneratedAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            handoffService.createAssetHandoff(createRequest(), createResult(), createProviderResult());

            ArgumentCaptor<ImageMediaAsset> captor = ArgumentCaptor.forClass(ImageMediaAsset.class);
            verify(mediaAssetRepository).save(captor.capture());

            ImageMediaAsset saved = captor.getValue();
            assertThat(saved.getAssetKind()).isEqualTo(ImageMediaAssetKind.GENERATED_IMAGE);
            assertThat(saved.getMimeType()).isEqualTo("image/png");
            assertThat(saved.getWidth()).isEqualTo(1024);
            assertThat(saved.getHeight()).isEqualTo(1024);
            assertThat(saved.getFileSizeBytes()).isEqualTo(204800L);
        }

        @Test
        @DisplayName("MediaAsset storage alanları null — gerçek storage yok")
        void mediaAssetStorageFieldsShouldBeNull() {
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                ImageMediaAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> {
                ImageGeneratedAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            handoffService.createAssetHandoff(createRequest(), createResult(), createProviderResult());

            ArgumentCaptor<ImageMediaAsset> captor = ArgumentCaptor.forClass(ImageMediaAsset.class);
            verify(mediaAssetRepository).save(captor.capture());

            ImageMediaAsset saved = captor.getValue();
            assertThat(saved.getStorageProvider()).isNull();
            assertThat(saved.getStorageBucket()).isNull();
            assertThat(saved.getStorageObjectKey()).isNull();
        }
    }

    // ── GeneratedAsset Creation ──────────────────────────────────────

    @Nested
    @DisplayName("ImageGeneratedAsset oluşturma")
    class GeneratedAssetCreation {

        @Test
        @DisplayName("Başarılı handoff ImageGeneratedAsset oluşturur")
        void shouldCreateGeneratedAsset() {
            UUID mediaId = UUID.randomUUID();
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                ImageMediaAsset saved = inv.getArgument(0);
                saved.setId(mediaId);
                return saved;
            });
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> {
                ImageGeneratedAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ImageGeneratedAsset result = handoffService.createAssetHandoff(
                    createRequest(), createResult(), createProviderResult());

            assertThat(result).isNotNull();
            assertThat(result.getImageGenerationRequestId()).isEqualTo(REQUEST_ID);
            assertThat(result.getImageGenerationResultId()).isEqualTo(RESULT_ID);
            assertThat(result.getImageMediaAssetId()).isEqualTo(mediaId);
            assertThat(result.getContentProjectId()).isEqualTo(PROJECT_ID);
            assertThat(result.getVisualAssetPlanId()).isEqualTo(PLAN_ID);
            assertThat(result.getVisualAssetItemId()).isEqualTo(ITEM_ID);
        }

        @Test
        @DisplayName("GeneratedAsset status GENERATED_PENDING_REVIEW olur")
        void generatedAssetShouldBeGeneratedPendingReview() {
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                ImageMediaAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> {
                ImageGeneratedAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ImageGeneratedAsset result = handoffService.createAssetHandoff(
                    createRequest(), createResult(), createProviderResult());

            assertThat(result.getStatus()).isEqualTo(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
        }

        @Test
        @DisplayName("Safe metadata alanları doğru set edilir")
        void shouldSetSafeMetadata() {
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                ImageMediaAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> {
                ImageGeneratedAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ImageGeneratedAsset result = handoffService.createAssetHandoff(
                    createRequest(), createResult(), createProviderResult());

            assertThat(result.getSafePromptSummary()).isEqualTo("Test prompt summary");
            assertThat(result.getQualitySignalsSummary()).isEqualTo("Mock quality");
            assertThat(result.getSafetyFlagsSummary()).isEqualTo("Mock safety");
        }

        @Test
        @DisplayName("GeneratedAsset APPROVED veya REJECTED değil")
        void generatedAssetShouldNotBeApprovedOrRejected() {
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                ImageMediaAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> {
                ImageGeneratedAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ImageGeneratedAsset result = handoffService.createAssetHandoff(
                    createRequest(), createResult(), createProviderResult());

            assertThat(result.getStatus()).isNotEqualTo(GeneratedAssetStatus.APPROVED);
            assertThat(result.getStatus()).isNotEqualTo(GeneratedAssetStatus.REJECTED);
            assertThat(result.getStatus().name()).doesNotContain("READY_FOR_PUBLISH");
            assertThat(result.getStatus().name()).doesNotContain("COMPLETED");
        }

        @Test
        @DisplayName("Review alanları null — review yapılmadı")
        void reviewFieldsShouldBeNull() {
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                ImageMediaAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> {
                ImageGeneratedAsset saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ImageGeneratedAsset result = handoffService.createAssetHandoff(
                    createRequest(), createResult(), createProviderResult());

            assertThat(result.getReviewedBy()).isNull();
            assertThat(result.getReviewedAt()).isNull();
            assertThat(result.getRejectionReason()).isNull();
        }
    }
}
