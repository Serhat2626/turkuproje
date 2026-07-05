package com.anadoluatlasi.backend.imagegen.storage;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FP-010 Task 03 — Real Private Storage Adapter Activation.
 *
 * All tests use mocked S3StorageClient — no real external calls.
 */
@DisplayName("FP-010 Task 03 — S3 Private Storage Adapter Activation")
class S3ImageStorageAdapterActivationTest {

    private S3PrivateStorageProperties props;
    private S3StorageClient mockClient;
    private S3ImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        props = new S3PrivateStorageProperties();
        props.setEnabled(true);
        props.setBucket("test-bucket");
        props.setEndpoint("https://s3.test.com");
        props.setRegion("eu-west-1");
        props.setAccessKey("AKIAFAKEKEY");
        props.setSecretKey("fakesecretkey");
        props.setPathPrefix("imagegen/");

        mockClient = mock(S3StorageClient.class);
        adapter = new S3ImageStorageAdapter(props, mockClient);
    }

    private ImageStorageWriteRequest validWriteRequest() {
        return ImageStorageWriteRequest.builder()
                .mediaAssetId(UUID.randomUUID())
                .generatedAssetId(UUID.randomUUID())
                .correlationId("test-corr-" + UUID.randomUUID())
                .mimeType("image/png")
                .width(1024)
                .height(1024)
                .fileSizeBytes(12345)
                .checksumSha256("abc123sha256")
                .imageBytes((new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x01}))
                .requestedAt(LocalDateTime.now())
                .build();
    }

    private ImageStorageReadRequest validReadRequest(String objectKey) {
        return ImageStorageReadRequest.builder()
                .storageObjectKey(objectKey)
                .mediaAssetId(UUID.randomUUID())
                .generatedAssetId(UUID.randomUUID())
                .correlationId("test-corr-" + UUID.randomUUID())
                .build();
    }

    // ── A. Config Gating ────────────────────────────────────────────

    @Nested
    @DisplayName("A. Config Gating")
    class ConfigGating {

        @Test
        @DisplayName("Disabled config returns safe write failure")
        void disabledConfig_writeFailure() {
            props.setEnabled(false);

            ImageStorageWriteResult result = adapter.write(validWriteRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
            assertThat(result.getErrorMessage()).doesNotContain("accessKey");
            assertThat(result.getErrorMessage()).doesNotContain("secret");
        }

        @Test
        @DisplayName("Missing bucket returns safe write failure")
        void missingBucket_writeFailure() {
            props.setBucket("");

            ImageStorageWriteResult result = adapter.write(validWriteRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
        }

        @Test
        @DisplayName("Missing accessKey returns safe write failure")
        void missingAccessKey_writeFailure() {
            props.setAccessKey("");

            ImageStorageWriteResult result = adapter.write(validWriteRequest());

            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Disabled config returns safe read failure")
        void disabledConfig_readFailure() {
            props.setEnabled(false);

            ImageStorageReadResult result = adapter.read(validReadRequest("imagegen/some/key"));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
        }

        @Test
        @DisplayName("Config error messages never contain secrets")
        void configErrors_neverContainSecrets() {
            props.setAccessKey("");

            ImageStorageWriteResult result = adapter.write(validWriteRequest());

            assertThat(result.getErrorMessage()).doesNotContain("AKIAFAKEKEY");
            assertThat(result.getErrorMessage()).doesNotContain("fakesecretkey");
            assertThat(result.getErrorMessage()).doesNotContain("accessKey");
            assertThat(result.getErrorMessage()).doesNotContain("secretKey");
        }
    }

    // ── B. Write Success ────────────────────────────────────────────

    @Nested
    @DisplayName("B. Write Success")
    class WriteSuccess {

        @Test
        @DisplayName("Valid write returns success with internal key")
        void validWrite_success() throws Exception {
            ImageStorageWriteRequest req = validWriteRequest();

            ImageStorageWriteResult result = adapter.write(req);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStorageProviderAlias()).isEqualTo("s3");
            assertThat(result.getStorageObjectKey()).startsWith("imagegen/");
            assertThat(result.getMimeType()).isEqualTo("image/png");
            assertThat(result.getWidth()).isEqualTo(1024);
            assertThat(result.getHeight()).isEqualTo(1024);
            assertThat(result.getFileSizeBytes()).isEqualTo(12345);
            assertThat(result.getStoredAt()).isNotNull();

            verify(mockClient).putObject(eq("test-bucket"), anyString(), any(byte[].class), eq("image/png"));
        }

        @Test
        @DisplayName("Write result has no public URL")
        void writeResult_noPublicUrl() throws Exception {
            ImageStorageWriteResult result = adapter.write(validWriteRequest());
            String str = result.toString();

            assertThat(str).doesNotContain("publicUrl");
            assertThat(str).doesNotContain("signedUrl");
            assertThat(str).doesNotContain("cdnUrl");
        }

        @Test
        @DisplayName("Write result has no provider URL or base64")
        void writeResult_noProviderUrlOrBase64() throws Exception {
            ImageStorageWriteResult result = adapter.write(validWriteRequest());
            String str = result.toString();

            assertThat(str).doesNotContain("providerUrl");
            assertThat(str).doesNotContain("base64");
        }

        @Test
        @DisplayName("putObject called with correct bucket")
        void putObject_correctBucket() throws Exception {
            adapter.write(validWriteRequest());

            verify(mockClient).putObject(eq("test-bucket"), anyString(), any(), any());
        }
    }

    // ── C. Write Failure ────────────────────────────────────────────

    @Nested
    @DisplayName("C. Write Failure")
    class WriteFailure {

        @Test
        @DisplayName("SDK exception returns safe STORAGE_WRITE_FAILED")
        void sdkException_safeFailure() throws Exception {
            doThrow(new S3StorageClient.S3StorageClientException("SDK error"))
                    .when(mockClient).putObject(any(), any(), any(), any());

            ImageStorageWriteResult result = adapter.write(validWriteRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
            assertThat(result.getErrorMessage()).doesNotContain("SDK");
            assertThat(result.getErrorMessage()).doesNotContain("test-bucket");
        }

        @Test
        @DisplayName("Unexpected exception returns safe failure")
        void unexpectedException_safeFailure() throws Exception {
            doThrow(new RuntimeException("something with bucket=my-secret-bucket"))
                    .when(mockClient).putObject(any(), any(), any(), any());

            ImageStorageWriteResult result = adapter.write(validWriteRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContain("my-secret-bucket");
            assertThat(result.getErrorMessage()).doesNotContain("RuntimeException");
        }

        @Test
        @DisplayName("Null request returns safe failure")
        void nullRequest_safeFailure() {
            ImageStorageWriteResult result = adapter.write(null);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_WRITE_FAILED);
        }

        @Test
        @DisplayName("Empty bytes returns safe failure")
        void emptyBytes_safeFailure() {
            ImageStorageWriteRequest req = ImageStorageWriteRequest.builder()
                    .mediaAssetId(UUID.randomUUID())
                    .generatedAssetId(UUID.randomUUID())
                    .imageBytes(new byte[0])
                    .build();

            ImageStorageWriteResult result = adapter.write(req);

            assertThat(result.isSuccess()).isFalse();
        }
    }

    // ── D. Read Success ─────────────────────────────────────────────

    @Nested
    @DisplayName("D. Read Success")
    class ReadSuccess {

        @Test
        @DisplayName("Valid read returns success with bytes")
        void validRead_success() throws Exception {
            byte[] storedBytes = (new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x01});
            when(mockClient.getObject(eq("test-bucket"), eq("imagegen/asset/gen")))
                    .thenReturn(storedBytes);

            ImageStorageReadResult result = adapter.read(validReadRequest("imagegen/asset/gen"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).isEqualTo(storedBytes);
            assertThat(result.getFileSizeBytes()).isEqualTo(storedBytes.length);
        }

        @Test
        @DisplayName("Read result has no public/signed/redirect URL")
        void readResult_noUrlFields() throws Exception {
            when(mockClient.getObject(any(), any()))
                    .thenReturn((new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x01}));

            ImageStorageReadResult result = adapter.read(validReadRequest("imagegen/test/key"));
            String str = result.toString();

            assertThat(str).doesNotContain("publicUrl");
            assertThat(str).doesNotContain("signedUrl");
            assertThat(str).doesNotContain("redirectUrl");
        }
    }

    // ── E. Read Failure ─────────────────────────────────────────────

    @Nested
    @DisplayName("E. Read Failure")
    class ReadFailure {

        @Test
        @DisplayName("Missing key returns safe failure")
        void missingKey_safeFailure() {
            ImageStorageReadResult result = adapter.read(validReadRequest(""));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
        }

        @Test
        @DisplayName("Null request returns safe failure")
        void nullRequest_safeFailure() {
            ImageStorageReadResult result = adapter.read(null);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
        }

        @Test
        @DisplayName("SDK exception returns safe STORAGE_READ_FAILED")
        void sdkException_safeFailure() throws Exception {
            doThrow(new S3StorageClient.S3StorageClientException("read error"))
                    .when(mockClient).getObject(any(), any());

            ImageStorageReadResult result = adapter.read(validReadRequest("imagegen/test/key"));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.STORAGE_READ_FAILED);
            assertThat(result.getErrorMessage()).doesNotContain("read error");
        }

        @Test
        @DisplayName("Empty bytes from storage returns PREVIEW_NOT_AVAILABLE")
        void emptyBytes_previewNotAvailable() throws Exception {
            when(mockClient.getObject(any(), any())).thenReturn(new byte[0]);

            ImageStorageReadResult result = adapter.read(validReadRequest("imagegen/test/key"));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("Read error does not include bucket/key/secret")
        void readError_noSecrets() throws Exception {
            doThrow(new RuntimeException("bucket=my-secret-bucket key=some/path accessKey=AKIA"))
                    .when(mockClient).getObject(any(), any());

            ImageStorageReadResult result = adapter.read(validReadRequest("imagegen/test/key"));

            assertThat(result.getErrorMessage()).doesNotContain("my-secret-bucket");
            assertThat(result.getErrorMessage()).doesNotContain("some/path");
            assertThat(result.getErrorMessage()).doesNotContain("AKIA");
        }
    }

    // ── F. Object Key Safety ────────────────────────────────────────

    @Nested
    @DisplayName("F. Object Key Safety")
    class ObjectKeySafety {

        @Test
        @DisplayName("Generated object key uses path prefix + asset IDs")
        void objectKey_usesAssetIds() {
            ImageStorageWriteRequest req = validWriteRequest();
            String key = adapter.generateObjectKey(req);

            assertThat(key).startsWith("imagegen/");
            assertThat(key).contains(req.getMediaAssetId().toString());
        }

        @Test
        @DisplayName("Object key does not contain provider URL")
        void objectKey_noProviderUrl() {
            String key = adapter.generateObjectKey(validWriteRequest());
            assertThat(key).doesNotContain("https://");
            assertThat(key).doesNotContain("openai");
            assertThat(key).doesNotContain("oaidalleapiprodscus");
        }

        @Test
        @DisplayName("Object key does not contain secrets")
        void objectKey_noSecrets() {
            String key = adapter.generateObjectKey(validWriteRequest());
            assertThat(key).doesNotContain("AKIAFAKEKEY");
            assertThat(key).doesNotContain("fakesecretkey");
        }

        @Test
        @DisplayName("Object key handles null mediaAssetId safely")
        void objectKey_nullAssetId() {
            ImageStorageWriteRequest req = ImageStorageWriteRequest.builder()
                    .mediaAssetId(null)
                    .generatedAssetId(UUID.randomUUID())
                    .imageBytes(new byte[]{1})
                    .build();
            String key = adapter.generateObjectKey(req);
            assertThat(key).startsWith("imagegen/unknown/");
        }
    }

    // ── G. Bean Selection / Provider Alias ───────────────────────────

    @Nested
    @DisplayName("G. Bean Selection & Provider")
    class BeanSelection {

        @Test
        @DisplayName("Provider alias is 's3'")
        void providerAlias_isS3() {
            assertThat(adapter.getProviderAlias()).isEqualTo("s3");
        }

        @Test
        @DisplayName("InMemoryImageStorageAdapter has different alias")
        void inMemory_differentAlias() {
            InMemoryImageStorageAdapter inMemory = new InMemoryImageStorageAdapter();
            assertThat(inMemory.getProviderAlias()).isEqualTo("mock");
            assertThat(inMemory.getProviderAlias()).isNotEqualTo(adapter.getProviderAlias());
        }

        @Test
        @DisplayName("S3ImageStorageAdapter implements ImageStorageAdapter")
        void implementsInterface() {
            assertThat(ImageStorageAdapter.class).isAssignableFrom(S3ImageStorageAdapter.class);
        }

        @Test
        @DisplayName("InMemoryImageStorageAdapter still works standalone")
        void inMemory_stillWorks() {
            InMemoryImageStorageAdapter inMemory = new InMemoryImageStorageAdapter();
            ImageStorageWriteResult result = inMemory.write(validWriteRequest());
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ── H. S3StorageClient Abstraction Safety ───────────────────────

    @Nested
    @DisplayName("H. S3StorageClient Safety")
    class ClientSafety {

        @Test
        @DisplayName("S3StorageClientException toString does not expose raw cause")
        void exceptionToString_safe() {
            S3StorageClient.S3StorageClientException ex =
                    new S3StorageClient.S3StorageClientException("safe msg",
                            new RuntimeException("bucket=secret key=path/to/secret"));

            String str = ex.toString();
            assertThat(str).doesNotContain("bucket=secret");
            assertThat(str).doesNotContain("path/to/secret");
            assertThat(str).contains("safe msg");
        }

        @Test
        @DisplayName("S3StorageClientException getSafeMessage returns safe text")
        void exceptionSafeMessage() {
            S3StorageClient.S3StorageClientException ex =
                    new S3StorageClient.S3StorageClientException("storage write failed");
            assertThat(ex.getSafeMessage()).isEqualTo("storage write failed");
        }
    }

    // ── I. Regression ───────────────────────────────────────────────

    @Nested
    @DisplayName("I. Regression")
    class Regression {

        @Test
        @DisplayName("No READY_FOR_PUBLISH in write result")
        void noReadyForPublish() throws Exception {
            ImageStorageWriteResult result = adapter.write(validWriteRequest());
            assertThat(result.toString()).doesNotContainIgnoringCase("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("No COMPLETED in write result")
        void noCompleted() throws Exception {
            ImageStorageWriteResult result = adapter.write(validWriteRequest());
            assertThat(result.toString()).doesNotContainIgnoringCase("COMPLETED");
        }

        @Test
        @DisplayName("No APPROVED in write result")
        void noApproved() throws Exception {
            ImageStorageWriteResult result = adapter.write(validWriteRequest());
            assertThat(result.toString()).doesNotContainIgnoringCase("APPROVED");
        }

        @Test
        @DisplayName("No publish/export fields in result")
        void noPublishExport() {
            java.util.Set<String> fields = java.util.Arrays.stream(
                    ImageStorageWriteResult.class.getDeclaredFields())
                    .map(java.lang.reflect.Field::getName)
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(fields).doesNotContain("publishStatus", "exportStatus", "publishUrl");
        }
    }
}
