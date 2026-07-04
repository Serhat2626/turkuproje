package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageAdapter;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageWriteRequest;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageWriteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-008 Task 08: ImageStorageHandoffService unit tests.
 */
@ExtendWith(MockitoExtension.class)
class ImageStorageHandoffServiceTest {

    @Mock
    private ImageMediaAssetRepository mediaAssetRepository;
    @Mock
    private ImageStorageAdapter storageAdapter;

    private ImageMediaValidationService validationService;
    private ImageStorageHandoffService handoffService;

    private static final UUID MEDIA_ASSET_ID = UUID.randomUUID();
    private static final UUID GENERATED_ASSET_ID = UUID.randomUUID();
    private static final String CORRELATION = "handoff-test-001";

    @BeforeEach
    void setUp() {
        validationService = new ImageMediaValidationService();
        handoffService = new ImageStorageHandoffService(
                mediaAssetRepository, validationService, storageAdapter);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private byte[] createPngBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createJpegBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private ImageMediaAsset stubMediaAsset() {
        ImageMediaAsset asset = ImageMediaAsset.builder()
                .assetKind(ImageMediaAssetKind.GENERATED_IMAGE)
                .build();
        asset.setId(MEDIA_ASSET_ID);
        return asset;
    }

    private ImageStorageHandoffRequest handoffRequest(byte[] bytes) {
        return ImageStorageHandoffRequest.builder()
                .mediaAssetId(MEDIA_ASSET_ID)
                .generatedAssetId(GENERATED_ASSET_ID)
                .imageBytes(bytes)
                .correlationId(CORRELATION)
                .requestedBy("test-user")
                .requestedAt(LocalDateTime.now())
                .build();
    }

    private ImageStorageWriteResult successWriteResult() {
        return ImageStorageWriteResult.builder()
                .success(true)
                .storageProviderAlias("mock")
                .storageObjectKey("imagegen/" + MEDIA_ASSET_ID + "/" + UUID.randomUUID())
                .mimeType("image/png")
                .width(100)
                .height(50)
                .fileSizeBytes(99)
                .checksumSha256("abc123")
                .storedAt(LocalDateTime.now())
                .build();
    }

    private ImageStorageWriteResult failWriteResult() {
        return ImageStorageWriteResult.builder()
                .success(false)
                .storageProviderAlias("mock")
                .errorCode(ImageStorageErrorCode.STORAGE_WRITE_FAILED)
                .errorMessage("Storage yazma başarısız")
                .build();
    }

    // ── Success Tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("Successful handoff")
    class SuccessfulHandoff {

        @Test
        @DisplayName("Valid PNG handoff — success")
        void validPngHandoffSuccess() throws IOException {
            byte[] png = createPngBytes(100, 50);
            ImageMediaAsset asset = stubMediaAsset();

            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(storageAdapter.write(any())).thenReturn(successWriteResult());

            ImageStorageHandoffResult result = handoffService.executeHandoff(handoffRequest(png));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_STORED);
            assertThat(result.getPreviewStatus()).isEqualTo(ImagePreviewStatus.PREVIEW_AVAILABLE);
            assertThat(result.getMimeType()).isEqualTo("image/png");
            assertThat(result.getWidth()).isEqualTo(100);
            assertThat(result.getHeight()).isEqualTo(50);
            assertThat(result.getFileSizeBytes()).isEqualTo(png.length);
            assertThat(result.getChecksumSha256()).isNotNull().hasSize(64);
            assertThat(result.getStoredAt()).isNotNull();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
            assertThat(result.getMediaAssetId()).isEqualTo(MEDIA_ASSET_ID);
            assertThat(result.getGeneratedAssetId()).isEqualTo(GENERATED_ASSET_ID);

            // MediaAsset entity updated
            assertThat(asset.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_STORED);
            assertThat(asset.getPreviewStatus()).isEqualTo(ImagePreviewStatus.PREVIEW_AVAILABLE);
            assertThat(asset.getStoredAt()).isNotNull();
            assertThat(asset.getStorageErrorCode()).isNull();
            assertThat(asset.getStorageErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Valid JPEG handoff — success")
        void validJpegHandoffSuccess() throws IOException {
            byte[] jpeg = createJpegBytes(200, 150);
            ImageMediaAsset asset = stubMediaAsset();

            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(storageAdapter.write(any())).thenReturn(successWriteResult());

            ImageStorageHandoffResult result = handoffService.executeHandoff(handoffRequest(jpeg));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMimeType()).isEqualTo("image/jpeg");
            assertThat(result.getWidth()).isEqualTo(200);
            assertThat(result.getHeight()).isEqualTo(150);
        }

        @Test
        @DisplayName("Storage adapter write is called with validated data")
        void storageAdapterCalledWithValidatedData() throws IOException {
            byte[] png = createPngBytes(64, 32);
            ImageMediaAsset asset = stubMediaAsset();

            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(storageAdapter.write(any())).thenReturn(successWriteResult());

            handoffService.executeHandoff(handoffRequest(png));

            ArgumentCaptor<ImageStorageWriteRequest> captor =
                    ArgumentCaptor.forClass(ImageStorageWriteRequest.class);
            verify(storageAdapter).write(captor.capture());

            ImageStorageWriteRequest writeReq = captor.getValue();
            assertThat(writeReq.getMediaAssetId()).isEqualTo(MEDIA_ASSET_ID);
            assertThat(writeReq.getMimeType()).isEqualTo("image/png");
            assertThat(writeReq.getWidth()).isEqualTo(64);
            assertThat(writeReq.getHeight()).isEqualTo(32);
            assertThat(writeReq.getImageBytes()).isEqualTo(png);
        }

        @Test
        @DisplayName("MediaAsset internal storage reference set")
        void mediaAssetInternalStorageReferenceSet() throws IOException {
            byte[] png = createPngBytes(10, 10);
            ImageMediaAsset asset = stubMediaAsset();

            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(storageAdapter.write(any())).thenReturn(successWriteResult());

            handoffService.executeHandoff(handoffRequest(png));

            assertThat(asset.getStorageProvider()).isEqualTo("mock");
            assertThat(asset.getStorageObjectKey()).isNotNull().startsWith("imagegen/");
        }
    }

    // ── Validation Failure ──────────────────────────────────────────

    @Nested
    @DisplayName("Validation failure")
    class ValidationFailure {

        @Test
        @DisplayName("Null bytes — STORAGE_FAILED, storage adapter NOT called")
        void nullBytesFailure() {
            ImageMediaAsset asset = stubMediaAsset();
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ImageStorageHandoffResult result = handoffService.executeHandoff(
                    handoffRequest(null));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_FAILED);
            assertThat(result.getPreviewStatus()).isEqualTo(ImagePreviewStatus.PREVIEW_UNAVAILABLE);
            assertThat(result.getErrorCode()).isNotNull();

            assertThat(asset.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_FAILED);
            assertThat(asset.getPreviewStatus()).isEqualTo(ImagePreviewStatus.PREVIEW_UNAVAILABLE);
            assertThat(asset.getStoredAt()).isNull();

            verify(storageAdapter, never()).write(any());
        }

        @Test
        @DisplayName("Corrupt bytes — STORAGE_FAILED, storage adapter NOT called")
        void corruptBytesFailure() {
            ImageMediaAsset asset = stubMediaAsset();
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            byte[] corrupt = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
                    0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D};

            ImageStorageHandoffResult result = handoffService.executeHandoff(
                    handoffRequest(corrupt));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE);

