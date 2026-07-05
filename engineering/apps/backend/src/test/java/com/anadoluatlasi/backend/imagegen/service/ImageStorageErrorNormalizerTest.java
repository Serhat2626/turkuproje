package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-008 Task 11: ImageStorageErrorNormalizer unit tests and
 * cross-cutting observability guardrails.
 */
class ImageStorageErrorNormalizerTest {

    // ── Safe Message for All Error Codes ─────────────────────────────

    @Nested
    @DisplayName("Safe message mapping")
    class SafeMessageMapping {

        @ParameterizedTest
        @EnumSource(ImageStorageErrorCode.class)
        @DisplayName("Every error code has a non-null safe message")
        void everyCodeHasSafeMessage(ImageStorageErrorCode code) {
            String msg = ImageStorageErrorNormalizer.safeMessage(code);
            assertThat(msg).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Null error code returns fallback")
        void nullCodeFallback() {
            String msg = ImageStorageErrorNormalizer.safeMessage((ImageStorageErrorCode) null);
            assertThat(msg).isNotNull().isNotBlank();
        }

        @ParameterizedTest
        @EnumSource(ImageStorageErrorCode.class)
        @DisplayName("Safe messages do not contain URLs or secrets")
        void safeMessagesNoInternalDetails(ImageStorageErrorCode code) {
            String msg = ImageStorageErrorNormalizer.safeMessage(code);
            assertThat(msg).doesNotContainIgnoringCase("http");
            assertThat(msg).doesNotContainIgnoringCase("s3://");
            assertThat(msg).doesNotContainIgnoringCase("gs://");
            assertThat(msg).doesNotContainIgnoringCase("storageObjectKey");
            assertThat(msg).doesNotContainIgnoringCase("apiKey");
            assertThat(msg).doesNotContainIgnoringCase("secret");
            assertThat(msg).doesNotContainIgnoringCase("base64");
        }
    }

    // ── Sanitization ────────────────────────────────────────────────

    @Nested
    @DisplayName("Sanitization")
    class Sanitization {

        @Test
        @DisplayName("Masks HTTP URLs")
        void masksHttpUrls() {
            String raw = "Failed to connect to https://api.openai.com/v1/images";
            String sanitized = ImageStorageErrorNormalizer.sanitize(raw);
            assertThat(sanitized).doesNotContain("https://");
            assertThat(sanitized).doesNotContain("openai.com");
            assertThat(sanitized).contains("[URL_REDACTED]");
        }

        @Test
        @DisplayName("Masks storage URIs")
        void masksStorageUris() {
            String raw = "Failed to read from s3://my-bucket/images/key.png and gs://other/file";
            String sanitized = ImageStorageErrorNormalizer.sanitize(raw);
            assertThat(sanitized).doesNotContain("s3://");
            assertThat(sanitized).doesNotContain("gs://");
            assertThat(sanitized).contains("[STORAGE_REDACTED]");
        }

        @Test
        @DisplayName("Masks storage object key patterns")
        void masksStorageObjectKey() {
            String raw = "Key not found: imagegen/550e8400-e29b-41d4-a716-446655440000/a1b2c3d4-e5f6-7890-abcd-ef1234567890";
            String sanitized = ImageStorageErrorNormalizer.sanitize(raw);
            assertThat(sanitized).doesNotContain("imagegen/550e8400");
            assertThat(sanitized).contains("[STORAGE_KEY_REDACTED]");
        }

        @Test
        @DisplayName("Masks API keys and secrets")
        void masksApiKeysAndSecrets() {
            String raw = "api_key=sk-abc123def456 and secret:mysecretvalue";
            String sanitized = ImageStorageErrorNormalizer.sanitize(raw);
            assertThat(sanitized).doesNotContain("sk-abc123");
            assertThat(sanitized).doesNotContain("mysecretvalue");
            assertThat(sanitized).contains("[REDACTED]");
        }

        @Test
        @DisplayName("Masks base64 data patterns")
        void masksBase64Patterns() {
            String raw = "Data: base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAA";
            String sanitized = ImageStorageErrorNormalizer.sanitize(raw);
            assertThat(sanitized).doesNotContain("iVBORw0");
            assertThat(sanitized).contains("[DATA_REDACTED]");
        }

        @Test
        @DisplayName("Masks long base64-like strings")
        void masksLongBase64Strings() {
            String longData = "A".repeat(100);
            String raw = "Response body: " + longData;
            String sanitized = ImageStorageErrorNormalizer.sanitize(raw);
            assertThat(sanitized).doesNotContain(longData);
            assertThat(sanitized).contains("[LONG_DATA_REDACTED]");
        }

        @Test
        @DisplayName("Truncates excessively long messages")
        void truncatesLongMessages() {
            String raw = "X".repeat(500);
            String sanitized = ImageStorageErrorNormalizer.sanitize(raw);
            assertThat(sanitized.length()).isLessThanOrEqualTo(201); // 200 + "…"
        }

        @Test
        @DisplayName("Null input returns safe fallback")
        void nullInputFallback() {
            assertThat(ImageStorageErrorNormalizer.sanitize(null)).isNotBlank();
        }

        @Test
        @DisplayName("Blank input returns safe fallback")
        void blankInputFallback() {
            assertThat(ImageStorageErrorNormalizer.sanitize("  ")).isNotBlank();
        }

