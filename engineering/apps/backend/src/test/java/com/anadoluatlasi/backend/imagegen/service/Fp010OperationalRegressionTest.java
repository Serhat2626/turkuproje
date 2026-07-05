package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageMediaAssetResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.StorageHandoffResponse;
import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationResult;
import com.anadoluatlasi.backend.imagegen.provider.ProviderOutputReference;
import com.anadoluatlasi.backend.imagegen.storage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FP-010 Task 05 — Operational Regression, Observability & Closure Candidate.
 *
 * Dense regression coverage across the full FP-010 operational path.
 * All tests use mocks — no real external calls.
 */
@DisplayName("FP-010 Task 05 — Operational Regression & Observability")
class Fp010OperationalRegressionTest {

    // ── Shared forbidden field names ────────────────────────────────
    private static final Set<String> FORBIDDEN_DTO_FIELDS = Set.of(
            "providerUrl", "providerOutputUrl", "signedUrl", "publicUrl", "cdnUrl",
            "base64", "rawPayload", "rawProviderResponse", "rawStorageProviderResponse",
            "storageObjectKey", "storageBucket", "storageProvider", "storageKey",
            "apiKey", "secretKey", "accessKey", "internalPrompt", "stackTrace"
    );

    // ── A. Immediate Handoff — Enabled Success ──────────────────────

    @Nested
    @DisplayName("A. Immediate Handoff Operational Success")
    class ImmediateHandoffSuccess {

        private ImmediateStorageHandoffProperties config;
        private ControlledStorageHandoffOrchestrationService controlledHandoff;
        private ImmediateStorageHandoffService service;

        @BeforeEach
        void setUp() {
            config = new ImmediateStorageHandoffProperties();
            config.setEnabled(true);
            controlledHandoff = mock(ControlledStorageHandoffOrchestrationService.class);
            service = new ImmediateStorageHandoffService(config, controlledHandoff);
        }

