package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-009 Task 05 — Provider Output Retrieval Contracts & Safe Result Models Tests.
 *
 * Tests verify:
 * 1. Safe result model does not contain forbidden fields.
 * 2. Safe request model does not contain forbidden fields.
 * 3. Mock client returns controlled success on valid input.
 * 4. Mock client returns safe failure on null/blank URL.
 * 5. Mock client does not expose provider URL in result.
 * 6. Error messages are sanitized.
 * 7. Mock client does not perform external retrieval.
 */
@DisplayName("FP-009 Provider Output Retrieval Contract Tests")
class ProviderOutputRetrievalContractTest {

    /**
     * Field names that must NEVER appear in safe retrieval models.
     * These represent forbidden concepts per FP-009 safety contract.
     */
    private static final Set<String> FORBIDDEN_FIELD_NAMES = Set.of(
            "providerUrl", "providerOutputUrl", "providerImageUrl",
            "providerDownloadUrl", "temporaryUrl", "signedUrl",
            "publicUrl", "cdnUrl", "storageObjectKey", "storageKey",
            "storageBucket", "storageProvider", "base64", "imageBase64",
            "imageBinary", "rawProviderResponse", "rawStorageResponse",
            "rawResponse", "apiKey", "secretKey", "accessKey",
            "bearerToken", "authToken", "password", "credential",
            "internalPrompt", "stackTrace"
    );

    @Nested
    @DisplayName("ProviderOutputRetrievalResult — Safe Field Guardrails")
    class ResultModelSafetyTests {

