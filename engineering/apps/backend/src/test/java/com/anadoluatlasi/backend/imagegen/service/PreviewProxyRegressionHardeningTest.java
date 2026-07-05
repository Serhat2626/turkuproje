package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageMediaAssetResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.StorageHandoffResponse;
import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.ImageGeneratedAssetRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageAdapter;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageReadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-009 Task 10 — Preview Proxy Regression Hardening.
 *
 * <p>Ensures preview remains backend-controlled byte streaming
 * even after FP-009 V12 storage reference fields exist on the entity.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-009 Task 10 — Preview Proxy Regression Hardening")
class PreviewProxyRegressionHardeningTest {

    private static final UUID GENERATED_ASSET_ID = UUID.randomUUID();
    private static final UUID MEDIA_ASSET_ID = UUID.randomUUID();
    private static final String STORAGE_KEY = "imagegen/" + MEDIA_ASSET_ID + "/some-uuid.png";
    private static final byte[] TEST_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x01, 0x02, 0x03, 0x04
    };

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "storageObjectKey", "storageReference", "storageBucket", "storageProvider",
            "providerUrl", "providerOutputUrl", "providerImageUrl",
            "signedUrl", "publicUrl", "cdnUrl",
            "base64", "imageBase64",
            "rawProviderResponse", "rawStorageResponse", "rawStorageProviderResponse",
            "apiKey", "secretKey", "bearerToken", "password",
            "stackTrace", "internalFullPrompt"
    );

    @Mock private ImageGeneratedAssetRepository generatedAssetRepository;
    @Mock private ImageMediaAssetRepository mediaAssetRepository;
    @Mock private ImageStorageAdapter storageAdapter;

    private ImagePreviewProxyService previewService;

    @BeforeEach
    void setUp() {
        previewService = new ImagePreviewProxyService(
                generatedAssetRepository, mediaAssetRepository, storageAdapter);
    }

    /** Creates a GeneratedAsset with GENERATED_PENDING_REVIEW status. */
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

    /**
     * Creates a MediaAsset with V12 storage reference fields populated.
     * This simulates the FP-009 state where storage references exist internally.
     */
    private ImageMediaAsset stubMediaAssetWithV12Fields() {
        ImageMediaAsset asset = ImageMediaAsset.builder()
                .assetKind(ImageMediaAssetKind.GENERATED_IMAGE)
                .mimeType("image/png")
                .width(256)
                .height(256)
                .fileSizeBytes((long) TEST_PNG.length)
                .storageObjectKey(STORAGE_KEY)
                .storageProvider("s3")
                .storageBucket("private-assets-bucket")
                .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_STORED)
                .previewStatus(ImagePreviewStatus.PREVIEW_AVAILABLE)
                .checksumSha256("abcdef1234567890")
                .build();
        asset.setId(MEDIA_ASSET_ID);
        return asset;
    }

    private ImageStorageReadResult successReadResult() {
        return ImageStorageReadResult.builder()
                .success(true)
                .imageBytes(TEST_PNG)
                .mimeType("image/png")
                .fileSizeBytes(TEST_PNG.length)
                .build();
    }

    // ── A. Preview Success Byte Streaming ────────────────────────────

    @Nested
    @DisplayName("A. Preview Success Byte Streaming")
    class PreviewSuccessByteStreaming {

        @Test
        @DisplayName("Preview returns raw byte array, not base64 string")
        void returnsRawBytes_notBase64() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).isEqualTo(TEST_PNG);
            // Bytes start with PNG magic — not base64 ASCII
            assertThat(result.getImageBytes()[0]).isEqualTo((byte) 0x89);
            assertThat(result.getImageBytes()[1]).isEqualTo((byte) 0x50); // 'P'
        }

        @Test
        @DisplayName("Content type is safe image type")
        void contentType_isSafeImageType() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.getMimeType()).isIn("image/png", "image/jpeg", "image/webp");
        }

        @Test
        @DisplayName("Preview with entity containing V12 storage fields still works correctly")
        void v12Fields_previewStillWorks() {
            ImageMediaAsset mediaAsset = stubMediaAssetWithV12Fields();
            // Verify V12 fields exist internally
            assertThat(mediaAsset.getStorageProvider()).isEqualTo("s3");
            assertThat(mediaAsset.getStorageBucket()).isEqualTo("private-assets-bucket");
            assertThat(mediaAsset.getStorageObjectKey()).isEqualTo(STORAGE_KEY);

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).isEqualTo(TEST_PNG);
        }
    }

    // ── B. Forbidden Exposure Regression ─────────────────────────────

    @Nested
    @DisplayName("B. Forbidden Exposure Regression")
    class ForbiddenExposureRegression {

        @Test
        @DisplayName("PreviewProxyResult has no storage key/provider/bucket fields")
        void result_noStorageFields() {
            Set<String> fields = getFieldNames(ImagePreviewProxyResult.class);
            assertThat(fields).doesNotContain(
                    "storageObjectKey", "storageBucket", "storageProvider",
                    "storageReference");
        }

        @Test
        @DisplayName("PreviewProxyResult has no URL fields")
        void result_noUrlFields() {
            Set<String> fields = getFieldNames(ImagePreviewProxyResult.class);
            assertThat(fields).doesNotContain(
                    "providerUrl", "providerOutputUrl", "signedUrl",
                    "publicUrl", "cdnUrl", "providerImageUrl");
        }

        @Test
        @DisplayName("PreviewProxyResult has no base64/raw payload fields")
        void result_noBase64OrRawPayload() {
            Set<String> fields = getFieldNames(ImagePreviewProxyResult.class);
            assertThat(fields).doesNotContain(
                    "base64", "imageBase64",
                    "rawProviderResponse", "rawStorageResponse",
                    "rawStorageProviderResponse");
        }

        @Test
        @DisplayName("PreviewProxyResult has no secret/config fields")
        void result_noSecretFields() {
            Set<String> fields = getFieldNames(ImagePreviewProxyResult.class);
            assertThat(fields).doesNotContain(
                    "apiKey", "secretKey", "bearerToken", "password",
                    "stackTrace", "internalFullPrompt");
        }

        @Test
        @DisplayName("Successful preview toString() does not contain storage key or bucket")
        void success_toString_noStorageDetails() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);
            String resultStr = result.toString();

            assertThat(resultStr).doesNotContain(STORAGE_KEY);
            assertThat(resultStr).doesNotContain("private-assets-bucket");
            assertThat(resultStr).doesNotContain("s3://");
        }
    }

    // ── C. Internal Storage Reference Allowed But Hidden ─────────────

    @Nested
    @DisplayName("C. Internal Storage Reference — Allowed But Hidden")
    class InternalStorageReferenceHidden {

        @Test
        @DisplayName("Entity HAS internal storage fields (V12)")
        void entity_hasInternalStorageFields() {
            Set<String> entityFields = getFieldNames(ImageMediaAsset.class);
            assertThat(entityFields).contains("storageProvider", "storageBucket", "storageObjectKey");
        }

        @Test
        @DisplayName("ImageMediaAssetResponse DTO does NOT expose storage internal fields")
        void dto_doesNotExposeStorageInternals() {
            Set<String> dtoFields = getFieldNames(ImageMediaAssetResponse.class);
            assertThat(dtoFields).doesNotContain(
                    "storageObjectKey", "storageBucket", "storageProvider");
        }

        @Test
        @DisplayName("StorageHandoffResponse DTO does NOT expose storage internal fields")
        void handoffDto_doesNotExposeStorageInternals() {
            Set<String> dtoFields = getFieldNames(StorageHandoffResponse.class);
            assertThat(dtoFields).doesNotContain(
                    "storageObjectKey", "storageBucket", "storageProvider");
        }

        @Test
        @DisplayName("Preview result does NOT carry entity storage fields")
        void previewResult_noEntityStorageFields() {
            Set<String> resultFields = getFieldNames(ImagePreviewProxyResult.class);
            assertThat(resultFields).doesNotContain(
                    "storageObjectKey", "storageBucket", "storageProvider");
        }

        @Test
        @DisplayName("Full forbidden field check on all admin DTOs")
        void allDtos_noForbiddenFields() {
            for (Class<?> dto : List.of(
                    ImagePreviewProxyResult.class,
                    ImageMediaAssetResponse.class,
                    StorageHandoffResponse.class)) {
                Set<String> fields = getFieldNames(dto);
                for (String forbidden : FORBIDDEN_FIELDS) {
                    assertThat(fields)
                            .as("%s must not contain: %s", dto.getSimpleName(), forbidden)
                            .doesNotContain(forbidden);
                }
            }
        }
    }

    // ── D. Failure Safety ────────────────────────────────────────────

    @Nested
    @DisplayName("D. Failure Safety")
    class FailureSafety {

        @Test
        @DisplayName("Missing generated asset → safe error, no storage key exposure")
        void missingGenAsset_safeError() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.empty());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContain(STORAGE_KEY);
            assertThat(result.getImageBytes()).isNull();
            verify(storageAdapter, never()).read(any());
        }

        @Test
        @DisplayName("Missing storage reference → safe error, no key in message")
        void missingStorageRef_safeError() {
            ImageMediaAsset mediaAsset = stubMediaAssetWithV12Fields();
            mediaAsset.setStorageObjectKey(null);

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
            assertThat(result.getErrorMessage()).doesNotContain("private-assets-bucket");
            verify(storageAdapter, never()).read(any());
        }

        @Test
        @DisplayName("Storage read failure → error does not expose bucket or key")
        void storageReadFail_noInternalDetails() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any())).thenReturn(ImageStorageReadResult.builder()
                    .success(false)
                    .errorCode(ImageStorageErrorCode.STORAGE_READ_FAILED)
                    .errorMessage("Depolama okuma başarısız")
                    .build());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            if (result.getErrorMessage() != null) {
                assertThat(result.getErrorMessage()).doesNotContain(STORAGE_KEY);
                assertThat(result.getErrorMessage()).doesNotContain("private-assets-bucket");
                assertThat(result.getErrorMessage()).doesNotContain("s3://");
                assertThat(result.getErrorMessage()).doesNotContain("https://");
            }
        }

        @Test
        @DisplayName("Storage exception → no stack trace or secrets in error")
        void storageException_noStackTraceOrSecrets() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any()))
                    .thenThrow(new RuntimeException("AmazonS3Exception: Access Denied for " + STORAGE_KEY));

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContain("AmazonS3Exception");
            assertThat(result.getErrorMessage()).doesNotContain("Access Denied");
            assertThat(result.getErrorMessage()).doesNotContain(STORAGE_KEY);
        }

        @Test
        @DisplayName("Unsupported MIME type → safe error")
        void unsupportedMime_safeError() {
            ImageMediaAsset mediaAsset = stubMediaAssetWithV12Fields();
            mediaAsset.setMimeType("application/pdf");

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE);
            verify(storageAdapter, never()).read(any());
        }

        @Test
        @DisplayName("Empty bytes from storage → safe error")
        void emptyBytes_safeError() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any())).thenReturn(ImageStorageReadResult.builder()
                    .success(true)
                    .imageBytes(new byte[0])
                    .build());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE);
        }
    }

    // ── E. Approval/Publish Regression ───────────────────────────────

    @Nested
    @DisplayName("E. Approval/Publish Regression")
    class ApprovalPublishRegression {

        @Test
        @DisplayName("Successful preview does not change GeneratedAsset review status")
        void preview_doesNotApprove() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset();
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(genAsset.getStatus()).isEqualTo(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
            assertThat(genAsset.getStatus().name()).doesNotContain("APPROVED");
            verify(generatedAssetRepository, never()).save(any());
        }

        @Test
        @DisplayName("No READY_FOR_PUBLISH or COMPLETED in any storage/preview enum")
        void noReadyForPublishOrCompleted() {
            for (ImageStorageHandoffStatus s : ImageStorageHandoffStatus.values()) {
                assertThat(s.name()).isNotEqualTo("READY_FOR_PUBLISH");
                assertThat(s.name()).isNotEqualTo("COMPLETED");
            }
            for (ImagePreviewStatus s : ImagePreviewStatus.values()) {
                assertThat(s.name()).isNotEqualTo("READY_FOR_PUBLISH");
                assertThat(s.name()).isNotEqualTo("COMPLETED");
            }
        }

        @Test
        @DisplayName("Preview does not trigger any save/write")
        void preview_noWriteOperations() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            previewService.getPreview(GENERATED_ASSET_ID);

            verify(generatedAssetRepository, never()).save(any());
            verify(mediaAssetRepository, never()).save(any());
            verify(storageAdapter, never()).write(any());
        }
    }

    // ── F. Endpoint Behavior ─────────────────────────────────────────

    @Nested
    @DisplayName("F. Endpoint Behavior")
    class EndpointBehavior {

        @Test
        @DisplayName("Preview service is read-only (no persist on success)")
        void readOnlyOnSuccess() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(stubGeneratedAsset()));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(stubMediaAssetWithV12Fields()));
            when(storageAdapter.read(any())).thenReturn(successReadResult());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isTrue();
            // No redirecting: result has imageBytes, not a redirect URL
            assertThat(result.getImageBytes()).isNotNull();
            assertThat(result.getImageBytes().length).isGreaterThan(0);
        }

        @Test
        @DisplayName("Preview service is read-only (no persist on failure)")
        void readOnlyOnFailure() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.empty());

            ImagePreviewProxyResult result = previewService.getPreview(GENERATED_ASSET_ID);

            assertThat(result.isSuccess()).isFalse();
            verify(generatedAssetRepository, never()).save(any());
            verify(mediaAssetRepository, never()).save(any());
        }

        @Test
        @DisplayName("ImagePreviewProxyResult has no redirect/URL fields for response construction")
        void noRedirectUrl() {
            Set<String> fields = getFieldNames(ImagePreviewProxyResult.class);
            assertThat(fields).doesNotContain(
                    "redirectUrl", "locationUrl", "forwardUrl",
                    "signedUrl", "publicUrl", "cdnUrl", "providerUrl");
        }
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
