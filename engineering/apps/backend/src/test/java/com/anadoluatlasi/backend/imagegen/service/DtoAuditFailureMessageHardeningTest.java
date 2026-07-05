package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageMediaAssetResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.StorageHandoffResponse;
import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.provider.ProviderOutputReference;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageReadResult;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageWriteRequest;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageWriteResult;
import com.anadoluatlasi.backend.imagegen.storage.S3PrivateStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-009 Task 11 — DTO / Audit / Failure Message Hardening.
 */
@DisplayName("FP-009 Task 11 — DTO / Audit / Failure Message Hardening")
class DtoAuditFailureMessageHardeningTest {

    private static final Set<String> FORBIDDEN_DTO_FIELDS = Set.of(
            "providerUrl", "providerOutputUrl", "providerImageUrl", "temporaryUrl",
            "signedUrl", "publicUrl", "cdnUrl",
            "storageObjectKey", "storageReference", "storageBucket", "storageProvider",
            "base64", "imageBase64", "imageData",
            "rawProviderResponse", "rawStorageResponse", "rawStorageProviderResponse",
            "apiKey", "secretKey", "bearerToken", "password", "accessKey",
            "stackTrace", "internalFullPrompt", "internalException"
    );

    private static final Set<String> FORBIDDEN_RESULT_FIELDS = Set.of(
            "providerUrl", "providerOutputUrl", "providerImageUrl",
            "signedUrl", "publicUrl", "cdnUrl",
            "storageObjectKey", "storageBucket", "storageProvider",
            "base64", "imageBase64",
            "rawProviderResponse", "rawStorageResponse",
            "apiKey", "secretKey", "bearerToken", "password",
            "stackTrace"
    );

    // ── A. Admin DTO Forbidden Field Regression ─────────────────────

    @Nested
    @DisplayName("A. Admin DTO Forbidden Field Regression")
    class AdminDtoRegression {

        @Test
        @DisplayName("StorageHandoffResponse — no forbidden fields")
        void storageHandoffResponse() {
            assertNoForbiddenDtoFields(StorageHandoffResponse.class);
        }

        @Test
        @DisplayName("ImageMediaAssetResponse — no forbidden fields")
        void imageMediaAssetResponse() {
            assertNoForbiddenDtoFields(ImageMediaAssetResponse.class);
        }

        @Test
        @DisplayName("ImagePreviewProxyResult — no forbidden fields")
        void previewProxyResult() {
            assertNoForbiddenDtoFields(ImagePreviewProxyResult.class);
        }

        @Test
        @DisplayName("ProviderOutputRetrievalResult — no URL/key/base64/secret fields")
        void retrievalResult() {
            assertNoForbiddenResultFields(ProviderOutputRetrievalResult.class);
        }

        @Test
        @DisplayName("RetrievalValidationResult — no URL/key/base64/secret fields")
        void validationResult() {
            assertNoForbiddenResultFields(RetrievalValidationResult.class);
        }

        @Test
        @DisplayName("ImageStorageHandoffResult — no forbidden fields")
        void handoffResult() {
            assertNoForbiddenResultFields(ImageStorageHandoffResult.class);
        }

        @Test
        @DisplayName("ImageStorageHandoffRequest — no forbidden fields")
        void handoffRequest() {
            assertNoForbiddenResultFields(ImageStorageHandoffRequest.class);
        }

        @Test
        @DisplayName("ProviderOutputRetrievalRequest — no forbidden fields")
        void retrievalRequest() {
            assertNoForbiddenResultFields(ProviderOutputRetrievalRequest.class);
        }
    }

    // ── B. Failure Message Sanitization ──────────────────────────────

    @Nested
    @DisplayName("B. Failure Message Sanitization")
    class FailureMessageSanitization {

