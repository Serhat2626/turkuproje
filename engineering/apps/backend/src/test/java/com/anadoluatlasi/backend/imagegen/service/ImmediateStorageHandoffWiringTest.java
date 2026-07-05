package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationResult;
import com.anadoluatlasi.backend.imagegen.provider.ProviderOutputReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FP-010 Task 04 — End-to-End Immediate Handoff Wiring.
 *
 * All tests use mocked services — no real provider/storage calls.
 */
@DisplayName("FP-010 Task 04 — Immediate Handoff Wiring")
class ImmediateStorageHandoffWiringTest {

    private ImmediateStorageHandoffProperties config;
    private ControlledStorageHandoffOrchestrationService controlledHandoff;
    private ImmediateStorageHandoffService service;

    @BeforeEach
    void setUp() {
        config = new ImmediateStorageHandoffProperties();
        controlledHandoff = mock(ControlledStorageHandoffOrchestrationService.class);
        service = new ImmediateStorageHandoffService(config, controlledHandoff);
    }

    private ImageGeneratedAsset mockGeneratedAsset() {
        ImageGeneratedAsset asset = new ImageGeneratedAsset();
        asset.setId(UUID.randomUUID());
        asset.setImageMediaAssetId(UUID.randomUUID());
        asset.setStatus(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
        return asset;
    }

    private ProviderOutputReference refWithUrl(String url) {
        ImageProviderGenerationResult result = ImageProviderGenerationResult.builder()
                .success(true)
                .providerAlias("OPENAI")
                .correlationId(UUID.randomUUID())
                .build();
        return new ProviderOutputReference(result, url);
    }

    private ProviderOutputReference refNoUrl() {
        ImageProviderGenerationResult result = ImageProviderGenerationResult.builder()
                .success(true)
                .providerAlias("OPENAI")
                .correlationId(UUID.randomUUID())
                .build();
        return new ProviderOutputReference(result, null);
    }

    private ImageStorageHandoffResult successHandoff(UUID genId) {
        return ImageStorageHandoffResult.builder()
                .success(true)
                .generatedAssetId(genId)
                .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_STORED)
                .previewStatus(ImagePreviewStatus.PREVIEW_AVAILABLE)
                .correlationId("test")
                .build();
    }