        @Test
        @DisplayName("Clean message passes through unchanged")
        void cleanMessagePassthrough() {
            String clean = "Disk full error";
            assertThat(ImageStorageErrorNormalizer.sanitize(clean)).isEqualTo(clean);
        }
    }

    // ── safeMessage with context ────────────────────────────────────

    @Nested
    @DisplayName("Safe message with context")
    class SafeMessageWithContext {

        @Test
        @DisplayName("Context appended to base message")
        void contextAppended() {
            String result = ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.STORAGE_WRITE_FAILED, "Disk full");
            assertThat(result).contains("Depolama yazma");
            assertThat(result).contains("Disk full");
        }

        @Test
        @DisplayName("Context with URL is sanitized")
        void contextUrlSanitized() {
            String result = ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.STORAGE_READ_FAILED,
                    "Cannot read from https://s3.amazonaws.com/bucket/key");
            assertThat(result).doesNotContain("https://");
            assertThat(result).doesNotContain("amazonaws");
            assertThat(result).contains("[URL_REDACTED]");
        }

        @Test
        @DisplayName("Null context returns base message only")
        void nullContextBaseOnly() {
            String result = ImageStorageErrorNormalizer.safeMessage(
                    ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE, null);
            assertThat(result).isEqualTo("Önizleme mevcut değil");
        }
    }

    // ── safeLogContext ───────────────────────────────────────────────

    @Nested
    @DisplayName("Safe log context")
    class SafeLogContext {

        @Test
        @DisplayName("Includes error code and correlationId")
        void includesCodeAndCorrelation() {
            String ctx = ImageStorageErrorNormalizer.safeLogContext(
                    ImageStorageErrorCode.STORAGE_WRITE_FAILED, "corr-123");
            assertThat(ctx).contains("STORAGE_WRITE_FAILED");
            assertThat(ctx).contains("corr-123");
        }

        @Test
        @DisplayName("Null correlationId shows 'none'")
        void nullCorrelationId() {
            String ctx = ImageStorageErrorNormalizer.safeLogContext(
                    ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE, null);
            assertThat(ctx).contains("none");
        }

        @Test
        @DisplayName("Null errorCode shows 'UNKNOWN'")
        void nullErrorCode() {
            String ctx = ImageStorageErrorNormalizer.safeLogContext(null, "corr-456");
            assertThat(ctx).contains("UNKNOWN");
        }
    }

    // ── Cross-cutting DTO/Result Model Guardrails ───────────────────

    @Nested
    @DisplayName("Cross-cutting model guardrails")
    class CrossCuttingGuardrails {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "storageObjectKey", "storageReference",
                "storageBucket", "storageProvider", "storageProviderAlias",
                "publicUrl", "signedUrl", "cdnUrl",
                "providerUrl", "providerImageUrl", "providerTemporaryUrl",
                "base64", "imageBase64", "rawBytes",
                "rawProviderPayload", "rawProviderResponse",
                "rawStorageProviderResponse",
                "apiKey", "secret", "internalFullPrompt", "internalPrompt",
                "readyForPublish", "publishStatus", "exportStatus"
        );

        @Test
        @DisplayName("HandoffResult no forbidden fields")
        void handoffResultClean() {
            assertNoForbiddenFields(ImageStorageHandoffResult.class);
        }

        @Test
        @DisplayName("HandoffRequest no forbidden fields")
        void handoffRequestClean() {
            Set<String> extra = Set.of(
                    "storageObjectKey", "providerUrl", "signedUrl",
                    "publicUrl", "cdnUrl", "rawProviderPayload",
                    "rawProviderResponse", "apiKey", "secret",
                    "internalFullPrompt", "readyForPublish", "publishStatus", "exportStatus"
            );
            assertNoForbiddenFields(ImageStorageHandoffRequest.class, extra);
        }

        @Test
        @DisplayName("PreviewProxyResult no forbidden fields")
        void previewResultClean() {
            assertNoForbiddenFields(ImagePreviewProxyResult.class);
        }

        private void assertNoForbiddenFields(Class<?> clazz) {
            assertNoForbiddenFields(clazz, FORBIDDEN_FIELDS);
        }

        private void assertNoForbiddenFields(Class<?> clazz, Set<String> forbidden) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());
            for (String f : forbidden) {
                assertThat(fields)
                        .as("%s should not have '%s'", clazz.getSimpleName(), f)
                        .doesNotContain(f);
            }
        }
    }

    // ── CorrelationId Propagation ────────────────────────────────────

    @Nested
    @DisplayName("CorrelationId field presence")
    class CorrelationIdPresence {

        @Test
        @DisplayName("HandoffResult has correlationId")
        void handoffResultCorrelation() {
            assertHasField(ImageStorageHandoffResult.class, "correlationId");
        }

        @Test
        @DisplayName("HandoffRequest has correlationId")
        void handoffRequestCorrelation() {
            assertHasField(ImageStorageHandoffRequest.class, "correlationId");
        }

        @Test
        @DisplayName("PreviewProxyResult has correlationId")
        void previewResultCorrelation() {
            assertHasField(ImagePreviewProxyResult.class, "correlationId");
        }

        private void assertHasField(Class<?> clazz, String fieldName) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());
            assertThat(fields)
                    .as("%s should have '%s'", clazz.getSimpleName(), fieldName)
                    .contains(fieldName);
        }
    }
}