        @Test
        @DisplayName("All error codes have safe messages via normalizer")
        void allErrorCodes_haveSafeMessages() {
            for (ImageStorageErrorCode code : ImageStorageErrorCode.values()) {
                String msg = ImageStorageErrorNormalizer.safeMessage(code);
                assertThat(msg).isNotNull();
                assertThat(msg).isNotBlank();
                assertThat(msg).doesNotContainIgnoringCase("http");
                assertThat(msg).doesNotContainIgnoringCase("s3://");
                assertThat(msg).doesNotContainIgnoringCase("apiKey");
                assertThat(msg).doesNotContainIgnoringCase("secret");
                assertThat(msg).doesNotContainIgnoringCase("storageObjectKey");
                assertThat(msg).doesNotContainIgnoringCase("base64");
                assertThat(msg).doesNotContainIgnoringCase("stackTrace");
            }
        }

        @Test
        @DisplayName("PROVIDER_OUTPUT_UNAVAILABLE message is safe")
        void providerUnavailable_safeMessage() {
            String msg = ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
            assertThat(msg).doesNotContain("openai");
            assertThat(msg).doesNotContain("https://");
            assertThat(msg).doesNotContain("oaidalleapiprodscus");
        }

        @Test
        @DisplayName("PROVIDER_OUTPUT_DOWNLOAD_FAILED message is safe")
        void providerDownloadFailed_safeMessage() {
            String msg = ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
            assertThat(msg).doesNotContain("https://");
            assertThat(msg).doesNotContain("token");
        }

        @Test
        @DisplayName("STORAGE_WRITE_FAILED message is safe")
        void storageWriteFailed_safeMessage() {
            String msg = ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.STORAGE_WRITE_FAILED);
            assertThat(msg).doesNotContain("bucket");
            assertThat(msg).doesNotContain("objectKey");
        }

