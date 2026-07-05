package com.anadoluatlasi.backend.imagegen.storage;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageMediaAssetResponse;
import com.anadoluatlasi.backend.imagegen.entity.ImageMediaAsset;
import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-009 Task 08 — V12 Migration + Real Private Storage Adapter Shell Tests.
 */
@DisplayName("FP-009 Task 08 — S3 Storage Adapter Shell & Safety Tests")
class S3ImageStorageAdapterTest {

    private static final Set<String> FORBIDDEN_FIELD_NAMES = Set.of(
            "providerUrl", "providerOutputUrl", "providerImageUrl",
            "signedUrl", "publicUrl", "cdnUrl",
            "base64", "imageBase64", "rawProviderResponse", "rawStorageResponse",
            "apiKey", "secretKey", "bearerToken", "password", "stackTrace"
    );

    private S3PrivateStorageProperties properties;
    private S3ImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new S3PrivateStorageProperties();
        properties.setEnabled(false);
        properties.setEndpoint("http://localhost:9000");
        properties.setRegion("us-east-1");
        properties.setBucket("test-bucket");
        properties.setPathPrefix("imagegen/");
        properties.setAccessKey("FAKE_ACCESS_KEY");
        properties.setSecretKey("FAKE_SECRET_KEY");
        adapter = new S3ImageStorageAdapter(properties);
    }

    // ── A. Migration/Schema Safety ──────────────────────────────────

    @Nested
    @DisplayName("A. V12 Migration & Entity Safety")
    class MigrationSafety {

        @Test
        @DisplayName("ImageMediaAsset has storage_provider, storage_bucket, storage_object_key fields")
        void entity_hasStorageReferenceFields() {
            Set<String> fields = getFieldNames(ImageMediaAsset.class);
            assertThat(fields).contains("storageProvider", "storageBucket", "storageObjectKey");
        }

        @Test
        @DisplayName("ImageMediaAsset does not have forbidden fields")
        void entity_noForbiddenFields() {
            Set<String> fields = getFieldNames(ImageMediaAsset.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields)
                        .as("Entity must not contain: %s", forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    // ── B. Storage Adapter Shell Safety ─────────────────────────────

    @Nested
    @DisplayName("B. S3 Adapter Shell Behavior")
    class AdapterShellBehavior {

        @Test
        @DisplayName("Provider alias is 's3'")
        void providerAlias_isS3() {
            assertThat(adapter.getProviderAlias()).isEqualTo("s3");
        }

        @Test
        @DisplayName("Implements ImageStorageAdapter")
        void implementsInterface() {
            assertThat(ImageStorageAdapter.class).isAssignableFrom(S3ImageStorageAdapter.class);
        }

        @Test
        @DisplayName("Write returns safe failure when disabled")
        void write_disabled_returnsSafeFailure() {
            properties.setEnabled(false);
            ImageStorageWriteRequest request = ImageStorageWriteRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .correlationId("test-corr")
                    .mimeType("image/png")
                    .imageBytes(new byte[]{1, 2, 3})
                    .requestedAt(LocalDateTime.now())
                    .build();

            ImageStorageWriteResult result = adapter.write(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStorageProviderAlias()).isEqualTo("s3");
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
            assertThat(result.getErrorMessage()).isNotBlank();
        }

        @Test
        @DisplayName("Write returns safe failure when enabled (shell not implemented)")
        void write_enabled_returnsSafeShellFailure() {
            properties.setEnabled(true);
            ImageStorageWriteRequest request = ImageStorageWriteRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .correlationId("test-corr")
                    .mimeType("image/png")
                    .imageBytes(new byte[]{1, 2, 3})
                    .requestedAt(LocalDateTime.now())
                    .build();

            ImageStorageWriteResult result = adapter.write(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStorageObjectKey()).isNotBlank();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
        }

        @Test
        @DisplayName("Read returns safe failure (shell not implemented)")
        void read_returnsSafeFailure() {
            ImageStorageReadRequest request = ImageStorageReadRequest.builder()
                    .storageObjectKey("imagegen/test-key")
                    .correlationId("test-corr")
                    .build();

            ImageStorageReadResult result = adapter.read(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
            assertThat(result.getErrorMessage()).isNotBlank();
        }

        @Test
        @DisplayName("Write result contains no public/signed URL")
        void writeResult_noPublicOrSignedUrl() {
            Set<String> fields = getFieldNames(ImageStorageWriteResult.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields)
                        .as("WriteResult must not contain: %s", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Read result contains no public/signed URL")
        void readResult_noPublicOrSignedUrl() {
            Set<String> fields = getFieldNames(ImageStorageReadResult.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields)
                        .as("ReadResult must not contain: %s", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Write does not make real external call")
        void write_noExternalCall() {
            properties.setEnabled(true);
            ImageStorageWriteRequest request = ImageStorageWriteRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .correlationId("test-corr")
                    .mimeType("image/png")
                    .imageBytes(new byte[]{1, 2, 3})
                    .requestedAt(LocalDateTime.now())
                    .build();

            // No real S3 endpoint — would fail if actually called
            ImageStorageWriteResult result = adapter.write(request);
            assertThat(result.isSuccess()).isFalse(); // Shell — no real call
        }

        @Test
        @DisplayName("Null request handled safely for write")
        void write_nullRequest_safe() {
            properties.setEnabled(false);
            ImageStorageWriteResult result = adapter.write(null);
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Null request handled safely for read")
        void read_nullRequest_safe() {
            ImageStorageReadResult result = adapter.read(null);
            assertThat(result.isSuccess()).isFalse();
        }
    }

    // ── C. Config Safety ────────────────────────────────────────────

    @Nested
    @DisplayName("C. Config Safety")
    class ConfigSafety {

        @Test
        @DisplayName("toString() does not contain access key")
        void toString_noAccessKey() {
            properties.setAccessKey("AKIAIOSFODNN7EXAMPLE");
            properties.setSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            String str = properties.toString();
            assertThat(str).doesNotContain("AKIAIOSFODNN7EXAMPLE");
            assertThat(str).doesNotContain("wJalrXUtnFEMI");
            assertThat(str).doesNotContain("EXAMPLEKEY");
        }

        @Test
        @DisplayName("Default config is disabled")
        void defaultConfig_isDisabled() {
            S3PrivateStorageProperties defaults = new S3PrivateStorageProperties();
            assertThat(defaults.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Config fields do not include forbidden names")
        void config_noForbiddenFieldNames() {
            Set<String> fields = getFieldNames(S3PrivateStorageProperties.class);
            // These specific forbidden fields should not be present
            assertThat(fields).doesNotContain("providerUrl", "signedUrl", "publicUrl",
                    "cdnUrl", "base64", "rawProviderResponse", "rawStorageResponse",
                    "bearerToken", "password", "stackTrace");
        }
    }

    // ── D. DTO Forbidden Field Regression ───────────────────────────

    @Nested
    @DisplayName("D. DTO Forbidden Field Regression")
    class DtoRegression {

        @Test
        @DisplayName("ImageMediaAssetResponse has no storage key/provider URL/signed URL")
        void adminDto_noForbiddenFields() {
            Set<String> fields = getFieldNames(ImageMediaAssetResponse.class);
            assertThat(fields).doesNotContain(
                    "storageObjectKey", "storageBucket", "storageProvider",
                    "providerUrl", "providerOutputUrl", "signedUrl",
                    "publicUrl", "cdnUrl", "base64", "rawProviderResponse",
                    "rawStorageResponse", "apiKey", "secretKey"
            );
        }
    }

    // ── E. Review/Publish Regression ────────────────────────────────

    @Nested
    @DisplayName("E. No Review/Publish Side Effects")
    class NoSideEffects {

        @Test
        @DisplayName("Write result does not mark asset approved")
        void writeResult_noApproval() {
            properties.setEnabled(true);
            ImageStorageWriteRequest request = ImageStorageWriteRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .correlationId("test-corr")
                    .mimeType("image/png")
                    .imageBytes(new byte[]{1})
                    .requestedAt(LocalDateTime.now())
                    .build();

            ImageStorageWriteResult result = adapter.write(request);
            String resultStr = result.toString();
            assertThat(resultStr).doesNotContain("APPROVED");
            assertThat(resultStr).doesNotContain("READY_FOR_PUBLISH");
            assertThat(resultStr).doesNotContain("COMPLETED");
        }

        @Test
        @DisplayName("Object key generation produces internal path format")
        void objectKey_internalFormat() {
            UUID mediaAssetId = UUID.randomUUID();
            ImageStorageWriteRequest request = ImageStorageWriteRequest.builder()
                    .mediaAssetId(mediaAssetId)
                    .correlationId("test-corr")
                    .build();

            String key = adapter.generateObjectKey(request);

            assertThat(key).startsWith("imagegen/");
            assertThat(key).contains(mediaAssetId.toString());
            assertThat(key).doesNotContain("http://");
            assertThat(key).doesNotContain("https://");
        }

        @Test
        @DisplayName("Write error messages are sanitized")
        void writeError_isSanitized() {
            properties.setEnabled(false);
            ImageStorageWriteRequest request = ImageStorageWriteRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .correlationId("test-corr")
                    .build();

            ImageStorageWriteResult result = adapter.write(request);

            assertThat(result.getErrorMessage()).doesNotContain("FAKE_ACCESS_KEY");
            assertThat(result.getErrorMessage()).doesNotContain("FAKE_SECRET_KEY");
            assertThat(result.getErrorMessage()).doesNotContain("localhost:9000");
            assertThat(result.getErrorMessage()).doesNotContain("test-bucket");
        }
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
