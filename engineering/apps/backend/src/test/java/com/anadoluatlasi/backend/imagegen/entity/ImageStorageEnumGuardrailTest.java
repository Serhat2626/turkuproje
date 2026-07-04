package com.anadoluatlasi.backend.imagegen.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-008 Task 04: Storage/preview enum ve entity skeleton guardrail testleri.
 *
 * <p>Enum'larda ve entity'de forbidden kavramların olmadığını doğrular.</p>
 */
class ImageStorageEnumGuardrailTest {

    // ── ImageStorageHandoffStatus ────────────────────────────────────

    @Nested
    @DisplayName("ImageStorageHandoffStatus guardrails")
    class StorageHandoffStatusGuardrails {

        @Test
        @DisplayName("READY_FOR_PUBLISH yok")
        void shouldNotContainReadyForPublish() {
            Set<String> values = enumValues(ImageStorageHandoffStatus.class);
            assertThat(values).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("COMPLETED yok")
        void shouldNotContainCompleted() {
            Set<String> values = enumValues(ImageStorageHandoffStatus.class);
            assertThat(values).doesNotContain("COMPLETED");
        }

        @Test
        @DisplayName("APPROVED yok — storage status review status değildir")
        void shouldNotContainApproved() {
            Set<String> values = enumValues(ImageStorageHandoffStatus.class);
            assertThat(values).doesNotContain("APPROVED");
        }

        @Test
        @DisplayName("Beklenen 4 değer mevcut")
        void shouldHaveExpectedValues() {
            Set<String> values = enumValues(ImageStorageHandoffStatus.class);
            assertThat(values).containsExactlyInAnyOrder(
                    "STORAGE_PENDING",
                    "STORAGE_VALIDATING",
                    "STORAGE_STORED",
                    "STORAGE_FAILED"
            );
        }
    }

    // ── ImagePreviewStatus ──────────────────────────────────────────

    @Nested
    @DisplayName("ImagePreviewStatus guardrails")
    class PreviewStatusGuardrails {

        @Test
        @DisplayName("READY_FOR_PUBLISH yok")
        void shouldNotContainReadyForPublish() {
            Set<String> values = enumValues(ImagePreviewStatus.class);
            assertThat(values).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("COMPLETED yok")
        void shouldNotContainCompleted() {
            Set<String> values = enumValues(ImagePreviewStatus.class);
            assertThat(values).doesNotContain("COMPLETED");
        }

        @Test
        @DisplayName("APPROVED yok — preview status approval değildir")
        void shouldNotContainApproved() {
            Set<String> values = enumValues(ImagePreviewStatus.class);
            assertThat(values).doesNotContain("APPROVED");
        }

        @Test
        @DisplayName("Beklenen 2 değer mevcut")
        void shouldHaveExpectedValues() {
            Set<String> values = enumValues(ImagePreviewStatus.class);
            assertThat(values).containsExactlyInAnyOrder(
                    "PREVIEW_UNAVAILABLE",
                    "PREVIEW_AVAILABLE"
            );
        }
    }

    // ── ImageStorageErrorCode ────────────────────────────────────────

    @Nested
    @DisplayName("ImageStorageErrorCode guardrails")
    class StorageErrorCodeGuardrails {

        @Test
        @DisplayName("13 error code mevcut")
        void shouldHave13ErrorCodes() {
            assertThat(ImageStorageErrorCode.values()).hasSize(13);
        }

        @Test
        @DisplayName("Temel error code'lar mevcut")
        void shouldContainCoreErrorCodes() {
            Set<String> values = enumValues(ImageStorageErrorCode.class);
            assertThat(values).contains(
                    "PROVIDER_OUTPUT_UNAVAILABLE",
                    "UNSUPPORTED_MEDIA_TYPE",
                    "MEDIA_TOO_LARGE",
                    "STORAGE_WRITE_FAILED",
                    "STORAGE_READ_FAILED",
                    "PREVIEW_NOT_AVAILABLE",
                    "PREVIEW_ACCESS_DENIED",
                    "UNKNOWN_STORAGE_HANDOFF_FAILURE"
            );
        }
    }

    // ── ImageMediaAsset Entity Forbidden Fields ─────────────────────

    @Nested
    @DisplayName("ImageMediaAsset forbidden fields")
    class MediaAssetForbiddenFields {

        private static final Set<String> FORBIDDEN = Set.of(
                "providerImageUrl", "providerUrl", "providerTemporaryUrl",
                "signedUrl", "publicUrl", "cdnUrl",
                "base64", "imageBase64", "imageBinary",
                "rawProviderPayload", "rawProviderResponse", "rawStorageProviderResponse",
                "apiKey", "secret", "internalFullPrompt",
                "readyForPublish", "publishStatus", "completedStatus",
                "exportStatus"
        );

        @Test
        @DisplayName("Forbidden field'lar entity'de yok")
        void shouldNotHaveForbiddenFields() {
            Set<String> fields = Arrays.stream(ImageMediaAsset.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN) {
                assertThat(fields)
                        .as("ImageMediaAsset should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private <E extends Enum<E>> Set<String> enumValues(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}
