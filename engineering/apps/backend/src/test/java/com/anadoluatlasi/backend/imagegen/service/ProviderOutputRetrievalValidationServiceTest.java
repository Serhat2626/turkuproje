package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-009 Task 06 — Mock Retrieval + Validation-First Orchestration Tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-009 Retrieval → Validation Orchestration Tests")
class ProviderOutputRetrievalValidationServiceTest {

    private static final UUID MEDIA_ASSET_ID = UUID.randomUUID();
    private static final UUID GENERATED_ASSET_ID = UUID.randomUUID();
    private static final String PROVIDER_ALIAS = "mock";
    private static final String CORRELATION_ID = "test-corr-orchestration";
    private static final String MOCK_PROVIDER_URL = "https://mock-provider.test/image/123";

    /** Minimal valid PNG bytes for testing. */
    private static final byte[] VALID_PNG_BYTES = MockProviderOutputRetrievalClient.MOCK_PNG_BYTES;

    /** Invalid/corrupt bytes. */
    private static final byte[] CORRUPT_BYTES = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};

    private static final Set<String> FORBIDDEN_FIELD_NAMES = Set.of(
            "providerUrl", "providerOutputUrl", "providerImageUrl",
            "providerDownloadUrl", "signedUrl", "publicUrl", "cdnUrl",
            "storageObjectKey", "storageKey", "storageBucket", "storageProvider",
            "base64", "imageBase64", "rawProviderResponse", "rawStorageResponse",
            "apiKey", "secretKey", "bearerToken", "password", "stackTrace"
    );

    @Mock
    private ProviderOutputRetrievalClient retrievalClient;

    @Mock
    private ImageMediaValidationService validationService;

    private ProviderOutputRetrievalValidationService orchestrationService;

    @BeforeEach
    void setUp() {
        orchestrationService = new ProviderOutputRetrievalValidationService(
                retrievalClient, validationService);
    }

    // --- A. Retrieval Success + Validation Success ---

    @Nested
    @DisplayName("A. Retrieval Success + Validation Success")
    class RetrievalAndValidationSuccess {

        @Test
        @DisplayName("Returns success with validated metadata when both pass")
        void execute_bothPass_returnsSuccess() {
            stubRetrievalSuccess(VALID_PNG_BYTES, "image/png");
            stubValidationSuccess("image/png", VALID_PNG_BYTES.length, 1, 1, "sha256-mock");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValidatedImageBytes()).isEqualTo(VALID_PNG_BYTES);
            assertThat(result.getDetectedMimeType()).isEqualTo("image/png");
            assertThat(result.getFileSizeBytes()).isEqualTo(VALID_PNG_BYTES.length);
            assertThat(result.getWidth()).isEqualTo(1);
            assertThat(result.getHeight()).isEqualTo(1);
            assertThat(result.getChecksumSha256()).isEqualTo("sha256-mock");
            assertThat(result.getCorrelationId()).isEqualTo(CORRELATION_ID);
            assertThat(result.getFailurePhase()).isNull();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Calls retrieval client then validation service in order")
        void execute_bothPass_callsInOrder() {
            stubRetrievalSuccess(VALID_PNG_BYTES, "image/png");
            stubValidationSuccess("image/png", VALID_PNG_BYTES.length, 1, 1, "sha256-mock");

            orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            var inOrder = inOrder(retrievalClient, validationService);
            inOrder.verify(retrievalClient).retrieve(eq(MOCK_PROVIDER_URL), any());
            inOrder.verify(validationService).validate(eq(VALID_PNG_BYTES), eq(CORRELATION_ID));
        }

        @Test
        @DisplayName("Success result does not contain provider URL")
        void execute_successResult_noProviderUrl() {
            stubRetrievalSuccess(VALID_PNG_BYTES, "image/png");
            stubValidationSuccess("image/png", VALID_PNG_BYTES.length, 1, 1, "sha256-mock");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            String resultString = result.toString();
            assertThat(resultString).doesNotContain("mock-provider.test");
            assertThat(resultString).doesNotContain(MOCK_PROVIDER_URL);
        }
    }

    // --- B. Retrieval Failure ---

    @Nested
    @DisplayName("B. Retrieval Failure")
    class RetrievalFailure {

        @Test
        @DisplayName("Retrieval failure returns safe error without calling validation")
        void execute_retrievalFails_validationNotCalled() {
            stubRetrievalFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED,
                    "Sağlayıcı çıktısı indirilemedi", true);

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailurePhase())
                    .isEqualTo(RetrievalValidationResult.FailurePhase.RETRIEVAL);
            assertThat(result.getErrorCode())
                    .isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
            assertThat(result.getErrorMessage()).doesNotContain("http");
            assertThat(result.isRetryable()).isTrue();
            assertThat(result.getCorrelationId()).isEqualTo(CORRELATION_ID);

            verify(validationService, never()).validate(any(), any());
        }

        @Test
        @DisplayName("Provider unavailable returns safe failure")
        void execute_providerUnavailable_safeFailure() {
            stubRetrievalFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE,
                    "Sağlayıcı çıktısı mevcut değil", false);

            RetrievalValidationResult result = orchestrationService.execute(
                    null, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailurePhase())
                    .isEqualTo(RetrievalValidationResult.FailurePhase.RETRIEVAL);
            assertThat(result.getErrorCode())
                    .isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
            verify(validationService, never()).validate(any(), any());
        }

        @Test
        @DisplayName("Provider expired returns safe failure")
        void execute_providerExpired_safeFailure() {
            stubRetrievalFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_EXPIRED,
                    "Sağlayıcı çıktısı süresi dolmuş", false);

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode())
                    .isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_EXPIRED);
        }
    }

    // --- C. Validation Failure ---

    @Nested
    @DisplayName("C. Validation Failure")
    class ValidationFailure {

        @Test
        @DisplayName("Unsupported media type returns validation failure")
        void execute_unsupportedType_validationFailure() {
            stubRetrievalSuccess(CORRUPT_BYTES, "application/octet-stream");
            stubValidationFailure(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    "Desteklenmeyen medya türü");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailurePhase())
                    .isEqualTo(RetrievalValidationResult.FailurePhase.VALIDATION);
            assertThat(result.getErrorCode())
                    .isEqualTo(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE);
            assertThat(result.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("Media too large returns validation failure")
        void execute_mediaTooLarge_validationFailure() {
            stubRetrievalSuccess(new byte[100], "image/png");
            stubValidationFailure(ImageStorageErrorCode.MEDIA_TOO_LARGE,
                    "Medya dosyası çok büyük");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailurePhase())
                    .isEqualTo(RetrievalValidationResult.FailurePhase.VALIDATION);
            assertThat(result.getErrorCode())
                    .isEqualTo(ImageStorageErrorCode.MEDIA_TOO_LARGE);
        }

        @Test
        @DisplayName("General validation failure returns safe error")
        void execute_generalValidationFailure() {
            stubRetrievalSuccess(CORRUPT_BYTES, "image/png");
            stubValidationFailure(ImageStorageErrorCode.MEDIA_VALIDATION_FAILED,
                    "Medya doğrulaması başarısız");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode())
                    .isEqualTo(ImageStorageErrorCode.MEDIA_VALIDATION_FAILED);
            assertThat(result.getValidatedImageBytes()).isNull();
        }

        @Test
        @DisplayName("Validation failure does not produce validated bytes")
        void execute_validationFails_noBytesInResult() {
            stubRetrievalSuccess(VALID_PNG_BYTES, "image/png");
            stubValidationFailure(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    "Desteklenmeyen medya türü");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getValidatedImageBytes()).isNull();
        }
    }

    // --- D. Corrupt/Invalid Bytes ---

    @Nested
    @DisplayName("D. Corrupt/Invalid Bytes")
    class CorruptBytes {

        @Test
        @DisplayName("Corrupt bytes fail validation safely")
        void execute_corruptBytes_validationFails() {
            stubRetrievalSuccess(CORRUPT_BYTES, "image/png");
            stubValidationFailure(ImageStorageErrorCode.MEDIA_VALIDATION_FAILED,
                    "Medya doğrulaması başarısız");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailurePhase())
                    .isEqualTo(RetrievalValidationResult.FailurePhase.VALIDATION);
            String resultString = result.toString();
            assertThat(resultString).doesNotContain("mock-provider");
        }

        @Test
        @DisplayName("Empty bytes fail validation safely")
        void execute_emptyBytes_validationFails() {
            stubRetrievalSuccess(new byte[0], "image/png");
            stubValidationFailure(ImageStorageErrorCode.MEDIA_VALIDATION_FAILED,
                    "Medya doğrulaması başarısız");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            assertThat(result.isSuccess()).isFalse();
        }
    }

    // --- E. Forbidden Field Regression ---

    @Nested
    @DisplayName("E. Forbidden Field Regression")
    class ForbiddenFieldRegression {

        @Test
        @DisplayName("RetrievalValidationResult contains no forbidden fields")
        void resultModel_noForbiddenFields() {
            Set<String> fieldNames = getFieldNames(RetrievalValidationResult.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fieldNames)
                        .as("Result must not contain: %s", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("RetrievalValidationResult contains only allowed fields")
        void resultModel_onlyAllowedFields() {
            Set<String> allowed = Set.of(
                    "success", "validatedImageBytes", "detectedMimeType",
                    "fileSizeBytes", "width", "height", "checksumSha256",
                    "failurePhase", "errorCode", "errorMessage",
                    "correlationId", "retryable", "completedAt"
            );
            Set<String> fieldNames = getFieldNames(RetrievalValidationResult.class);
            assertThat(fieldNames).isSubsetOf(allowed);
        }
    }

    // --- F. Safety Regression ---

    @Nested
    @DisplayName("F. Safety Regression")
    class SafetyRegression {

        @Test
        @DisplayName("Retrieved does not mean approved — no approval behavior")
        void execute_success_noApprovalBehavior() {
            stubRetrievalSuccess(VALID_PNG_BYTES, "image/png");
            stubValidationSuccess("image/png", VALID_PNG_BYTES.length, 1, 1, "sha256");

            RetrievalValidationResult result = orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            // Success only means validated — no approval/status/publish
            assertThat(result.isSuccess()).isTrue();
            String resultStr = result.toString();
            assertThat(resultStr).doesNotContain("APPROVED");
            assertThat(resultStr).doesNotContain("READY_FOR_PUBLISH");
            assertThat(resultStr).doesNotContain("COMPLETED");
        }

        @Test
        @DisplayName("FailurePhase enum has only RETRIEVAL and VALIDATION")
        void failurePhase_onlyExpectedValues() {
            RetrievalValidationResult.FailurePhase[] values =
                    RetrievalValidationResult.FailurePhase.values();
            assertThat(values).hasSize(2);
            assertThat(values).containsExactlyInAnyOrder(
                    RetrievalValidationResult.FailurePhase.RETRIEVAL,
                    RetrievalValidationResult.FailurePhase.VALIDATION
            );
        }

        @Test
        @DisplayName("No storage interaction in this orchestration")
        void execute_noStorageInteraction() {
            stubRetrievalSuccess(VALID_PNG_BYTES, "image/png");
            stubValidationSuccess("image/png", VALID_PNG_BYTES.length, 1, 1, "sha256");

            orchestrationService.execute(
                    MOCK_PROVIDER_URL, MEDIA_ASSET_ID, GENERATED_ASSET_ID,
                    PROVIDER_ALIAS, CORRELATION_ID);

            // Service only has retrievalClient and validationService injected.
            // No storage adapter/handoff service dependency exists.
            verifyNoMoreInteractions(retrievalClient, validationService);
        }
    }

    // --- Stubs ---

    private void stubRetrievalSuccess(byte[] bytes, String contentType) {
        when(retrievalClient.retrieve(any(), any()))
                .thenReturn(ProviderOutputRetrievalResult.builder()
                        .success(true)
                        .imageBytes(bytes)
                        .detectedContentType(contentType)
                        .contentLength(bytes.length)
                        .correlationId(CORRELATION_ID)
                        .retrievedAt(LocalDateTime.now())
                        .build());
    }

    private void stubRetrievalFailure(ImageStorageErrorCode errorCode,
                                       String message, boolean retryable) {
        when(retrievalClient.retrieve(any(), any()))
                .thenReturn(ProviderOutputRetrievalResult.builder()
                        .success(false)
                        .errorCode(errorCode)
                        .errorMessage(message)
                        .correlationId(CORRELATION_ID)
                        .retryable(retryable)
                        .retrievedAt(LocalDateTime.now())
                        .build());
    }

    private void stubValidationSuccess(String mimeType, long size,
                                        int width, int height, String checksum) {
        when(validationService.validate(any(byte[].class), any()))
                .thenReturn(ImageMediaValidationResult.builder()
                        .valid(true)
                        .detectedMimeType(mimeType)
                        .fileSizeBytes(size)
                        .width(width)
                        .height(height)
                        .checksumSha256(checksum)
                        .validatedAt(LocalDateTime.now())
                        .correlationId(CORRELATION_ID)
                        .build());
    }

    private void stubValidationFailure(ImageStorageErrorCode errorCode, String message) {
        when(validationService.validate(any(byte[].class), any()))
                .thenReturn(ImageMediaValidationResult.builder()
                        .valid(false)
                        .errorCode(errorCode)
                        .errorMessage(message)
                        .validatedAt(LocalDateTime.now())
                        .correlationId(CORRELATION_ID)
                        .build());
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
