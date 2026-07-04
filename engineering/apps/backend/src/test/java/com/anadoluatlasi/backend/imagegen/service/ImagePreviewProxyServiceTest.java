package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.ImageGeneratedAssetRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageAdapter;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageReadRequest;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageReadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-008 Task 10: ImagePreviewProxyService unit tests.
 */
@ExtendWith(MockitoExtension.class)
class ImagePreviewProxyServiceTest {

    @Mock
    private ImageGeneratedAssetRepository generatedAssetRepository;
    @Mock
    private ImageMediaAssetRepository mediaAssetRepository;
    @Mock
    private ImageStorageAdapter storageAdapter;

    private ImagePreviewProxyService previewService;

    private static final UUID GENERATED_ASSET_ID = UUID.randomUUID();
    private static final UUID MEDIA_ASSET_ID = UUID.randomUUID();
    private static final String STORAGE_KEY = "imagegen/" + MEDIA_ASSET_ID + "/test-key";
    private static final byte[] TEST_IMAGE_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG header
            0x01, 0x02, 0x03, 0x04, 0x05
    };

    @BeforeEach
    void setUp() {
        previewService = new ImagePreviewProxyService(
                generatedAssetRepository, mediaAssetRepository, storageAdapter);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ImageGeneratedAsset stubGeneratedAsset() {
        ImageGeneratedAsset asset = ImageGeneratedAsset.builder()
                .imageMediaAssetId(MEDIA_ASSET_ID)
                .imageGenerationRequestId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .visualAssetPlanId(UUID.randomUUID())
                .visualAssetItemId(UUID.randomUUID())
                .status(GeneratedAssetStatus.GENERATED_PENDING_REVIEW)
                .build();
        asset.setId(GENERATED_ASSET_ID);
        return asset;
    }

    private ImageMediaAsset stubMediaAsset(ImageStorageHandoffStatus handoffStatus,
                                            ImagePreviewStatus previewStatus) {
        ImageMediaAsset asset = ImageMediaAsset.builder()
                .assetKind(ImageMediaAssetKind.GENERATED_IMAGE)
                .mimeType("image/png")
                .width(100)
                .height(50)
                .fileSizeBytes(99L)
                .storageObjectKey(STORAGE_KEY)
                .storageProvider("mock")
                .storageHandoffStatus(handoffStatus)
                .previewStatus(previewStatus)
                .build();
        asset.setId(MEDIA_ASSET_ID);
        return asset;
    }

    private ImageStorageReadResult successReadResult() {
        return ImageStorageReadResult.builder()
                .success(true)
                .imageBytes(TEST_IMAGE_BYTES)
                .mimeType("image/png")
                .fileSizeBytes(TEST_IMAGE_BYTES.length)
                .build();
    }

    // ── Success Tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("Successful preview")
    class SuccessfulPreview {

        @Test
        @DisplayName("Available asset returns image bytes")
        void availableAssetReturnsBytes() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAsset(
                            ImageStorageHandoffStatus.STORAGE_STORED,
                            ImagePreviewStatus.PREVIEW_AVAILABLE)));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).isEqualTo(TEST_IMAGE_BYTES);
            assertThat(result.getMimeType()).isEqualTo("image/png");
            assertThat(result.getFileSizeBytes()).isEqualTo(TEST_IMAGE_BYTES.length);
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Storage adapter read called with internal key")
        void storageReadCalledWithInternalKey() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAsset(
                            ImageStorageHandoffStatus.STORAGE_STORED,
                            ImagePreviewStatus.PREVIEW_AVAILABLE)));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            previewService.getPreview(GENERATED_ASSET_ID);

            ArgumentCaptor<ImageStorageReadRequest> captor =
                    ArgumentCaptor.forClass(ImageStorageReadRequest.class);
            verify(storageAdapter).read(captor.capture());

            assertThat(captor.getValue().getStorageObjectKey()).isEqualTo(STORAGE_KEY);
            assertThat(captor.getValue().getMediaAssetId()).isEqualTo(MEDIA_ASSET_ID);
        }

        @Test
        @DisplayName("Preview does not change GeneratedAsset status")
        void previewDoesNotChangeStatus() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset();
            GeneratedAssetStatus originalStatus = genAsset.getStatus();

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAsset(
                            ImageStorageHandoffStatus.STORAGE_STORED,
                            ImagePreviewStatus.PREVIEW_AVAILABLE)));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(genAsset.getStatus()).isEqualTo(originalStatus);
            verify(generatedAssetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Preview does not change MediaAsset status")
        void previewDoesNotChangeMediaStatus() {
            ImageMediaAsset mediaAsset = stubMediaAsset(
                    ImageStorageHandoffStatus.STORAGE_STORED,
                    ImagePreviewStatus.PREVIEW_AVAILABLE);

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(mediaAsset.getStorageHandoffStatus())
                    .isEqualTo(ImageStorageHandoffStatus.STORAGE_STORED);
            assertThat(mediaAsset.getPreviewStatus())
                    .isEqualTo(ImagePreviewStatus.PREVIEW_AVAILABLE);
            verify(mediaAssetRepository, never()).save(any());
        }
    }

    // ── Not Found / Missing ─────────────────────────────────────────

    @Nested
    @DisplayName("Missing entities")
    class MissingEntities {

        @Test
        @DisplayName("GeneratedAsset not found — safe error")
        void generatedAssetNotFound() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.empty());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE);
            verify(storageAdapter, never()).read(any());
        }

        @Test
        @DisplayName("No linked MediaAsset — safe error")
        void noLinkedMediaAsset() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset();
            genAsset.setImageMediaAssetId(null);

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("MediaAsset not found — safe error")
        void mediaAssetNotFound() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.empty());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE);
        }
    }

    // ── Eligibility Failures ────────────────────────────────────────

    @Nested
    @DisplayName("Eligibility failures")
    class EligibilityFailures {

        @Test
        @DisplayName("STORAGE_FAILED — preview not available")
        void storageFailedNoPreview() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAsset(
                            ImageStorageHandoffStatus.STORAGE_FAILED,
                            ImagePreviewStatus.PREVIEW_UNAVAILABLE)));

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE);
            verify(storageAdapter, never()).read(any());
        }

        @Test
        @DisplayName("PREVIEW_UNAVAILABLE — preview not available")
        void previewUnavailable() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAsset(
                            ImageStorageHandoffStatus.STORAGE_STORED,
                            ImagePreviewStatus.PREVIEW_UNAVAILABLE)));

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            verify(storageAdapter, never()).read(any());
        }

        @Test
        @DisplayName("Missing storage key — safe error")
        void missingStorageKey() {
            ImageMediaAsset mediaAsset = stubMediaAsset(
                    ImageStorageHandoffStatus.STORAGE_STORED,
                    ImagePreviewStatus.PREVIEW_AVAILABLE);
            mediaAsset.setStorageObjectKey(null);

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
            verify(storageAdapter, never()).read(any());
        }
    }

    // ── Storage Read Failure ────────────────────────────────────────

    @Nested
    @DisplayName("Storage read failure")
    class StorageReadFailure {

        @Test
        @DisplayName("Storage adapter returns failure")
        void storageReadFailure() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAsset(
                            ImageStorageHandoffStatus.STORAGE_STORED,
                            ImagePreviewStatus.PREVIEW_AVAILABLE)));
            when(storageAdapter.read(any())).thenReturn(ImageStorageReadResult.builder()
                    .success(false)
                    .errorCode(ImageStorageErrorCode.STORAGE_READ_FAILED)
                    .errorMessage("Depolama okuma başarısız")
                    .build());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
        }

        @Test
        @DisplayName("Storage adapter throws exception — safe error")
        void storageReadException() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAsset(
                            ImageStorageHandoffStatus.STORAGE_STORED,
                            ImagePreviewStatus.PREVIEW_AVAILABLE)));
            when(storageAdapter.read(any())).thenThrow(new RuntimeException("disk error"));

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
            assertThat(result.getErrorMessage()).doesNotContain("disk error");
        }
    }

    // ── Safety Guardrails ───────────────────────────────────────────

    @Nested
    @DisplayName("Safety guardrails")
    class SafetyGuardrails {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "storageObjectKey", "storageReference",
                "publicUrl", "signedUrl", "cdnUrl",
                "providerUrl", "providerImageUrl",
                "base64", "imageBase64",
                "rawProviderResponse", "rawStorageProviderResponse",
                "apiKey", "secret", "internalFullPrompt",
                "readyForPublish", "publishStatus", "exportStatus"
        );

        @Test
        @DisplayName("PreviewProxyResult has no forbidden fields")
        void resultNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ImagePreviewProxyResult.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("PreviewProxyResult should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Error messages do not expose storage keys or URLs")
        void errorMessagesNoInternalDetails() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAsset(
                            ImageStorageHandoffStatus.STORAGE_STORED,
                            ImagePreviewStatus.PREVIEW_AVAILABLE)));
            when(storageAdapter.read(any())).thenReturn(ImageStorageReadResult.builder()
                    .success(false)
                    .errorCode(ImageStorageErrorCode.STORAGE_READ_FAILED)
                    .errorMessage("Depolama okuma başarısız")
                    .build());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            if (result.getErrorMessage() != null) {
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("s3://");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("gs://");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("http");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("storageObjectKey");
                assertThat(result.getErrorMessage()).doesNotContain(STORAGE_KEY);
            }
        }
    }
}