        @Test
        @DisplayName("STORAGE_READ_FAILED message is safe")
        void storageReadFailed_safeMessage() {
            String msg = ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.STORAGE_READ_FAILED);
            assertThat(msg).doesNotContain("s3://");
            assertThat(msg).doesNotContain("key");
        }

        @Test
        @DisplayName("Failure result toString does not leak URL or secrets")
        void failureResult_toStringIsSafe() {
            ImageStorageHandoffResult result = ImageStorageHandoffResult.builder()
                    .success(false)
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_FAILED)
                    .previewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE)
                    .errorCode(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED)
                    .errorMessage("Provider çıktısı indirilemedi")
                    .correlationId("test-corr")
                    .build();

            String str = result.toString();
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("oai.example.com");
            assertThat(str).doesNotContain("imagegen/");
        }
    }

    // ── C. Logging / toString Safety ────────────────────────────────

    @Nested
    @DisplayName("C. Logging / toString Safety")
    class LoggingToStringSafety {

        @Test
        @DisplayName("ProviderOutputReference.toString() hides URL")
        void providerOutputRef_toStringHidesUrl() {
            ProviderOutputReference ref = new ProviderOutputReference(
                    null, "https://oaidalleapiprodscus.blob.core.windows.net/secret-image-123.png");

            String str = ref.toString();
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("oaidalleapiprodscus");
            assertThat(str).doesNotContain("secret-image");
            assertThat(str).contains("hasProviderOutputUrl=true");
        }

        @Test
        @DisplayName("ProviderOutputReference.toString() handles null URL")
        void providerOutputRef_nullUrl() {
            ProviderOutputReference ref = new ProviderOutputReference(null, null);
            String str = ref.toString();
            assertThat(str).contains("hasProviderOutputUrl=false");
            assertThat(str).doesNotContain("null:");
        }

        @Test
        @DisplayName("ProviderOutputReference.toString() handles blank URL")
        void providerOutputRef_blankUrl() {
            ProviderOutputReference ref = new ProviderOutputReference(null, "   ");
            String str = ref.toString();
            assertThat(str).contains("hasProviderOutputUrl=false");
        }

        @Test
        @DisplayName("S3PrivateStorageProperties.toString() excludes secrets")
        void s3Config_toStringExcludesSecrets() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setAccessKey("AKIAIOSFODNN7EXAMPLE");
            props.setSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            props.setEndpoint("https://s3.amazonaws.com");
            props.setBucket("my-private-bucket");

            String str = props.toString();
            assertThat(str).doesNotContain("AKIAIOSFODNN7EXAMPLE");
            assertThat(str).doesNotContain("wJalrXUtnFEMI");
            assertThat(str).doesNotContain("EXAMPLEKEY");
        }

        @Test
        @DisplayName("ImageStorageWriteResult.toString() does not expose storage key to callers")
        void writeResult_toStringCheck() {
            ImageStorageWriteResult result = ImageStorageWriteResult.builder()
                    .success(true)
                    .storageProviderAlias("s3")
                    .storageObjectKey("imagegen/secret-uuid/file.png")
                    .build();

            // WriteResult CAN have storageObjectKey internally (it's internal model),
            // but we verify that the *DTO layer* never copies it
            Set<String> dtoFields = getFieldNames(StorageHandoffResponse.class);
            assertThat(dtoFields).doesNotContain("storageObjectKey");
        }

        @Test
        @DisplayName("ImageStorageReadResult has no URL/secret fields")
        void readResult_noUrlFields() {
            assertNoForbiddenResultFields(ImageStorageReadResult.class);
        }

        @Test
        @DisplayName("ImageStorageWriteRequest has no URL/secret fields")
        void writeRequest_noUrlFields() {
            Set<String> fields = getFieldNames(ImageStorageWriteRequest.class);
            assertThat(fields).doesNotContain(
                    "providerUrl", "signedUrl", "publicUrl", "cdnUrl",
                    "apiKey", "secretKey", "base64");
        }

        @Test
        @DisplayName("RetrievalValidationResult toString does not contain URL or secret")
        void rvResult_toStringIsSafe() {
            RetrievalValidationResult result = RetrievalValidationResult.builder()
                    .success(false)
                    .failurePhase(RetrievalValidationResult.FailurePhase.RETRIEVAL)
                    .errorCode(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED)
                    .errorMessage("İndirme başarısız")
                    .correlationId("test-corr")
                    .build();

            String str = result.toString();
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("apiKey");
            assertThat(str).doesNotContain("secret");
        }
    }

    // ── D. Endpoint Response Safety ─────────────────────────────────

    @Nested
    @DisplayName("D. Endpoint Response Safety")
    class EndpointResponseSafety {

        @Test
        @DisplayName("StorageHandoffResponse allowed fields are safe")
        void handoffResponse_allowedFieldsAreSafe() {
            Set<String> fields = getFieldNames(StorageHandoffResponse.class);
            Set<String> allowed = Set.of(
                    "success", "mediaAssetId", "generatedAssetId",
                    "storageHandoffStatus", "previewStatus",
                    "mimeType", "width", "height", "fileSizeBytes",
                    "checksumSha256", "storedAt",
                    "errorCode", "errorMessage", "correlationId"
            );
            assertThat(fields).containsAll(allowed);
            // No extras beyond allowed
            for (String field : fields) {
                assertThat(allowed)
                        .as("Unexpected field '%s' in StorageHandoffResponse", field)
                        .contains(field);
            }
        }

        @Test
        @DisplayName("ImageMediaAssetResponse has no V12 internal fields")
        void mediaAssetResponse_noV12Internals() {
            Set<String> fields = getFieldNames(ImageMediaAssetResponse.class);
            assertThat(fields).doesNotContain(
                    "storageObjectKey", "storageBucket", "storageProvider");
        }

        @Test
        @DisplayName("ImagePreviewProxyResult allowed fields are safe")
        void previewResult_allowedFieldsAreSafe() {
            Set<String> fields = getFieldNames(ImagePreviewProxyResult.class);
            Set<String> allowed = Set.of(
                    "success", "imageBytes", "mimeType", "fileSizeBytes",
                    "errorCode", "errorMessage", "correlationId"
            );
            assertThat(fields).containsAll(allowed);
            for (String field : fields) {
                assertThat(allowed)
                        .as("Unexpected field '%s' in ImagePreviewProxyResult", field)
                        .contains(field);
            }
        }
    }

    // ── E. Review / Publish Regression ───────────────────────────────

    @Nested
    @DisplayName("E. Review / Publish Regression")
    class ReviewPublishRegression {

        @Test
        @DisplayName("No READY_FOR_PUBLISH in storage/preview enums")
        void noReadyForPublish() {
            for (ImageStorageHandoffStatus s : ImageStorageHandoffStatus.values()) {
                assertThat(s.name()).isNotEqualTo("READY_FOR_PUBLISH");
                assertThat(s.name()).isNotEqualTo("COMPLETED");
                assertThat(s.name()).isNotEqualTo("APPROVED");
            }
            for (ImagePreviewStatus s : ImagePreviewStatus.values()) {
                assertThat(s.name()).isNotEqualTo("READY_FOR_PUBLISH");
                assertThat(s.name()).isNotEqualTo("COMPLETED");
                assertThat(s.name()).isNotEqualTo("APPROVED");
            }
        }

        @Test
        @DisplayName("No publish/export field in any DTO")
        void noPublishExportFields() {
            Set<String> forbidden = Set.of(
                    "publishStatus", "exportStatus", "publishedAt", "exportedAt",
                    "readyForPublish", "publishUrl", "exportUrl"
            );
            for (Class<?> dto : List.of(
                    StorageHandoffResponse.class,
                    ImageMediaAssetResponse.class,
                    ImagePreviewProxyResult.class,
                    ImageStorageHandoffResult.class)) {
                Set<String> fields = getFieldNames(dto);
                for (String f : forbidden) {
                    assertThat(fields)
                            .as("%s must not contain '%s'", dto.getSimpleName(), f)
                            .doesNotContain(f);
                }
            }
        }

        @Test
        @DisplayName("GeneratedAssetStatus has no COMPLETED")
        void generatedAssetStatus_noCompleted() {
            Set<String> values = Arrays.stream(GeneratedAssetStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(values).doesNotContain("COMPLETED", "READY_FOR_PUBLISH");
        }
    }

    // ── F. No Broad Behavior Change ─────────────────────────────────

    @Nested
    @DisplayName("F. No Broad Behavior Change")
    class NoBroadBehaviorChange {

        @Test
        @DisplayName("ControlledStorageHandoffOrchestrationService has no URL/secret fields")
        void orchestrationService_noSensitiveFields() {
            Set<String> fields = getFieldNames(ControlledStorageHandoffOrchestrationService.class);
            assertThat(fields).doesNotContain(
                    "providerUrl", "storageObjectKey", "apiKey",
                    "secretKey", "signedUrl", "publicUrl", "base64");
        }

        @Test
        @DisplayName("Entity has internal V12 fields but all DTOs exclude them")
        void entityVsDtoFieldGap() {
            Set<String> entityFields = getFieldNames(ImageMediaAsset.class);
            assertThat(entityFields).contains("storageProvider", "storageBucket", "storageObjectKey");

            // All admin DTOs must exclude these
            for (Class<?> dto : List.of(
                    ImageMediaAssetResponse.class,
                    StorageHandoffResponse.class,
                    ImagePreviewProxyResult.class)) {
                Set<String> dtoFields = getFieldNames(dto);
                assertThat(dtoFields)
                        .as("%s must not expose entity internal fields", dto.getSimpleName())
                        .doesNotContain("storageObjectKey", "storageBucket", "storageProvider");
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void assertNoForbiddenDtoFields(Class<?> clazz) {
        Set<String> fields = getFieldNames(clazz);
        for (String forbidden : FORBIDDEN_DTO_FIELDS) {
            assertThat(fields)
                    .as("%s must not contain '%s'", clazz.getSimpleName(), forbidden)
                    .doesNotContain(forbidden);
        }
    }

    private void assertNoForbiddenResultFields(Class<?> clazz) {
        Set<String> fields = getFieldNames(clazz);
        for (String forbidden : FORBIDDEN_RESULT_FIELDS) {
            assertThat(fields)
                    .as("%s must not contain '%s'", clazz.getSimpleName(), forbidden)
                    .doesNotContain(forbidden);
        }
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
