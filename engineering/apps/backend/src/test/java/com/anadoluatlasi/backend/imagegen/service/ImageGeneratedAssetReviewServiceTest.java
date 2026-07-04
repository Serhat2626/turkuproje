package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.imagegen.controller.dto.ApproveGeneratedAssetRequest;
import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGeneratedAssetResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.RejectGeneratedAssetRequest;
import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.GeneratedAssetReviewRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-007: ImageGeneratedAssetReviewService unit testleri.
 */
@ExtendWith(MockitoExtension.class)
class ImageGeneratedAssetReviewServiceTest {

    @Mock
    private ImageGeneratedAssetRepository generatedAssetRepository;

    @Mock
    private ImageMediaAssetRepository mediaAssetRepository;

    @Mock
    private GeneratedAssetReviewRepository reviewRepository;

    private ImageGeneratedAssetReviewService reviewService;

    private static final UUID ASSET_ID = UUID.randomUUID();
    private static final UUID MEDIA_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reviewService = new ImageGeneratedAssetReviewService(
                generatedAssetRepository, mediaAssetRepository, reviewRepository);
    }

    private ImageGeneratedAsset createPendingReviewAsset() {
        ImageGeneratedAsset asset = ImageGeneratedAsset.builder()
                .imageGenerationRequestId(REQUEST_ID)
                .imageGenerationResultId(RESULT_ID)
                .imageMediaAssetId(MEDIA_ID)
                .contentProjectId(PROJECT_ID)
                .visualAssetPlanId(PLAN_ID)
                .visualAssetItemId(ITEM_ID)
                .status(GeneratedAssetStatus.GENERATED_PENDING_REVIEW)
                .safePromptSummary("Test prompt")
                .build();
        asset.setId(ASSET_ID);
        return asset;
    }

    private ImageMediaAsset createMediaAsset() {
        ImageMediaAsset media = ImageMediaAsset.builder()
                .assetKind(ImageMediaAssetKind.GENERATED_IMAGE)
                .mimeType("image/png")
                .width(1024)
                .height(1024)
                .build();
        media.setId(MEDIA_ID);
        return media;
    }

    // ── Approve Tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("approveGeneratedAsset")
    class ApproveTests {

        @Test
        @DisplayName("GENERATED_PENDING_REVIEW asset approve edilir")
        void shouldApproveAsset() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            ApproveGeneratedAssetRequest request = ApproveGeneratedAssetRequest.builder()
                    .reviewNote("Looks good").build();

            ImageGeneratedAssetResponse response = reviewService.approveGeneratedAsset(ASSET_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Approve sonrası GeneratedAsset status APPROVED olur")
        void assetShouldBeApproved() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            reviewService.approveGeneratedAsset(ASSET_ID, null);

            assertThat(asset.getStatus()).isEqualTo(GeneratedAssetStatus.APPROVED);
            assertThat(asset.getReviewedBy()).isNotBlank();
            assertThat(asset.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("Approve sonrası MediaAsset REVIEW_APPROVED olur")
        void mediaAssetShouldBeReviewApproved() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            reviewService.approveGeneratedAsset(ASSET_ID, null);

            assertThat(media.getReviewStatus()).isEqualTo(ImageMediaAssetReviewStatus.REVIEW_APPROVED);
        }

        @Test
        @DisplayName("Approve review record oluşturur")
        void shouldCreateApproveReviewRecord() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            reviewService.approveGeneratedAsset(ASSET_ID,
                    ApproveGeneratedAssetRequest.builder().reviewNote("OK").build());

            ArgumentCaptor<GeneratedAssetReview> captor =
                    ArgumentCaptor.forClass(GeneratedAssetReview.class);
            verify(reviewRepository).save(captor.capture());

            GeneratedAssetReview review = captor.getValue();
            assertThat(review.getGeneratedAssetId()).isEqualTo(ASSET_ID);
            assertThat(review.getDecision()).isEqualTo(GeneratedAssetReviewDecision.APPROVED);
            assertThat(review.getReviewedBy()).isNotBlank();
            assertThat(review.getReviewedAt()).isNotNull();
        }
    }

    // ── Reject Tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectGeneratedAsset")
    class RejectTests {

        @Test
        @DisplayName("GENERATED_PENDING_REVIEW asset reject edilir")
        void shouldRejectAsset() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            RejectGeneratedAssetRequest request = RejectGeneratedAssetRequest.builder()
                    .rejectionReason("Kalitesiz görsel").build();

            ImageGeneratedAssetResponse response = reviewService.rejectGeneratedAsset(ASSET_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Reject sonrası GeneratedAsset status REJECTED olur")
        void assetShouldBeRejected() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            RejectGeneratedAssetRequest request = RejectGeneratedAssetRequest.builder()
                    .rejectionReason("Yanlış sahne").build();

            reviewService.rejectGeneratedAsset(ASSET_ID, request);

            assertThat(asset.getStatus()).isEqualTo(GeneratedAssetStatus.REJECTED);
            assertThat(asset.getRejectionReason()).isEqualTo("Yanlış sahne");
            assertThat(asset.getReviewedBy()).isNotBlank();
            assertThat(asset.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("Reject sonrası MediaAsset REVIEW_REJECTED olur")
        void mediaAssetShouldBeReviewRejected() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            RejectGeneratedAssetRequest request = RejectGeneratedAssetRequest.builder()
                    .rejectionReason("Yanlış sahne").build();

            reviewService.rejectGeneratedAsset(ASSET_ID, request);

            assertThat(media.getReviewStatus()).isEqualTo(ImageMediaAssetReviewStatus.REVIEW_REJECTED);
        }

        @Test
        @DisplayName("Reject review record oluşturur")
        void shouldCreateRejectReviewRecord() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            RejectGeneratedAssetRequest request = RejectGeneratedAssetRequest.builder()
                    .rejectionReason("Kompozisyon hatası").build();

            reviewService.rejectGeneratedAsset(ASSET_ID, request);

            ArgumentCaptor<GeneratedAssetReview> captor =
                    ArgumentCaptor.forClass(GeneratedAssetReview.class);
            verify(reviewRepository).save(captor.capture());

            GeneratedAssetReview review = captor.getValue();
            assertThat(review.getGeneratedAssetId()).isEqualTo(ASSET_ID);
            assertThat(review.getDecision()).isEqualTo(GeneratedAssetReviewDecision.REJECTED);
            assertThat(review.getRejectionReason()).isEqualTo("Kompozisyon hatası");
            assertThat(review.getReviewedBy()).isNotBlank();
            assertThat(review.getReviewedAt()).isNotNull();
        }
    }

    // ── Validation Tests ────────────────────────────────────────────

    @Nested
    @DisplayName("Validation rules")
    class ValidationTests {

        @Test
        @DisplayName("Asset bulunamazsa NotFoundException")
        void shouldThrowNotFoundForMissingAsset() {
            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.approveGeneratedAsset(ASSET_ID, null))
                    .isInstanceOf(NotFoundException.class);

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("APPROVED asset tekrar approve edilemez")
        void shouldRejectAlreadyApproved() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            asset.setStatus(GeneratedAssetStatus.APPROVED);

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> reviewService.approveGeneratedAsset(ASSET_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("GENERATED_PENDING_REVIEW");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECTED asset tekrar reject edilemez")
        void shouldRejectAlreadyRejected() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            asset.setStatus(GeneratedAssetStatus.REJECTED);

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

            RejectGeneratedAssetRequest request = RejectGeneratedAssetRequest.builder()
                    .rejectionReason("reason").build();

            assertThatThrownBy(() -> reviewService.rejectGeneratedAsset(ASSET_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("GENERATED_PENDING_REVIEW");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("APPROVED asset reject edilemez")
        void shouldNotAllowRejectOnApproved() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            asset.setStatus(GeneratedAssetStatus.APPROVED);

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

            RejectGeneratedAssetRequest request = RejectGeneratedAssetRequest.builder()
                    .rejectionReason("reason").build();

            assertThatThrownBy(() -> reviewService.rejectGeneratedAsset(ASSET_ID, request))
                    .isInstanceOf(BusinessException.class);

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECTED asset approve edilemez")
        void shouldNotAllowApproveOnRejected() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            asset.setStatus(GeneratedAssetStatus.REJECTED);

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> reviewService.approveGeneratedAsset(ASSET_ID, null))
                    .isInstanceOf(BusinessException.class);

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("MediaAsset null ise review güvenli şekilde devam eder")
        void shouldHandleNullMediaAssetGracefully() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            asset.setImageMediaAssetId(null);

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            // Should NOT throw — media asset update skipped safely
            ImageGeneratedAssetResponse response = reviewService.approveGeneratedAsset(ASSET_ID, null);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("APPROVED");
            verify(mediaAssetRepository, never()).save(any());
        }
    }

    // ── Status Guard Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("Status kuralları")
    class StatusGuard {

        @Test
        @DisplayName("Approve READY_FOR_PUBLISH kullanmaz")
        void approveShouldNotUseReadyForPublish() {
            ImageGeneratedAsset asset = createPendingReviewAsset();
            ImageMediaAsset media = createMediaAsset();

            when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(generatedAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mediaAssetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.save(any())).thenAnswer(inv -> {
                GeneratedAssetReview review = inv.getArgument(0);
                review.setId(UUID.randomUUID());
                return review;
            });

            reviewService.approveGeneratedAsset(ASSET_ID, null);

            assertThat(asset.getStatus().name()).doesNotContain("READY_FOR_PUBLISH");
            assertThat(asset.getStatus().name()).doesNotContain("COMPLETED");
        }
    }
}
