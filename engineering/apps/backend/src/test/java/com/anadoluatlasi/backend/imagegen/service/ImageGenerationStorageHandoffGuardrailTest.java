package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageMediaAssetResponse;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-007 Task 13: Storage handoff güvenlik guardrail testleri.
 *
 * <p>Provider output'un, DTO'ların ve entity'lerin storage-related
 * forbidden field içermediğini compile-time yakın güvenceyle doğrular.</p>
 *
 * <p>Bu testler storage implementation yapılmadan önce yasak alanların
 * eklenmesini erken tespit eder.</p>
 */
class ImageGenerationStorageHandoffGuardrailTest {

    /**
     * Provider result model'de olmaması gereken storage/URL/binary field'lar.
     */
    private static final Set<String> PROVIDER_RESULT_FORBIDDEN = Set.of(
            "imageUrl",
            "imageBase64",
            "imageBinary",
            "signedUrl",
            "publicUrl",
            "cdnUrl",
            "downloadUrl",
            "storageObjectKey",
            "storageReference",
            "storageBucket",
            "storageProvider",
            "rawProviderResponse",
            "rawProviderRequest",
            "rawProviderPayload",
            "internalFullPrompt",
            "internalPrompt",
            "rawPrompt",
            "apiKey",
            "secret"
    );

    /**
     * MediaAsset response DTO'da olmaması gereken field'lar.
     */
    private static final Set<String> MEDIA_ASSET_RESPONSE_FORBIDDEN = Set.of(
            "storageObjectKey",
            "storageReference",
            "storageBucket",
            "storageProvider",
            "storageProviderAlias",
            "signedUrl",
            "publicUrl",
            "cdnUrl",
            "downloadUrl",
            "imageUrl",
            "imageBase64",
            "imageBinary",
            "rawProviderResponse",
            "rawProviderRequest",
            "internalFullPrompt",
            "internalPrompt",
            "apiKey",
            "secret",
            "readyForPublish",
            "publishStatus",
            "exportStatus"
    );

    // ── Provider Result Model ───────────────────────────────────────

    @Nested
    @DisplayName("ImageProviderGenerationResult — storage/URL/binary field yok")
    class ProviderResultGuardrails {

        @Test
        @DisplayName("Provider result URL/base64/storage field içermez")
        void shouldNotHaveForbiddenStorageFields() {
            Set<String> fields = getFieldNames(ImageProviderGenerationResult.class);

            for (String forbidden : PROVIDER_RESULT_FORBIDDEN) {
                assertThat(fields)
                        .as("ImageProviderGenerationResult should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Provider result allowlist dışı field yok")
        void shouldOnlyHaveAllowlistedFields() {
            Set<String> allowlist = Set.of(
                    "success", "providerAlias", "modelName", "outputCount",
                    "generationDurationMs", "mimeType", "width", "height",
                    "fileSizeBytes", "checksumSha256", "errorCode", "errorMessage",
                    "correlationId", "completedAt", "qualitySignalsSummary",
                    "safetyFlagsSummary"
            );

            Set<String> actualFields = getFieldNames(ImageProviderGenerationResult.class);

            for (String actual : actualFields) {
                assertThat(allowlist)
                        .as("Field '%s' not in provider result allowlist", actual)
                        .contains(actual);
            }
        }
    }

    // ── MediaAsset Response DTO ─────────────────────────────────────

    @Nested
    @DisplayName("ImageMediaAssetResponse — storage/URL forbidden field yok")
    class MediaAssetResponseGuardrails {

        @Test
        @DisplayName("MediaAsset response storage/URL/internal field içermez")
        void shouldNotHaveForbiddenFields() {
            Set<String> fields = getFieldNames(ImageMediaAssetResponse.class);

            for (String forbidden : MEDIA_ASSET_RESPONSE_FORBIDDEN) {
                assertThat(fields)
                        .as("ImageMediaAssetResponse should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    // ── Provider Config Safety ──────────────────────────────────────

    @Nested
    @DisplayName("Provider config safety")
    class ProviderConfigSafety {

        @Test
        @DisplayName("ImageProviderGenerationRequest storage field içermez")
        void requestShouldNotHaveStorageFields() {
            Set<String> forbidden = Set.of(
                    "storageObjectKey", "storageBucket", "storageReference",
                    "imageUrl", "imageBase64", "signedUrl", "publicUrl",
                    "apiKey", "secret", "internalFullPrompt"
            );

            Set<String> fields = getFieldNames(
                    com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationRequest.class);

            for (String f : forbidden) {
                assertThat(fields)
                        .as("ImageProviderGenerationRequest should not have '%s'", f)
                        .doesNotContain(f);
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
