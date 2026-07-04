package com.anadoluatlasi.backend.imagegen.controller.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-008 Task 09: Storage handoff DTO and API contract guardrail tests.
 *
 * <p>DTO'larda forbidden field'ların olmadığını ve safe field'ların
 * mevcut olduğunu doğrular.</p>
 */
class StorageHandoffDtoGuardrailTest {

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "storageObjectKey", "storageReference",
            "storageBucket", "storageProvider", "storageProviderAlias",
            "publicUrl", "signedUrl", "cdnUrl",
            "providerUrl", "providerImageUrl", "providerTemporaryUrl",
            "base64", "imageBase64", "imageBinary", "rawBytes",
            "rawProviderPayload", "rawProviderResponse",
            "rawStorageProviderResponse",
            "apiKey", "secret", "internalFullPrompt", "internalPrompt",
            "readyForPublish", "publishStatus", "exportStatus"
    );

    // ── StorageHandoffResponse ──────────────────────────────────────

    @Nested
    @DisplayName("StorageHandoffResponse guardrails")
    class HandoffResponseGuardrails {

        @Test
        @DisplayName("No forbidden fields in StorageHandoffResponse")
        void noForbiddenFields() {
            Set<String> fields = fieldNames(StorageHandoffResponse.class);
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("StorageHandoffResponse should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Has required safe fields")
        void hasRequiredSafeFields() {
            Set<String> fields = fieldNames(StorageHandoffResponse.class);
            assertThat(fields).contains(
                    "success", "mediaAssetId", "generatedAssetId",
                    "storageHandoffStatus", "previewStatus",
                    "errorCode", "errorMessage", "correlationId"
            );
        }
    }

    // ── ImageMediaAssetResponse ──────────────────────────────────────

    @Nested
    @DisplayName("ImageMediaAssetResponse guardrails")
    class MediaAssetResponseGuardrails {

        @Test
        @DisplayName("No forbidden fields in ImageMediaAssetResponse")
        void noForbiddenFields() {
            Set<String> fields = fieldNames(ImageMediaAssetResponse.class);
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("ImageMediaAssetResponse should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Has FP-008 safe storage status fields")
        void hasSafeStorageStatusFields() {
            Set<String> fields = fieldNames(ImageMediaAssetResponse.class);
            assertThat(fields).contains(
                    "storageHandoffStatus", "previewStatus",
                    "storageErrorCode", "storageErrorMessage", "storedAt"
            );
        }

        @Test
        @DisplayName("Still has original FP-007 fields")
        void hasOriginalFields() {
            Set<String> fields = fieldNames(ImageMediaAssetResponse.class);
            assertThat(fields).contains(
                    "id", "assetKind", "mimeType",
                    "width", "height", "fileSizeBytes",
                    "checksumSha256", "reviewStatus"
            );
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private Set<String> fieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