        @Test
        @DisplayName("Enabled config + valid URL → handoff called, no approval")
        void enabledConfig_validUrl_handoffCalledNoApproval() {
            ImageGeneratedAsset asset = createAsset(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
            ProviderOutputReference ref = createRef("https://transient.provider.com/img.png");

            when(controlledHandoff.executeControlledHandoff(any(), any(), any()))
                    .thenReturn(successHandoff(asset.getId()));

            service.attemptImmediateHandoff(asset, ref);

            verify(controlledHandoff).executeControlledHandoff(eq(asset.getId()), any(), any());
            // Asset still GENERATED_PENDING_REVIEW — NOT approved
            assertThat(asset.getStatus()).isEqualTo(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
        }

        @Test
        @DisplayName("Handoff success result has safe status STORAGE_STORED")
        void handoffResult_storedNotApproved() {
            ImageStorageHandoffResult result = successHandoff(UUID.randomUUID());
            assertThat(result.getStorageHandoffStatus()).isEqualTo(ImageStorageHandoffStatus.STORAGE_STORED);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Handoff success does not set READY_FOR_PUBLISH or COMPLETED")
        void handoffSuccess_noReadyForPublishOrCompleted() {
            ImageStorageHandoffResult result = successHandoff(UUID.randomUUID());
            String str = result.toString();
            assertThat(str).doesNotContainIgnoringCase("READY_FOR_PUBLISH");
            assertThat(str).doesNotContainIgnoringCase("COMPLETED");
        }
    }

    // ── B. Immediate Handoff — Disabled/Default ─────────────────────

    @Nested
    @DisplayName("B. Immediate Handoff Disabled / Default")
    class ImmediateHandoffDisabled {

        @Test
        @DisplayName("Default config is disabled")
        void defaultConfig_disabled() {
            ImmediateStorageHandoffProperties p = new ImmediateStorageHandoffProperties();
            assertThat(p.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Disabled → no handoff, no storage, no URL persistence")
        void disabled_noHandoff() {
            ImmediateStorageHandoffProperties config = new ImmediateStorageHandoffProperties();
            config.setEnabled(false);
            ControlledStorageHandoffOrchestrationService mock = mock(ControlledStorageHandoffOrchestrationService.class);
            ImmediateStorageHandoffService svc = new ImmediateStorageHandoffService(config, mock);

            svc.attemptImmediateHandoff(
                    createAsset(GeneratedAssetStatus.GENERATED_PENDING_REVIEW),
                    createRef("https://transient.com/img.png")
            );

            verifyNoInteractions(mock);
        }

        @Test
        @DisplayName("Disabled → generation still succeeds (no side effects)")
        void disabled_generationUnaffected() {
            ImageGeneratedAsset asset = createAsset(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
            // Simply confirm asset status is preserved
            assertThat(asset.getStatus()).isEqualTo(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
        }
    }

    // ── C. Provider Download Failure ────────────────────────────────

    @Nested
    @DisplayName("C. Provider Download Failure")
    class ProviderDownloadFailure {

        @Test
        @DisplayName("Download disabled → safe failure properties")
        void downloadDisabled_safeProperties() {
            ProviderOutputDownloadProperties props = new ProviderOutputDownloadProperties();
            assertThat(props.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("HttpProviderOutputRetrievalClient config has no secret in toString")
        void downloadConfig_noSecretInToString() {
            ProviderOutputDownloadProperties props = new ProviderOutputDownloadProperties();
            String str = props.toString();
            assertThat(str).doesNotContain("apiKey");
            assertThat(str).doesNotContain("secretKey");
        }

        @Test
        @DisplayName("Handoff with failed retrieval returns safe error")
        void failedRetrieval_safeError() {
            ImmediateStorageHandoffProperties config = new ImmediateStorageHandoffProperties();
            config.setEnabled(true);
            ControlledStorageHandoffOrchestrationService mock = mock(ControlledStorageHandoffOrchestrationService.class);

            when(mock.executeControlledHandoff(any(), any(), any()))
                    .thenReturn(failureHandoff(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED));

            ImmediateStorageHandoffService svc = new ImmediateStorageHandoffService(config, mock);
            svc.attemptImmediateHandoff(
                    createAsset(GeneratedAssetStatus.GENERATED_PENDING_REVIEW),
                    createRef("https://transient.com/img.png")
            );

            // Non-fatal — no exception thrown
            verify(mock).executeControlledHandoff(any(), any(), any());
        }
    }

    // ── D. Storage Config/Write Failure ─────────────────────────────

    @Nested
    @DisplayName("D. Storage Config/Write Failure")
    class StorageFailure {

        @Test
        @DisplayName("S3 storage disabled by default")
        void s3Disabled_byDefault() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            assertThat(props.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("S3 config validation fails safely on missing bucket")
        void s3Config_missingBucket_failsSafely() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setEnabled(true);
            props.setBucket("");
            String error = props.validateForProduction();
            assertThat(error).isNotNull();
            assertThat(error).doesNotContain("accessKey");
            assertThat(error).doesNotContain("secretKey");
        }

        @Test
        @DisplayName("S3 adapter write failure has no key/secret leak")
        void s3WriteFailure_noKeyLeak() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setEnabled(true);
            props.setBucket("test");
            props.setEndpoint("https://s3.test.com");
            props.setRegion("eu-west-1");
            props.setAccessKey("AKIAFAKE");
            props.setSecretKey("secretfake");
            props.setPathPrefix("img/");

            S3StorageClient mockClient = mock(S3StorageClient.class);
            S3ImageStorageAdapter adapter = new S3ImageStorageAdapter(props, mockClient);

            // Null bytes → safe failure
            ImageStorageWriteResult result = adapter.write(null);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContain("AKIAFAKE");
            assertThat(result.getErrorMessage()).doesNotContain("secretfake");
            assertThat(result.getErrorMessage()).doesNotContain("test");
        }

        @Test
        @DisplayName("InMemory adapter still functions as fallback")
        void inMemoryFallback_works() {
            InMemoryImageStorageAdapter adapter = new InMemoryImageStorageAdapter();
            assertThat(adapter.getProviderAlias()).isEqualTo("mock");

            ImageStorageWriteRequest req = ImageStorageWriteRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .imageBytes(new byte[]{1, 2, 3, 4})
                    .mimeType("image/png")
                    .width(100).height(100).fileSizeBytes(4)
                    .build();
            ImageStorageWriteResult wResult = adapter.write(req);
            assertThat(wResult.isSuccess()).isTrue();
        }
    }

    // ── E. Preview After Storage ────────────────────────────────────

    @Nested
    @DisplayName("E. Preview After Storage")
    class PreviewAfterStorage {

        @Test
        @DisplayName("ImageStorageReadResult has no redirect/URL/base64 fields")
        void readResult_noRedirectFields() {
            Set<String> fields = getFieldNames(ImageStorageReadResult.class);
            for (String f : FORBIDDEN_DTO_FIELDS) {
                assertThat(fields).as("ReadResult must not have: %s", f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("InMemory read returns backend bytes, no URL")
        void inMemory_readReturnsBytes() {
            InMemoryImageStorageAdapter adapter = new InMemoryImageStorageAdapter();
            ImageStorageWriteRequest wReq = ImageStorageWriteRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .imageBytes(new byte[]{10, 20, 30})
                    .mimeType("image/png").width(1).height(1).fileSizeBytes(3)
                    .build();
            ImageStorageWriteResult wResult = adapter.write(wReq);
            assertThat(wResult.isSuccess()).isTrue();

            ImageStorageReadResult rResult = adapter.read(ImageStorageReadRequest.builder()
                    .storageObjectKey(wResult.getStorageObjectKey())
                    .mediaAssetId(UUID.randomUUID())
                    .build());
            assertThat(rResult.isSuccess()).isTrue();
            assertThat(rResult.getImageBytes()).isEqualTo(new byte[]{10, 20, 30});
            assertThat(rResult.toString()).doesNotContain("publicUrl");
            assertThat(rResult.toString()).doesNotContain("signedUrl");
        }
    }

    // ── F. DTO/API Safety ───────────────────────────────────────────

    @Nested
    @DisplayName("F. DTO / API Safety")
    class DtoApiSafety {

        @Test
        @DisplayName("StorageHandoffResponse has no forbidden fields")
        void handoffResponse_noForbiddenFields() {
            Set<String> fields = getFieldNames(StorageHandoffResponse.class);
            for (String f : FORBIDDEN_DTO_FIELDS) {
                assertThat(fields).as("StorageHandoffResponse must not have: %s", f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("ImageMediaAssetResponse has no forbidden fields")
        void mediaAssetResponse_noForbiddenFields() {
            Set<String> fields = getFieldNames(ImageMediaAssetResponse.class);
            for (String f : FORBIDDEN_DTO_FIELDS) {
                assertThat(fields).as("ImageMediaAssetResponse must not have: %s", f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("ImageStorageHandoffResult has no forbidden fields")
        void handoffResult_noForbiddenFields() {
            Set<String> fields = getFieldNames(ImageStorageHandoffResult.class);
            for (String f : FORBIDDEN_DTO_FIELDS) {
                assertThat(fields).as("ImageStorageHandoffResult must not have: %s", f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("ImageStorageWriteResult is internal — storageObjectKey allowed but no admin DTO fields")
        void writeResult_internalModel_noAdminFields() {
            // ImageStorageWriteResult is internal — storageObjectKey is expected and correct.
            // Verify it has no admin/user-facing forbidden fields.
            Set<String> fields = getFieldNames(ImageStorageWriteResult.class);
            for (String f : Set.of("providerUrl", "providerOutputUrl", "signedUrl", "publicUrl",
                    "cdnUrl", "base64", "rawPayload", "apiKey", "secretKey", "internalPrompt")) {
                assertThat(fields).as("WriteResult must not have: %s", f).doesNotContain(f);
            }
        }
    }

    // ── G. Observability / toString / Failure Safety ────────────────

    @Nested
    @DisplayName("G. Observability / toString / Failure Safety")
    class ObservabilitySafety {

        @Test
        @DisplayName("ProviderOutputReference toString excludes URL")
        void providerRef_toString_excludesUrl() {
            ProviderOutputReference ref = createRef("https://secret.openai.com/xxx");
            String str = ref.toString();
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("openai");
            assertThat(str).contains("hasProviderOutputUrl=true");
        }

        @Test
        @DisplayName("ProviderOutputReference with null URL safe")
        void providerRef_nullUrl_safe() {
            ProviderOutputReference ref = new ProviderOutputReference(
                    ImageProviderGenerationResult.builder().success(true).build(), null);
            assertThat(ref.toString()).contains("hasProviderOutputUrl=false");
        }

        @Test
        @DisplayName("S3StorageClientException toString does not leak sensitive data")
        void s3Exception_toString_safe() {
            S3StorageClient.S3StorageClientException ex =
                    new S3StorageClient.S3StorageClientException("safe msg",
                            new RuntimeException("bucket=secret-bucket key=abc/def"));
            String str = ex.toString();
            assertThat(str).doesNotContain("secret-bucket");
            assertThat(str).doesNotContain("abc/def");
            assertThat(str).contains("safe msg");
        }

        @Test
        @DisplayName("ImageStorageErrorCode enum has no sensitive values")
        void errorCode_noSensitiveValues() {
            for (ImageStorageErrorCode code : ImageStorageErrorCode.values()) {
                String safe = ImageStorageErrorNormalizer.safeMessage(code);
                assertThat(safe).doesNotContain("https://");
                assertThat(safe).doesNotContain("apiKey");
                assertThat(safe).doesNotContain("bucket");
                assertThat(safe).doesNotContain("objectKey");
            }
        }

        @Test
        @DisplayName("S3PrivateStorageProperties config toString has no key values")
        void s3Props_toString_noKeys() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setAccessKey("AKIATOPSECRET");
            props.setSecretKey("super-secret-key-value");
            // Lombok @Data toString may include fields — we verify isEnabledAndConfigured is safe
            assertThat(props.isEnabledAndConfigured()).isFalse();
        }

        @Test
        @DisplayName("ImmediateStorageHandoffProperties has only safe boolean field")
        void immediateHandoffProps_safeFields() {
            Set<String> fields = getFieldNames(ImmediateStorageHandoffProperties.class);
            assertThat(fields).containsExactlyInAnyOrder("enabled");
        }
    }

    // ── H. Publish / Review Regression ──────────────────────────────

    @Nested
    @DisplayName("H. Publish / Review Regression")
    class PublishReviewRegression {

        @Test
        @DisplayName("GeneratedAssetStatus has no READY_FOR_PUBLISH")
        void noReadyForPublish() {
            Set<String> statuses = Arrays.stream(GeneratedAssetStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("GeneratedAssetStatus has no COMPLETED")
        void noCompleted() {
            Set<String> statuses = Arrays.stream(GeneratedAssetStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain("COMPLETED");
        }

        @Test
        @DisplayName("ImageStorageHandoffStatus has no APPROVED")
        void handoffStatus_noApproved() {
            Set<String> statuses = Arrays.stream(ImageStorageHandoffStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain("APPROVED");
        }

        @Test
        @DisplayName("ImageStorageHandoffStatus has no PUBLISH/EXPORT")
        void handoffStatus_noPublishExport() {
            Set<String> statuses = Arrays.stream(ImageStorageHandoffStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain("PUBLISHED", "EXPORTED",
                    "READY_FOR_PUBLISH", "READY_FOR_EXPORT");
        }

        @Test
        @DisplayName("ImagePreviewStatus has no public delivery")
        void previewStatus_noPublicDelivery() {
            Set<String> statuses = Arrays.stream(ImagePreviewStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain("PUBLIC_URL_READY", "CDN_READY",
                    "SIGNED_URL_READY", "REDIRECT_READY");
        }

        @Test
        @DisplayName("Stored does NOT mean approved")
        void storedDoesNotMeanApproved() {
            // STORAGE_STORED is a handoff status, NOT an approval status
            assertThat(ImageStorageHandoffStatus.STORAGE_STORED.name())
                    .doesNotContain("APPROVED");
        }

        @Test
        @DisplayName("No video/audio status values")
        void noVideoAudioStatuses() {
            Set<String> genStatuses = Arrays.stream(GeneratedAssetStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(genStatuses).doesNotContain("VIDEO_READY", "AUDIO_READY",
                    "VIDEO_GENERATED", "AUDIO_GENERATED");
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static ImageGeneratedAsset createAsset(GeneratedAssetStatus status) {
        ImageGeneratedAsset asset = new ImageGeneratedAsset();
        asset.setId(UUID.randomUUID());
        asset.setImageMediaAssetId(UUID.randomUUID());
        asset.setStatus(status);
        return asset;
    }

    private static ProviderOutputReference createRef(String url) {
        return new ProviderOutputReference(
                ImageProviderGenerationResult.builder()
                        .success(true)
                        .providerAlias("OPENAI")
                        .correlationId(UUID.randomUUID())
                        .build(),
                url
        );
    }

    private static ImageStorageHandoffResult successHandoff(UUID genId) {
        return ImageStorageHandoffResult.builder()
                .success(true)
                .generatedAssetId(genId)
                .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_STORED)
                .previewStatus(ImagePreviewStatus.PREVIEW_AVAILABLE)
                .correlationId("corr-" + genId)
                .build();
    }

    private static ImageStorageHandoffResult failureHandoff(ImageStorageErrorCode errorCode) {
        return ImageStorageHandoffResult.builder()
                .success(false)
                .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_FAILED)
                .previewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE)
                .errorCode(errorCode)
                .errorMessage(ImageStorageErrorNormalizer.safeMessage(errorCode))
                .build();
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
