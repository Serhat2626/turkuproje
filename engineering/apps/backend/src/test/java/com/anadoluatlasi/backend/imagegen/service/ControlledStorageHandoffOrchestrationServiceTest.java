package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.StorageHandoffResponse;
import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.ImageGeneratedAssetRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageAdapter;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageWriteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-009 Task 09 — Controlled Storage Handoff Activation Tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-009 Task 09 — Controlled Storage Handoff Orchestration")
class ControlledStorageHandoffOrchestrationServiceTest {

    private static final UUID GENERATED_ASSET_ID = UUID.randomUUID();
    private static final UUID MEDIA_ASSET_ID = UUID.randomUUID();
    private static final String CORRELATION_ID = "test-corr-" + UUID.randomUUID();
    private static final String PROVIDER_URL = "https://oai.example.com/transient-img-999.png";

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "storageObjectKey", "storageReference", "storageBucket", "storageProvider",
            "providerUrl", "providerOutputUrl", "signedUrl", "publicUrl", "cdnUrl",
            "base64", "imageBase64", "rawProviderResponse", "rawStorageResponse",
            "apiKey", "secretKey", "bearerToken", "password", "stackTrace"
    );

    @Mock private ImageGeneratedAssetRepository generatedAssetRepository;
    @Mock private ImageMediaAssetRepository mediaAssetRepository;
    @Mock private ProviderOutputRetrievalClient retrievalClient;
    @Mock private ImageMediaValidationService validationService;
    @Mock private ImageStorageAdapter storageAdapter;

    private ControlledStorageHandoffOrchestrationService orchestrationService;
    private ImageStorageHandoffService storageHandoffService;

    @BeforeEach
    void setUp() {
        storageHandoffService = new ImageStorageHandoffService(
                mediaAssetRepository, validationService, storageAdapter);

        ProviderOutputRetrievalValidationService rvService =
                new ProviderOutputRetrievalValidationService(retrievalClient, validationService);

        orchestrationService = new ControlledStorageHandoffOrchestrationService(
                generatedAssetRepository, mediaAssetRepository,
                rvService, storageHandoffService);
    }

    private ImageGeneratedAsset stubGeneratedAsset(UUID mediaAssetId) {
        return ImageGeneratedAsset.builder()
                .imageGenerationRequestId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .visualAssetPlanId(UUID.randomUUID())
                .visualAssetItemId(UUID.randomUUID())
                .status(GeneratedAssetStatus.GENERATED_PENDING_REVIEW)
                .imageMediaAssetId(mediaAssetId)
                .build();
    }

    private ImageMediaAsset stubMediaAsset() {
        ImageMediaAsset asset = new ImageMediaAsset();
        asset.setAssetKind(ImageMediaAssetKind.GENERATED_IMAGE);
        asset.setStorageHandoffStatus(ImageStorageHandoffStatus.STORAGE_PENDING);
        asset.setPreviewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE);
        return asset;
    }

    // Valid 1x1 PNG for tests
    private byte[] validPng() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00
        };
    }

    // ── A. Successful Controlled Handoff ─────────────────────────────

    @Nested
    @DisplayName("A. Successful Handoff")
    class SuccessfulHandoff {

        @Test
        @DisplayName("Full success: retrieval → validation → storage")
        void fullSuccess() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            ImageMediaAsset mediaAsset = stubMediaAsset();

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));

            byte[] png = validPng();

            // Mock retrieval success
            when(retrievalClient.retrieve(eq(PROVIDER_URL), any()))
                    .thenReturn(ProviderOutputRetrievalResult.builder()
                            .success(true)
                            .imageBytes(png)
                            .contentLength(png.length)
                            .detectedContentType("image/png")
                            .correlationId(CORRELATION_ID)
                            .build());

            // Mock validation success (called TWICE: once in RV service, once in handoff)
            ImageMediaValidationResult validResult = ImageMediaValidationResult.builder()
                    .valid(true)
                    .detectedMimeType("image/png")
                    .fileSizeBytes(png.length)
                    .width(1)
                    .height(1)
                    .checksumSha256("abc123")
                    .build();
            when(validationService.validate(any(byte[].class), anyString()))
                    .thenReturn(validResult);

            // Mock storage write success
            when(storageAdapter.write(any()))
                    .thenReturn(ImageStorageWriteResult.builder()
                            .success(true)
                            .storageProviderAlias("mock")
                            .storageObjectKey("imagegen/test-key")
                            .mimeType("image/png")
                            .fileSizeBytes(png.length)
                            .storedAt(LocalDateTime.now())
                            .build());

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, PROVIDER_URL, CORRELATION_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_STORED);
            assertThat(result.getPreviewStatus()).isEqualTo(ImagePreviewStatus.PREVIEW_AVAILABLE);
            assertThat(result.getMediaAssetId()).isEqualTo(MEDIA_ASSET_ID);
        }

        @Test
        @DisplayName("Success result contains no forbidden data")
        void successResult_noForbiddenData() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            ImageMediaAsset mediaAsset = stubMediaAsset();

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));

            byte[] png = validPng();
            when(retrievalClient.retrieve(anyString(), any()))
                    .thenReturn(ProviderOutputRetrievalResult.builder()
                            .success(true).imageBytes(png)
                            .contentLength(png.length).correlationId(CORRELATION_ID).build());

            when(validationService.validate(any(byte[].class), anyString()))
                    .thenReturn(ImageMediaValidationResult.builder()
                            .valid(true).detectedMimeType("image/png")
                            .fileSizeBytes(png.length).width(1).height(1).checksumSha256("abc").build());

            when(storageAdapter.write(any()))
                    .thenReturn(ImageStorageWriteResult.builder()
                            .success(true).storageProviderAlias("mock")
                            .storageObjectKey("imagegen/secret-key-123")
                            .storedAt(LocalDateTime.now()).build());

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, PROVIDER_URL, CORRELATION_ID);

            String resultStr = result.toString();
            assertThat(resultStr).doesNotContain("oai.example.com");
            assertThat(resultStr).doesNotContain("transient-img");
            assertThat(resultStr).doesNotContain("secret-key-123");
            assertThat(resultStr).doesNotContain("imagegen/secret");
        }

        @Test
        @DisplayName("Stored does NOT mean approved — review status unchanged")
        void stored_doesNotMeanApproved() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            ImageMediaAsset mediaAsset = stubMediaAsset();

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));

            byte[] png = validPng();
            when(retrievalClient.retrieve(anyString(), any()))
                    .thenReturn(ProviderOutputRetrievalResult.builder()
                            .success(true).imageBytes(png).contentLength(png.length)
                            .correlationId(CORRELATION_ID).build());
            when(validationService.validate(any(byte[].class), anyString()))
                    .thenReturn(ImageMediaValidationResult.builder()
                            .valid(true).detectedMimeType("image/png")
                            .fileSizeBytes(png.length).width(1).height(1).checksumSha256("x").build());
            when(storageAdapter.write(any()))
                    .thenReturn(ImageStorageWriteResult.builder()
                            .success(true).storageProviderAlias("mock")
                            .storageObjectKey("k").storedAt(LocalDateTime.now()).build());

            orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, PROVIDER_URL, CORRELATION_ID);

            // GeneratedAsset status should remain GENERATED_PENDING_REVIEW
            assertThat(genAsset.getStatus()).isEqualTo(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
        }
    }

    // ── B. Retrieval Failure ────────────────────────────────────────

    @Nested
    @DisplayName("B. Retrieval Failure")
    class RetrievalFailure {

        @Test
        @DisplayName("Null provider URL → PROVIDER_OUTPUT_UNAVAILABLE")
        void nullUrl_providerUnavailable() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, null, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
            verify(storageAdapter, never()).write(any());
        }

        @Test
        @DisplayName("Blank provider URL → PROVIDER_OUTPUT_UNAVAILABLE")
        void blankUrl_providerUnavailable() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, "   ", CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
        }

        @Test
        @DisplayName("Retrieval client failure → safe error, storage not called")
        void retrievalFails_storageNotCalled() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            when(retrievalClient.retrieve(anyString(), any()))
                    .thenReturn(ProviderOutputRetrievalResult.builder()
                            .success(false)
                            .errorCode(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED)
                            .errorMessage("Download failed")
                            .correlationId(CORRELATION_ID)
                            .build());

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, PROVIDER_URL, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
            verify(storageAdapter, never()).write(any());
        }

        @Test
        @DisplayName("Retrieval failure error message does not contain provider URL")
        void retrievalFailure_noUrlExposure() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            when(retrievalClient.retrieve(anyString(), any()))
                    .thenReturn(ProviderOutputRetrievalResult.builder()
                            .success(false)
                            .errorCode(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED)
                            .errorMessage("Safe message")
                            .correlationId(CORRELATION_ID)
                            .build());

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, PROVIDER_URL, CORRELATION_ID);

            assertThat(result.getErrorMessage()).doesNotContain("oai.example.com");
            assertThat(result.getErrorMessage()).doesNotContain("transient-img");
        }
    }

    // ── C. Validation Failure ───────────────────────────────────────

    @Nested
    @DisplayName("C. Validation Failure")
    class ValidationFailure {

        @Test
        @DisplayName("Validation fails → storage not called")
        void validationFails_storageNotCalled() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            byte[] invalidBytes = new byte[]{0x00, 0x01, 0x02};
            when(retrievalClient.retrieve(anyString(), any()))
                    .thenReturn(ProviderOutputRetrievalResult.builder()
                            .success(true).imageBytes(invalidBytes)
                            .contentLength(3).correlationId(CORRELATION_ID).build());
            when(validationService.validate(any(byte[].class), anyString()))
                    .thenReturn(ImageMediaValidationResult.builder()
                            .valid(false)
                            .errorCode(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE)
                            .errorMessage("Unsupported type")
                            .build());

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, PROVIDER_URL, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE);
            verify(storageAdapter, never()).write(any());
        }
    }

    // ── D. Storage Failure ──────────────────────────────────────────

    @Nested
    @DisplayName("D. Storage Failure")
    class StorageFailure {

        @Test
        @DisplayName("Storage write fails → safe failure, no public URL exposed")
        void storageFails_safeFailure() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            ImageMediaAsset mediaAsset = stubMediaAsset();

            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));
            when(mediaAssetRepository.findById(MEDIA_ASSET_ID))
                    .thenReturn(Optional.of(mediaAsset));

            byte[] png = validPng();
            when(retrievalClient.retrieve(anyString(), any()))
                    .thenReturn(ProviderOutputRetrievalResult.builder()
                            .success(true).imageBytes(png).contentLength(png.length)
                            .correlationId(CORRELATION_ID).build());
            when(validationService.validate(any(byte[].class), anyString()))
                    .thenReturn(ImageMediaValidationResult.builder()
                            .valid(true).detectedMimeType("image/png")
                            .fileSizeBytes(png.length).width(1).height(1).checksumSha256("x").build());
            when(storageAdapter.write(any()))
                    .thenReturn(ImageStorageWriteResult.builder()
                            .success(false)
                            .errorCode(ImageStorageErrorCode.STORAGE_WRITE_FAILED)
                            .errorMessage("Storage write failed")
                            .build());

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, PROVIDER_URL, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_FAILED);
            assertThat(result.getPreviewStatus()).isEqualTo(ImagePreviewStatus.PREVIEW_UNAVAILABLE);
        }
    }

    // ── E. Endpoint Safety ──────────────────────────────────────────

    @Nested
    @DisplayName("E. Endpoint Safety")
    class EndpointSafety {

        @Test
        @DisplayName("Generated asset not found → safe failure")
        void assetNotFound() {
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.empty());

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, PROVIDER_URL, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.UNKNOWN_STORAGE_HANDOFF_FAILURE);
        }

        @Test
        @DisplayName("Creates media asset if not linked")
        void createsMediaAsset_ifNotLinked() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(null); // no media asset
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            ImageMediaAsset newAsset = stubMediaAsset();
            newAsset.setId(UUID.randomUUID());
            when(mediaAssetRepository.save(any(ImageMediaAsset.class)))
                    .thenReturn(newAsset);

            // Will fail at provider URL missing, but media asset creation should happen
            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, null, CORRELATION_ID);

            verify(mediaAssetRepository).save(any(ImageMediaAsset.class));
            assertThat(genAsset.getImageMediaAssetId()).isNotNull();
        }
    }

    // ── F. DTO/Forbidden Field Regression ───────────────────────────

    @Nested
    @DisplayName("F. DTO/Forbidden Field Regression")
    class DtoRegression {

        @Test
        @DisplayName("StorageHandoffResponse has no forbidden fields")
        void response_noForbiddenFields() {
            Set<String> fields = getFieldNames(StorageHandoffResponse.class);
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("Response must not contain: %s", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("ImageStorageHandoffResult has no forbidden fields")
        void result_noForbiddenFields() {
            Set<String> fields = getFieldNames(ImageStorageHandoffResult.class);
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("ControlledStorageHandoffOrchestrationService has no URL/key fields")
        void service_noUrlFields() {
            Set<String> fields = getFieldNames(ControlledStorageHandoffOrchestrationService.class);
            assertThat(fields).doesNotContain(
                    "providerUrl", "providerOutputUrl", "storageObjectKey",
                    "signedUrl", "publicUrl", "base64", "apiKey", "secretKey");
        }
    }

    // ── G. Review/Publish Regression ────────────────────────────────

    @Nested
    @DisplayName("G. No Review/Publish Side Effects")
    class NoSideEffects {

        @Test
        @DisplayName("No READY_FOR_PUBLISH in any status enum used")
        void noReadyForPublish() {
            Set<String> handoffValues = Arrays.stream(ImageStorageHandoffStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(handoffValues).doesNotContain("READY_FOR_PUBLISH", "COMPLETED");

            Set<String> previewValues = Arrays.stream(ImagePreviewStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(previewValues).doesNotContain("READY_FOR_PUBLISH", "COMPLETED");
        }

        @Test
        @DisplayName("Endpoint-triggered handoff with null URL returns safe failure, no storage call")
        void endpointTriggered_nullUrl_noStorage() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, null, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            verify(storageAdapter, never()).write(any());
            assertThat(result.toString()).doesNotContain("APPROVED");
            assertThat(result.toString()).doesNotContain("READY_FOR_PUBLISH");
        }
    }

    // ── H. Logging/Sanitization ─────────────────────────────────────

    @Nested
    @DisplayName("H. Error Sanitization")
    class ErrorSanitization {

        @Test
        @DisplayName("Error messages do not contain provider URL")
        void errors_noUrlLeakage() {
            ImageGeneratedAsset genAsset = stubGeneratedAsset(MEDIA_ASSET_ID);
            when(generatedAssetRepository.findById(GENERATED_ASSET_ID))
                    .thenReturn(Optional.of(genAsset));

            ImageStorageHandoffResult result = orchestrationService.executeControlledHandoff(
                    GENERATED_ASSET_ID, null, CORRELATION_ID);

            if (result.getErrorMessage() != null) {
                assertThat(result.getErrorMessage()).doesNotContain("https://");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("apiKey");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("secret");
            }
        }
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