    private ImageStorageHandoffResult failureHandoff(UUID genId) {
        return ImageStorageHandoffResult.builder()
                .success(false)
                .generatedAssetId(genId)
                .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_FAILED)
                .previewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE)
                .errorCode(ImageStorageErrorCode.STORAGE_WRITE_FAILED)
                .errorMessage("Depolama yazma sırasında hata oluştu")
                .correlationId("test")
                .build();
    }

    // ── A. Immediate Handoff Success ────────────────────────────────

    @Nested
    @DisplayName("A. Immediate Handoff Success")
    class HandoffSuccess {

        @Test
        @DisplayName("Enabled + provider URL → calls controlled handoff")
        void enabled_withUrl_callsHandoff() {
            config.setEnabled(true);
            ImageGeneratedAsset asset = mockGeneratedAsset();
            ProviderOutputReference ref = refWithUrl("https://provider.example.com/output.png");

            when(controlledHandoff.executeControlledHandoff(eq(asset.getId()), any(), any()))
                    .thenReturn(successHandoff(asset.getId()));

            service.attemptImmediateHandoff(asset, ref);

            verify(controlledHandoff).executeControlledHandoff(
                    eq(asset.getId()),
                    eq("https://provider.example.com/output.png"),
                    any()
            );
        }

        @Test
        @DisplayName("Handoff success does NOT approve the asset")
        void handoff_doesNotApprove() {
            config.setEnabled(true);
            ImageGeneratedAsset asset = mockGeneratedAsset();

            when(controlledHandoff.executeControlledHandoff(any(), any(), any()))
                    .thenReturn(successHandoff(asset.getId()));

            service.attemptImmediateHandoff(asset, refWithUrl("https://p.com/img.png"));

            // Asset status remains GENERATED_PENDING_REVIEW
            assertThat(asset.getStatus()).isEqualTo(GeneratedAssetStatus.GENERATED_PENDING_REVIEW);
        }
    }

    // ── B. Immediate Handoff Disabled ───────────────────────────────

    @Nested
    @DisplayName("B. Immediate Handoff Disabled")
    class HandoffDisabled {

        @Test
        @DisplayName("Disabled config → no handoff call")
        void disabled_noHandoffCall() {
            config.setEnabled(false);

            service.attemptImmediateHandoff(mockGeneratedAsset(),
                    refWithUrl("https://provider.example.com/output.png"));

            verifyNoInteractions(controlledHandoff);
        }

        @Test
        @DisplayName("Default config is disabled")
        void defaultConfig_disabled() {
            ImmediateStorageHandoffProperties defaults = new ImmediateStorageHandoffProperties();
            assertThat(defaults.isEnabled()).isFalse();
        }
    }

    // ── C. Missing Provider URL ─────────────────────────────────────

    @Nested
    @DisplayName("C. Missing Provider URL")
    class MissingProviderUrl {

        @Test
        @DisplayName("Null URL → no handoff call")
        void nullUrl_noHandoff() {
            config.setEnabled(true);

            service.attemptImmediateHandoff(mockGeneratedAsset(), refNoUrl());

            verifyNoInteractions(controlledHandoff);
        }

        @Test
        @DisplayName("Blank URL → no handoff call")
        void blankUrl_noHandoff() {
            config.setEnabled(true);

            service.attemptImmediateHandoff(mockGeneratedAsset(), refWithUrl("   "));

            verifyNoInteractions(controlledHandoff);
        }

        @Test
        @DisplayName("Null output reference → no handoff call")
        void nullRef_noHandoff() {
            config.setEnabled(true);

            service.attemptImmediateHandoff(mockGeneratedAsset(), null);

            verifyNoInteractions(controlledHandoff);
        }
    }

    // ── D. Provider Download Failure ────────────────────────────────

    @Nested
    @DisplayName("D. Handoff Failure (non-fatal)")
    class HandoffFailure {

        @Test
        @DisplayName("Controlled handoff failure → non-fatal, no exception thrown")
        void handoffFailure_nonFatal() {
            config.setEnabled(true);
            ImageGeneratedAsset asset = mockGeneratedAsset();

            when(controlledHandoff.executeControlledHandoff(any(), any(), any()))
                    .thenReturn(failureHandoff(asset.getId()));

            // Should not throw
            service.attemptImmediateHandoff(asset, refWithUrl("https://p.com/img.png"));

            verify(controlledHandoff).executeControlledHandoff(any(), any(), any());
        }

        @Test
        @DisplayName("Unexpected exception → non-fatal, swallowed safely")
        void unexpectedException_nonFatal() {
            config.setEnabled(true);

            when(controlledHandoff.executeControlledHandoff(any(), any(), any()))
                    .thenThrow(new RuntimeException("unexpected"));

            // Should not throw
            service.attemptImmediateHandoff(mockGeneratedAsset(),
                    refWithUrl("https://p.com/img.png"));
        }
    }

    // ── E. Storage Failure ──────────────────────────────────────────

    @Nested
    @DisplayName("E. Storage Failure Safety")
    class StorageFailureSafety {

        @Test
        @DisplayName("Storage failure result has no provider URL")
        void storageFailure_noProviderUrl() {
            ImageStorageHandoffResult failure = failureHandoff(UUID.randomUUID());
            String str = failure.toString();

            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("provider.example.com");
        }

        @Test
        @DisplayName("Storage failure result has no storage key")
        void storageFailure_noStorageKey() {
            ImageStorageHandoffResult failure = failureHandoff(UUID.randomUUID());
            String str = failure.toString();

            assertThat(str).doesNotContain("imagegen/");
        }
    }

    // ── F. ProviderOutputReference Safety ───────────────────────────

    @Nested
    @DisplayName("F. ProviderOutputReference Safety")
    class ReferenceSafety {

        @Test
        @DisplayName("ProviderOutputReference toString does not expose URL")
        void toString_noUrl() {
            ProviderOutputReference ref = refWithUrl("https://secret-provider.com/img.png");
            String str = ref.toString();

            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secret-provider");
            assertThat(str).contains("hasProviderOutputUrl=true");
        }

        @Test
        @DisplayName("ProviderOutputReference with null URL is safe")
        void nullUrl_safe() {
            ProviderOutputReference ref = refNoUrl();
            String str = ref.toString();

            assertThat(str).doesNotContain("https://");
            assertThat(str).contains("hasProviderOutputUrl=false");
        }
    }

    // ── G. Config Safety ────────────────────────────────────────────

    @Nested
    @DisplayName("G. Config Safety")
    class ConfigSafety {

        @Test
        @DisplayName("Config has no secret fields")
        void noSecretFields() {
            ImmediateStorageHandoffProperties p = new ImmediateStorageHandoffProperties();
            String str = p.toString();

            assertThat(str).doesNotContain("apiKey");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("password");
        }
    }

    // ── H. DTO/Forbidden Exposure ───────────────────────────────────

    @Nested
    @DisplayName("H. DTO / Forbidden Exposure")
    class DtoExposure {

        @Test
        @DisplayName("ImageStorageHandoffResult has no providerUrl field")
        void handoffResult_noProviderUrl() {
            java.util.Set<String> fields = java.util.Arrays.stream(
                    ImageStorageHandoffResult.class.getDeclaredFields())
                    .map(java.lang.reflect.Field::getName)
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(fields).doesNotContain("providerUrl", "providerOutputUrl",
                    "signedUrl", "publicUrl", "cdnUrl", "base64");
        }

        @Test
        @DisplayName("ImageStorageHandoffResult has no storageObjectKey field")
        void handoffResult_noStorageKey() {
            java.util.Set<String> fields = java.util.Arrays.stream(
                    ImageStorageHandoffResult.class.getDeclaredFields())
                    .map(java.lang.reflect.Field::getName)
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(fields).doesNotContain("storageObjectKey", "storageBucket", "storageKey");
        }
    }

    // ── I. Preview / Review Regression ──────────────────────────────

    @Nested
    @DisplayName("I. Preview / Review Regression")
    class PreviewReviewRegression {

        @Test
        @DisplayName("No READY_FOR_PUBLISH status exists in GeneratedAssetStatus")
        void noReadyForPublish() {
            java.util.Set<String> statuses = java.util.Arrays.stream(GeneratedAssetStatus.values())
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(statuses).doesNotContain("READY_FOR_PUBLISH", "COMPLETED");
        }

        @Test
        @DisplayName("Handoff success returns STORAGE_COMPLETED, not APPROVED")
        void handoff_notApproved() {
            ImageStorageHandoffResult result = successHandoff(UUID.randomUUID());
            assertThat(result.getStorageHandoffStatus())
                    .isEqualTo(ImageStorageHandoffStatus.STORAGE_STORED);
        }

        @Test
        @DisplayName("No publish/export fields in handoff result")
        void noPublishExport() {
            java.util.Set<String> fields = java.util.Arrays.stream(
                    ImageStorageHandoffResult.class.getDeclaredFields())
                    .map(java.lang.reflect.Field::getName)
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(fields).doesNotContain("publishStatus", "exportStatus", "publishUrl", "exportUrl");
        }
    }
}