        @Test
        @DisplayName("Result model does not contain provider URL field")
        void resultModel_doesNotContainProviderUrlField() {
            Set<String> fieldNames = getFieldNames(ProviderOutputRetrievalResult.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fieldNames)
                        .as("Result model must not contain forbidden field: %s", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Result model contains only allowed fields")
        void resultModel_containsOnlyAllowedFields() {
            Set<String> allowedFields = Set.of(
                    "success", "imageBytes", "detectedContentType", "contentLength",
                    "errorCode", "errorMessage", "correlationId", "retryable", "retrievedAt"
            );
            Set<String> fieldNames = getFieldNames(ProviderOutputRetrievalResult.class);
            assertThat(fieldNames)
                    .as("Result model must contain only allowed fields")
                    .isSubsetOf(allowedFields);
        }

        @Test
        @DisplayName("Success result can be built with safe fields")
        void resultModel_successBuild() {
            ProviderOutputRetrievalResult result = ProviderOutputRetrievalResult.builder()
                    .success(true)
                    .imageBytes(new byte[]{1, 2, 3})
                    .detectedContentType("image/png")
                    .contentLength(3)
                    .correlationId("test-correlation-id")
                    .retryable(false)
                    .build();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).hasSize(3);
            assertThat(result.getDetectedContentType()).isEqualTo("image/png");
            assertThat(result.getContentLength()).isEqualTo(3);
            assertThat(result.getCorrelationId()).isEqualTo("test-correlation-id");
            assertThat(result.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("Failure result can be built with safe error fields")
        void resultModel_failureBuild() {
            ProviderOutputRetrievalResult result = ProviderOutputRetrievalResult.builder()
                    .success(false)
                    .errorCode(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED)
                    .errorMessage("Sağlayıcı çıktısı indirilemedi")
                    .correlationId("test-correlation-id")
                    .retryable(true)
                    .build();

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getImageBytes()).isNull();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
            assertThat(result.getErrorMessage()).doesNotContain("http://", "https://");
            assertThat(result.isRetryable()).isTrue();
        }
    }

    @Nested
    @DisplayName("ProviderOutputRetrievalRequest — Safe Field Guardrails")
    class RequestModelSafetyTests {

        @Test
        @DisplayName("Request model does not contain provider URL field")
        void requestModel_doesNotContainProviderUrlField() {
            Set<String> fieldNames = getFieldNames(ProviderOutputRetrievalRequest.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fieldNames)
                        .as("Request model must not contain forbidden field: %s", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Request model contains only allowed fields")
        void requestModel_containsOnlyAllowedFields() {
            Set<String> allowedFields = Set.of(
                    "mediaAssetId", "generatedAssetId", "providerAlias", "correlationId"
            );
            Set<String> fieldNames = getFieldNames(ProviderOutputRetrievalRequest.class);
            assertThat(fieldNames)
                    .as("Request model must contain only allowed fields")
                    .isSubsetOf(allowedFields);
        }

        @Test
        @DisplayName("Request model can be built with safe fields")
        void requestModel_build() {
            UUID mediaAssetId = UUID.randomUUID();
            UUID generatedAssetId = UUID.randomUUID();

            ProviderOutputRetrievalRequest request = ProviderOutputRetrievalRequest.builder()
                    .mediaAssetId(mediaAssetId)
                    .generatedAssetId(generatedAssetId)
                    .providerAlias("openai")
                    .correlationId("test-correlation-id")
                    .build();

            assertThat(request.getMediaAssetId()).isEqualTo(mediaAssetId);
            assertThat(request.getGeneratedAssetId()).isEqualTo(generatedAssetId);
            assertThat(request.getProviderAlias()).isEqualTo("openai");
            assertThat(request.getCorrelationId()).isEqualTo("test-correlation-id");
        }
    }

    @Nested
    @DisplayName("MockProviderOutputRetrievalClient — Behavior Tests")
    class MockClientTests {

        private MockProviderOutputRetrievalClient client;

        @BeforeEach
        void setUp() {
            client = new MockProviderOutputRetrievalClient();
        }

        @Test
        @DisplayName("Returns success with PNG bytes on valid URL")
        void retrieve_validUrl_returnsSuccess() {
            ProviderOutputRetrievalRequest request = ProviderOutputRetrievalRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .providerAlias("mock")
                    .correlationId("test-corr-001")
                    .build();

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://mock-provider.example.com/image/123", request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).isNotNull();
            assertThat(result.getImageBytes().length).isGreaterThan(0);
            assertThat(result.getDetectedContentType()).isEqualTo("image/png");
            assertThat(result.getContentLength()).isGreaterThan(0);
            assertThat(result.getCorrelationId()).isEqualTo("test-corr-001");
            assertThat(result.getRetrievedAt()).isNotNull();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Returns safe failure on null URL")
        void retrieve_nullUrl_returnsSafeFailure() {
            ProviderOutputRetrievalRequest request = ProviderOutputRetrievalRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .providerAlias("mock")
                    .correlationId("test-corr-002")
                    .build();

            ProviderOutputRetrievalResult result = client.retrieve(null, request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getImageBytes()).isNull();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
            assertThat(result.getErrorMessage()).isNotBlank();
            assertThat(result.getCorrelationId()).isEqualTo("test-corr-002");
            assertThat(result.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("Returns safe failure on blank URL")
        void retrieve_blankUrl_returnsSafeFailure() {
            ProviderOutputRetrievalRequest request = ProviderOutputRetrievalRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .providerAlias("mock")
                    .correlationId("test-corr-003")
                    .build();

            ProviderOutputRetrievalResult result = client.retrieve("   ", request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
            assertThat(result.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("Returns safe failure on null request")
        void retrieve_nullRequest_returnsSafeFailure() {
            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://mock-provider.example.com/image/123", null);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
            assertThat(result.getErrorMessage()).isNotBlank();
            assertThat(result.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("Success result does not contain provider URL")
        void retrieve_successResult_doesNotContainProviderUrl() {
            ProviderOutputRetrievalRequest request = ProviderOutputRetrievalRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .providerAlias("mock")
                    .correlationId("test-corr-004")
                    .build();

            String providerUrl = "https://secret-provider.example.com/image/sensitive-id";
            ProviderOutputRetrievalResult result = client.retrieve(providerUrl, request);

            // The result must not contain the provider URL anywhere
            assertThat(result.isSuccess()).isTrue();
            String resultString = result.toString();
            assertThat(resultString).doesNotContain("secret-provider.example.com");
            assertThat(resultString).doesNotContain("sensitive-id");
            assertThat(resultString).doesNotContain(providerUrl);
        }

        @Test
        @DisplayName("Failure result error message does not contain provider URL")
        void retrieve_failureResult_errorMessageDoesNotContainUrl() {
            ProviderOutputRetrievalRequest request = ProviderOutputRetrievalRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .providerAlias("mock")
                    .correlationId("test-corr-005")
                    .build();

            ProviderOutputRetrievalResult result = client.retrieve(null, request);

            assertThat(result.getErrorMessage()).doesNotContain("http://");
            assertThat(result.getErrorMessage()).doesNotContain("https://");
            assertThat(result.getErrorMessage()).doesNotContain("api_key");
            assertThat(result.getErrorMessage()).doesNotContain("secret");
            assertThat(result.getErrorMessage()).doesNotContain("token");
        }

        @Test
        @DisplayName("Mock client does not perform external network call")
        void retrieve_doesNotPerformExternalCall() {
            // The mock client uses static MOCK_PNG_BYTES — no network dependency.
            // Verify by calling with a clearly fake URL that would fail if accessed.
            ProviderOutputRetrievalRequest request = ProviderOutputRetrievalRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .providerAlias("mock")
                    .correlationId("test-corr-006")
                    .build();

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://this-url-does-not-exist.invalid/should-not-be-called", request);

            // If it tried to actually call this URL, it would fail.
            // Mock returns success with controlled bytes.
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).isEqualTo(MockProviderOutputRetrievalClient.MOCK_PNG_BYTES);
        }

        @Test
        @DisplayName("Mock PNG bytes start with valid PNG signature")
        void mockPngBytes_hasValidSignature() {
            byte[] pngSignature = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            };
            byte[] mockBytes = MockProviderOutputRetrievalClient.MOCK_PNG_BYTES;

            assertThat(mockBytes.length).isGreaterThanOrEqualTo(8);
            for (int i = 0; i < 8; i++) {
                assertThat(mockBytes[i])
                        .as("PNG signature byte at index %d", i)
                        .isEqualTo(pngSignature[i]);
            }
        }
    }

    @Nested
    @DisplayName("ProviderOutputRetrievalClient — Interface Contract Tests")
    class InterfaceContractTests {

        @Test
        @DisplayName("Interface has exactly one method: retrieve")
        void interface_hasRetrieveMethod() {
            assertThat(ProviderOutputRetrievalClient.class.isInterface()).isTrue();
            assertThat(ProviderOutputRetrievalClient.class.getDeclaredMethods()).hasSize(1);
            assertThat(ProviderOutputRetrievalClient.class.getDeclaredMethods()[0].getName())
                    .isEqualTo("retrieve");
        }

        @Test
        @DisplayName("Retrieve method has correct parameter types")
        void interface_retrieveMethodParameters() throws NoSuchMethodException {
            var method = ProviderOutputRetrievalClient.class.getDeclaredMethod(
                    "retrieve", String.class, ProviderOutputRetrievalRequest.class);

            assertThat(method.getReturnType()).isEqualTo(ProviderOutputRetrievalResult.class);
            assertThat(method.getParameterCount()).isEqualTo(2);
            // First param is String (provider URL - transient, not in request model)
            assertThat(method.getParameterTypes()[0]).isEqualTo(String.class);
            // Second param is the safe request model
            assertThat(method.getParameterTypes()[1]).isEqualTo(ProviderOutputRetrievalRequest.class);
        }

        @Test
        @DisplayName("MockProviderOutputRetrievalClient implements ProviderOutputRetrievalClient")
        void mockClient_implementsInterface() {
            assertThat(ProviderOutputRetrievalClient.class)
                    .isAssignableFrom(MockProviderOutputRetrievalClient.class);
        }
    }

    @Nested
    @DisplayName("Error Sanitization Integration Tests")
    class ErrorSanitizationTests {

        @Test
        @DisplayName("Sanitizer redacts URLs in error messages")
        void sanitizer_redactsUrls() {
            String rawMessage = "Failed to download from https://api.openai.com/v1/images/12345";
            String sanitized = ImageStorageErrorNormalizer.sanitize(rawMessage);
            assertThat(sanitized).doesNotContain("https://api.openai.com");
            assertThat(sanitized).contains("[URL_REDACTED]");
        }

        @Test
        @DisplayName("Sanitizer redacts API keys in error messages")
        void sanitizer_redactsApiKeys() {
            String rawMessage = "Authentication failed: api_key=sk-abc123def456";
            String sanitized = ImageStorageErrorNormalizer.sanitize(rawMessage);
            assertThat(sanitized).doesNotContain("sk-abc123def456");
            assertThat(sanitized).contains("[REDACTED]");
        }

        @Test
        @DisplayName("Sanitizer redacts storage keys in error messages")
        void sanitizer_redactsStorageKeys() {
            String rawMessage = "Storage error for imagegen/550e8400-e29b-41d4-a716-446655440000/7c9e6679-7425-40de-944b-e07fc1f90ae7";
            String sanitized = ImageStorageErrorNormalizer.sanitize(rawMessage);
            assertThat(sanitized).contains("[STORAGE_KEY_REDACTED]");
        }

        @Test
        @DisplayName("Safe error messages for retrieval error codes are product-level")
        void safeMessages_areProductLevel() {
            // Verify existing retrieval-related error codes produce safe Turkish messages
            assertThat(ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE))
                    .isNotBlank()
                    .doesNotContain("http://")
                    .doesNotContain("https://")
                    .doesNotContain("api_key");

            assertThat(ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.PROVIDER_OUTPUT_EXPIRED))
                    .isNotBlank();

            assertThat(ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED))
                    .isNotBlank();
        }
    }

    // --- Helper Methods ---

    /**
     * Extract all declared field names from a class (excluding inherited fields).
     */
    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