            verify(storageAdapter, never()).write(any());
        }

        @Test
        @DisplayName("Validation error code propagated to MediaAsset")
        void validationErrorPropagated() {
            ImageMediaAsset asset = stubMediaAsset();
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ImageStorageHandoffResult result = handoffService.executeHandoff(
                    handoffRequest(new byte[0]));

            assertThat(result.isSuccess()).isFalse();
            assertThat(asset.getStorageErrorCode()).isNotNull();
            assertThat(asset.getStorageErrorMessage()).isNotNull();
        }
    }

    // ── Storage Write Failure ───────────────────────────────────────

    @Nested
    @DisplayName("Storage write failure")
    class StorageWriteFailure {

        @Test
        @DisplayName("Storage adapter returns failure — STORAGE_FAILED")
        void storageAdapterFailure() throws IOException {
            byte[] png = createPngBytes(10, 10);
            ImageMediaAsset asset = stubMediaAsset();

            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(storageAdapter.write(any())).thenReturn(failWriteResult());

            ImageStorageHandoffResult result = handoffService.executeHandoff(handoffRequest(png));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_FAILED);
            assertThat(result.getPreviewStatus()).isEqualTo(ImagePreviewStatus.PREVIEW_UNAVAILABLE);
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);

            assertThat(asset.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_FAILED);
            assertThat(asset.getStoredAt()).isNull();
        }

        @Test
        @DisplayName("Storage adapter throws exception — STORAGE_FAILED")
        void storageAdapterException() throws IOException {
            byte[] png = createPngBytes(10, 10);
            ImageMediaAsset asset = stubMediaAsset();

            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(storageAdapter.write(any())).thenThrow(new RuntimeException("disk full"));

            ImageStorageHandoffResult result = handoffService.executeHandoff(handoffRequest(png));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
            // Raw exception message not in safe message
            assertThat(result.getErrorMessage()).doesNotContain("disk full");
        }
    }

    // ── Missing Media Asset ─────────────────────────────────────────

    @Nested
    @DisplayName("Missing media asset")
    class MissingMediaAsset {

        @Test
        @DisplayName("Media asset not found — safe error")
        void mediaAssetNotFound() {
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.empty());

            ImageStorageHandoffResult result = handoffService.executeHandoff(
                    handoffRequest(new byte[]{1, 2, 3}));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isNotNull();

            verify(storageAdapter, never()).write(any());
        }
    }

    // ── Safety Guardrails ───────────────────────────────────────────

    @Nested
    @DisplayName("Safety guardrails")
    class SafetyGuardrails {

        private static final Set<String> FORBIDDEN_RESULT_FIELDS = Set.of(
                "storageObjectKey", "storageReference",
                "providerUrl", "providerImageUrl", "providerTemporaryUrl",
                "signedUrl", "publicUrl", "cdnUrl",
                "base64", "imageBase64", "imageBinary",
                "rawStorageProviderResponse", "rawProviderResponse", "rawProviderPayload",
                "apiKey", "secret", "internalFullPrompt",
                "readyForPublish", "publishStatus", "exportStatus"
        );

        @Test
        @DisplayName("Handoff result has no forbidden fields")
        void resultNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ImageStorageHandoffResult.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_RESULT_FIELDS) {
                assertThat(fields)
                        .as("HandoffResult should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Handoff request has no forbidden fields")
        void requestNoForbiddenFields() {
            Set<String> forbidden = Set.of(
                    "providerUrl", "signedUrl", "publicUrl", "cdnUrl",
                    "storageObjectKey", "rawProviderPayload", "rawProviderResponse",
                    "apiKey", "secret", "internalFullPrompt",
                    "readyForPublish", "publishStatus", "exportStatus"
            );

            Set<String> fields = Arrays.stream(ImageStorageHandoffRequest.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String f : forbidden) {
                assertThat(fields).as("HandoffRequest should not have '%s'", f)
                        .doesNotContain(f);
            }
        }

        @Test
        @DisplayName("Error messages do not expose storage keys or URLs")
        void errorMessagesNoStorageKeysOrUrls() throws IOException {
            byte[] png = createPngBytes(10, 10);
            ImageMediaAsset asset = stubMediaAsset();

            when(mediaAssetRepository.findById(MEDIA_ASSET_ID)).thenReturn(Optional.of(asset));
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(storageAdapter.write(any())).thenReturn(failWriteResult());

            ImageStorageHandoffResult result = handoffService.executeHandoff(handoffRequest(png));

            if (result.getErrorMessage() != null) {
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("s3://");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("gs://");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("http");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("storageObjectKey");
            }
        }
    }
}
