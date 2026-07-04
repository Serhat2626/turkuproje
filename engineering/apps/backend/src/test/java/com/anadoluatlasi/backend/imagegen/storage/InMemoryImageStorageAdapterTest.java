package com.anadoluatlasi.backend.imagegen.storage;

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
 * FP-008 Task 07: InMemoryImageStorageAdapter unit tests.
 *
 * <p>No external HTTP/SDK, no storage provider — pure in-memory tests.</p>
 */
class InMemoryImageStorageAdapterTest {

    private InMemoryImageStorageAdapter adapter;
    private static final String CORRELATION = "test-storage-001";
    private static final UUID MEDIA_ASSET_ID = UUID.randomUUID();
    private static final UUID GENERATED_ASSET_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new InMemoryImageStorageAdapter();
    }

    // ── Helper ──────────────────────────────────────────────────────

    private ImageStorageWriteRequest validWriteRequest(byte[] bytes) {
        return ImageStorageWriteRequest.builder()
                .mediaAssetId(MEDIA_ASSET_ID)
                .generatedAssetId(GENERATED_ASSET_ID)
                .correlationId(CORRELATION)
                .mimeType("image/png")
                .width(100)
                .height(50)
                .fileSizeBytes(bytes.length)
                .checksumSha256("abc123def456")
                .imageBytes(bytes)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // ── Write Tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Write operations")
    class WriteOperations {

        @Test
        @DisplayName("Write valid bytes — success")
        void writeValidBytesSuccess() {
            byte[] data = new byte[]{1, 2, 3, 4, 5};
            ImageStorageWriteResult result = adapter.write(validWriteRequest(data));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStorageProviderAlias()).isEqualTo("mock");
            assertThat(result.getStorageObjectKey()).isNotNull().isNotBlank();
            assertThat(result.getStorageObjectKey()).startsWith("imagegen/");
            assertThat(result.getMimeType()).isEqualTo("image/png");
            assertThat(result.getWidth()).isEqualTo(100);
            assertThat(result.getHeight()).isEqualTo(50);
            assertThat(result.getFileSizeBytes()).isEqualTo(5);
            assertThat(result.getChecksumSha256()).isEqualTo("abc123def456");
            assertThat(result.getStoredAt()).isNotNull();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
            assertThat(result.getCorrelationId()).isEqualTo(CORRELATION);
        }

        @Test
        @DisplayName("Write stores object retrievable by key")
        void writeStoresRetrievableObject() {
            byte[] data = new byte[]{10, 20, 30};
            ImageStorageWriteResult writeResult = adapter.write(validWriteRequest(data));

            assertThat(writeResult.isSuccess()).isTrue();
            assertThat(adapter.exists(writeResult.getStorageObjectKey())).isTrue();
            assertThat(adapter.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("Write null request — failure")
        void writeNullRequestFailure() {
            ImageStorageWriteResult result = adapter.write(null);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
            assertThat(result.getErrorMessage()).isNotNull();
        }

        @Test
        @DisplayName("Write empty bytes — failure")
        void writeEmptyBytesFailure() {
            ImageStorageWriteRequest request = ImageStorageWriteRequest.builder()
                    .mediaAssetId(MEDIA_ASSET_ID)
                    .correlationId(CORRELATION)
                    .imageBytes(new byte[0])
                    .build();

            ImageStorageWriteResult result = adapter.write(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
        }

        @Test
        @DisplayName("Write null bytes — failure")
        void writeNullBytesFailure() {
            ImageStorageWriteRequest request = ImageStorageWriteRequest.builder()
                    .mediaAssetId(MEDIA_ASSET_ID)
                    .correlationId(CORRELATION)
                    .imageBytes(null)
                    .build();

            ImageStorageWriteResult result = adapter.write(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
        }

        @Test
        @DisplayName("Multiple writes produce unique keys")
        void multipleWritesUniqueKeys() {
            byte[] data = new byte[]{1, 2, 3};
            ImageStorageWriteResult r1 = adapter.write(validWriteRequest(data));
            ImageStorageWriteResult r2 = adapter.write(validWriteRequest(data));

            assertThat(r1.getStorageObjectKey()).isNotEqualTo(r2.getStorageObjectKey());
            assertThat(adapter.size()).isEqualTo(2);
        }
    }

    // ── Read Tests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Read operations")
    class ReadOperations {

        @Test
        @DisplayName("Read existing object — success")
        void readExistingObjectSuccess() {
            byte[] data = new byte[]{7, 8, 9};
            ImageStorageWriteResult writeResult = adapter.write(validWriteRequest(data));

            ImageStorageReadRequest readRequest = ImageStorageReadRequest.builder()
                    .storageObjectKey(writeResult.getStorageObjectKey())
                    .mediaAssetId(MEDIA_ASSET_ID)
                    .generatedAssetId(GENERATED_ASSET_ID)
                    .correlationId(CORRELATION)
                    .build();

            ImageStorageReadResult readResult = adapter.read(readRequest);

            assertThat(readResult.isSuccess()).isTrue();
            assertThat(readResult.getImageBytes()).isEqualTo(data);
            assertThat(readResult.getMimeType()).isEqualTo("image/png");
            assertThat(readResult.getFileSizeBytes()).isEqualTo(3);
            assertThat(readResult.getErrorCode()).isNull();
            assertThat(readResult.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Read missing object — safe error")
        void readMissingObjectSafeError() {
            ImageStorageReadRequest request = ImageStorageReadRequest.builder()
                    .storageObjectKey("nonexistent/key")
                    .mediaAssetId(MEDIA_ASSET_ID)
                    .correlationId(CORRELATION)
                    .build();

            ImageStorageReadResult result = adapter.read(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE);
            assertThat(result.getErrorMessage()).isNotNull();
            assertThat(result.getImageBytes()).isNull();
        }

        @Test
        @DisplayName("Read null request — safe error")
        void readNullRequestSafeError() {
            ImageStorageReadResult result = adapter.read(null);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
        }

        @Test
        @DisplayName("Read blank key — safe error")
        void readBlankKeySafeError() {
            ImageStorageReadRequest request = ImageStorageReadRequest.builder()
                    .storageObjectKey("")
                    .mediaAssetId(MEDIA_ASSET_ID)
                    .correlationId(CORRELATION)
                    .build();

            ImageStorageReadResult result = adapter.read(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
        }
    }

    // ── Provider Alias ──────────────────────────────────────────────

    @Nested
    @DisplayName("Provider identity")
    class ProviderIdentity {

        @Test
        @DisplayName("Provider alias is 'mock'")
        void providerAliasIsMock() {
            assertThat(adapter.getProviderAlias()).isEqualTo("mock");
        }
    }

    // ── Safety Guardrails ───────────────────────────────────────────

    @Nested
    @DisplayName("Safety guardrails")
    class SafetyGuardrails {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "publicUrl", "signedUrl", "cdnUrl",
                "providerUrl", "providerImageUrl", "providerTemporaryUrl",
                "base64", "imageBase64",
                "rawStorageProviderResponse", "rawProviderResponse",
                "apiKey", "secret", "internalFullPrompt",
                "readyForPublish", "publishStatus", "exportStatus"
        );

        @Test
        @DisplayName("WriteResult has no forbidden fields")
        void writeResultNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ImageStorageWriteResult.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("WriteResult should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("ReadResult has no forbidden fields")
        void readResultNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ImageStorageReadResult.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("ReadResult should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("WriteRequest has no forbidden fields")
        void writeRequestNoForbiddenFields() {
            Set<String> urlForbidden = Set.of(
                    "publicUrl", "signedUrl", "cdnUrl",
                    "providerUrl", "providerImageUrl", "providerTemporaryUrl",
                    "base64", "imageBase64", "storageObjectKey",
                    "rawProviderResponse", "rawStorageProviderResponse",
                    "apiKey", "secret", "internalFullPrompt",
                    "readyForPublish", "publishStatus", "exportStatus"
            );

            Set<String> fields = Arrays.stream(ImageStorageWriteRequest.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : urlForbidden) {
                assertThat(fields)
                        .as("WriteRequest should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Storage key not in error messages")
        void storageKeyNotInErrorMessages() {
            ImageStorageReadResult result = adapter.read(ImageStorageReadRequest.builder()
                    .storageObjectKey("secret/internal/key/path")
                    .mediaAssetId(MEDIA_ASSET_ID)
                    .correlationId(CORRELATION)
                    .build());

            assertThat(result.isSuccess()).isFalse();
            if (result.getErrorMessage() != null) {
                assertThat(result.getErrorMessage()).doesNotContain("secret/internal/key/path");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("storageObjectKey");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("objectKey");
            }
        }
    }

    // ── Clear / Size ────────────────────────────────────────────────

    @Nested
    @DisplayName("Utility methods")
    class UtilityMethods {

        @Test
        @DisplayName("Clear removes all objects")
        void clearRemovesAll() {
            adapter.write(validWriteRequest(new byte[]{1}));
            adapter.write(validWriteRequest(new byte[]{2}));
            assertThat(adapter.size()).isEqualTo(2);

            adapter.clear();
            assertThat(adapter.size()).isEqualTo(0);
        }
    }
}
