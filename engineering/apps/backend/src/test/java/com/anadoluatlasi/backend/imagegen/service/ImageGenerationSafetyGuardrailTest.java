package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.*;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-007 Task 11: Safe Error Handling and Observability guardrail testleri.
 *
 * <p>Bu testler compile-time ve runtime güvenlik garantileri sağlar:</p>
 * <ul>
 *   <li>DTO'larda forbidden field yok</li>
 *   <li>Error normalizer safe message üretir</li>
 *   <li>Error normalizer raw data sanitize eder</li>
 *   <li>Normalized error code'lar doğru mesaj üretir</li>
 * </ul>
 */
class ImageGenerationSafetyGuardrailTest {

    // ── Forbidden DTO Fields ────────────────────────────────────────

    /**
     * Admin-facing DTO'larda kesinlikle OLMAMASI gereken field isimleri.
     */
    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "providerRequestId",
            "rawProviderRequestPayload",
            "rawProviderResponsePayload",
            "internalFullPrompt",
            "internalPrompt",
            "apiKey",
            "secret",
            "signedUrl",
            "publicUrl",
            "cdnUrl",
            "storageObjectKey",
            "storageReference",
            "storageBucket",
            "storageProvider",
            "readyForPublish",
            "publishStatus",
            "exportStatus"
    );

    @Nested
    @DisplayName("DTO safety — forbidden fields")
    class DtoSafety {

        @Test
        @DisplayName("ImageGenerationRequestResponse forbidden field içermez")
        void requestResponseShouldNotHaveForbiddenFields() {
            assertNoForbiddenFields(ImageGenerationRequestResponse.class);
        }

        @Test
        @DisplayName("ImageGenerationResultResponse forbidden field içermez")
        void resultResponseShouldNotHaveForbiddenFields() {
            assertNoForbiddenFields(ImageGenerationResultResponse.class);
        }

        @Test
        @DisplayName("ImageGeneratedAssetResponse forbidden field içermez")
        void generatedAssetResponseShouldNotHaveForbiddenFields() {
            assertNoForbiddenFields(ImageGeneratedAssetResponse.class);
        }

        @Test
        @DisplayName("ImageMediaAssetResponse forbidden field içermez")
        void mediaAssetResponseShouldNotHaveForbiddenFields() {
            assertNoForbiddenFields(ImageMediaAssetResponse.class);
        }

        @Test
        @DisplayName("GeneratedAssetReviewDetailResponse forbidden field içermez")
        void reviewDetailResponseShouldNotHaveForbiddenFields() {
            assertNoForbiddenFields(GeneratedAssetReviewDetailResponse.class);
        }

        @Test
        @DisplayName("GeneratedAssetReviewResponse forbidden field içermez")
        void reviewResponseShouldNotHaveForbiddenFields() {
            assertNoForbiddenFields(GeneratedAssetReviewResponse.class);
        }
    }

    // ── Error Normalizer ────────────────────────────────────────────

    @Nested
    @DisplayName("ImageGenerationErrorNormalizer")
    class ErrorNormalizerTests {

        @Test
        @DisplayName("Her error code safe message üretir")
        void allErrorCodesShouldHaveSafeMessage() {
            for (ImageGenerationErrorCode code : ImageGenerationErrorCode.values()) {
                String message = ImageGenerationErrorNormalizer.safeMessage(code);
                assertThat(message).isNotBlank();
                assertThat(message).doesNotContainIgnoringCase("apiKey");
                assertThat(message).doesNotContainIgnoringCase("secret");
                assertThat(message).doesNotContainIgnoringCase("signedUrl");
                assertThat(message).doesNotContainIgnoringCase("publicUrl");
                assertThat(message).doesNotContainIgnoringCase("storageObjectKey");
                assertThat(message).doesNotContainIgnoringCase("internalPrompt");
            }
        }

        @Test
        @DisplayName("Null error code safe fallback döner")
        void nullCodeShouldReturnFallback() {
            String message = ImageGenerationErrorNormalizer.safeMessage(null);
            assertThat(message).isNotBlank();
        }

        @Test
        @DisplayName("UNKNOWN_GENERATION_FAILURE safe message üretir")
        void unknownFailureShouldProduceSafeMessage() {
            String message = ImageGenerationErrorNormalizer.safeMessage(
                    ImageGenerationErrorCode.UNKNOWN_GENERATION_FAILURE);
            assertThat(message).doesNotContain("Exception");
            assertThat(message).doesNotContain("stack");
            assertThat(message).doesNotContain("null");
        }

        @Test
        @DisplayName("Sanitize URL'leri maskeler")
        void shouldMaskUrls() {
            String sanitized = ImageGenerationErrorNormalizer.sanitize(
                    "Error connecting to https://api.provider.com/v1/generate");
            assertThat(sanitized).doesNotContain("https://");
            assertThat(sanitized).contains("[URL_REDACTED]");
        }

        @Test
        @DisplayName("Sanitize API key'leri maskeler")
        void shouldMaskApiKeys() {
            String sanitized = ImageGenerationErrorNormalizer.sanitize(
                    "Authentication failed: api_key=sk-abc123def456");
            assertThat(sanitized).doesNotContain("sk-abc123def456");
            assertThat(sanitized).contains("[REDACTED]");
        }

        @Test
        @DisplayName("Sanitize storage path'leri maskeler")
        void shouldMaskStoragePaths() {
            String sanitized = ImageGenerationErrorNormalizer.sanitize(
                    "Upload failed to s3://my-bucket/images/gen-123.png");
            assertThat(sanitized).doesNotContain("s3://");
            assertThat(sanitized).contains("[STORAGE_REDACTED]");
        }

        @Test
        @DisplayName("Sanitize base64 data'yı maskeler")
        void shouldMaskBase64Data() {
            String sanitized = ImageGenerationErrorNormalizer.sanitize(
                    "Invalid payload: base64:iVBORw0KGgoAAAANSUh...");
            assertThat(sanitized).doesNotContain("iVBORw0KGgo");
            assertThat(sanitized).contains("[DATA_REDACTED]");
        }

        @Test
        @DisplayName("Sanitize uzun mesajı truncate eder")
        void shouldTruncateLongMessage() {
            String longMessage = "A".repeat(500);
            String sanitized = ImageGenerationErrorNormalizer.sanitize(longMessage);
            assertThat(sanitized.length()).isLessThanOrEqualTo(201); // 200 + "…"
        }

        @Test
        @DisplayName("Sanitize null/blank safe fallback döner")
        void shouldHandleNullInput() {
            assertThat(ImageGenerationErrorNormalizer.sanitize(null)).isNotBlank();
            assertThat(ImageGenerationErrorNormalizer.sanitize("")).isNotBlank();
            assertThat(ImageGenerationErrorNormalizer.sanitize("   ")).isNotBlank();
        }

        @Test
        @DisplayName("Safe message forbidden term içermez")
        void safeMessageShouldNotContainForbiddenTerms() {
            List<String> forbidden = List.of(
                    "rawProvider", "providerPayload", "providerResponse",
                    "internalPrompt", "apiKey", "secret",
                    "signedUrl", "publicUrl", "storageObjectKey"
            );

            for (ImageGenerationErrorCode code : ImageGenerationErrorCode.values()) {
                String message = ImageGenerationErrorNormalizer.safeMessage(code);
                for (String term : forbidden) {
                    assertThat(message)
                            .as("Error code %s message should not contain '%s'", code, term)
                            .doesNotContainIgnoringCase(term);
                }
            }
        }
    }

    // ── Status Guards ───────────────────────────────────────────────

    @Nested
    @DisplayName("Status enum guardrails")
    class StatusGuards {

        @Test
        @DisplayName("ImageGenerationRequestStatus READY_FOR_PUBLISH içermez")
        void requestStatusShouldNotHaveReadyForPublish() {
            assertNoEnumValue(
                    com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequestStatus.class,
                    "READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("ImageGenerationRequestStatus COMPLETED içermez")
        void requestStatusShouldNotHaveCompleted() {
            assertNoEnumValue(
                    com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequestStatus.class,
                    "COMPLETED");
        }

        @Test
        @DisplayName("GeneratedAssetStatus READY_FOR_PUBLISH içermez")
        void generatedAssetStatusShouldNotHaveReadyForPublish() {
            assertNoEnumValue(
                    com.anadoluatlasi.backend.imagegen.entity.GeneratedAssetStatus.class,
                    "READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("GeneratedAssetStatus COMPLETED içermez")
        void generatedAssetStatusShouldNotHaveCompleted() {
            assertNoEnumValue(
                    com.anadoluatlasi.backend.imagegen.entity.GeneratedAssetStatus.class,
                    "COMPLETED");
        }
    }

    // ── Helper Methods ──────────────────────────────────────────────

    private void assertNoForbiddenFields(Class<?> clazz) {
        Set<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        for (String forbidden : FORBIDDEN_FIELDS) {
            assertThat(fieldNames)
                    .as("DTO %s should not have forbidden field '%s'", clazz.getSimpleName(), forbidden)
                    .doesNotContain(forbidden);
        }
    }

    private <E extends Enum<E>> void assertNoEnumValue(Class<E> enumClass, String forbiddenValue) {
        Set<String> values = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertThat(values)
                .as("Enum %s should not contain value '%s'", enumClass.getSimpleName(), forbiddenValue)
                .doesNotContain(forbiddenValue);
    }
}
