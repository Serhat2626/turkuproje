package com.anadoluatlasi.backend.platformpackage.controller;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportMetadata;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportNotAllowedException;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportResponse;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportSceneUsageDto;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportService;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportType;
import com.anadoluatlasi.backend.platformpackage.export.UnsupportedPlatformPackageExportTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-005 Task 05: PlatformPackageExportController unit testleri.
 *
 * <p>Kapsam: endpoint routing, response format, error handling,
 * safety (no unsafe field exposure), read-only behaviour.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformPackageExportControllerTest {

    @Mock
    private PlatformPackageExportService exportService;

    private PlatformPackageExportController controller;

    private UUID packageId;

    @BeforeEach
    void setUp() {
        controller = new PlatformPackageExportController(exportService);
        packageId = UUID.randomUUID();
    }

    // ═══ Helper ═══

    private PlatformPackageExportResponse createMockResponse(PlatformPackageExportType type) {
        return PlatformPackageExportResponse.builder()
                .packageId(packageId)
                .packageType("YOUTUBE_LONG_FORM")
                .exportType(type)
                .filename("truva-atinin-hikayesi_youtube-long-form_v1." + type.getFileExtension())
                .contentType(type.getContentType())
                .content("# Test Export Content")
                .exportedAt(Instant.now())
                .metadata(PlatformPackageExportMetadata.builder()
                        .contentProjectId(UUID.randomUUID())
                        .sourceStoryboardDraftId(UUID.randomUUID())
                        .version(1)
                        .packageStatus("APPROVED")
                        .packageTitle("Truva Atının Hikayesi")
                        .approvedAt(LocalDateTime.of(2026, 7, 3, 12, 0))
                        .approvedBy("admin-serhat")
                        .providerType("OPENAI")
                        .modelName("gpt-4o-mini")
                        .generatedBy("AI_CONTENT_AGENT")
                        .sceneUsages(List.of(
                                PlatformPackageExportSceneUsageDto.builder()
                                        .sceneNumber(1)
                                        .usageType("PRIMARY")
                                        .included(true)
                                        .adaptationNote("Tam kullanım")
                                        .build()
                        ))
                        .build())
                .build();
    }

    // ═══ 200 OK Tests — All 4 Export Types ═══

    @Nested
    @DisplayName("200 OK — APPROVED export")
    class SuccessfulExportTests {

        @Test
        @DisplayName("GET MARKDOWN_BRIEF → 200 OK")
        void shouldExportMarkdownBrief() {
            PlatformPackageExportResponse mockResp = createMockResponse(PlatformPackageExportType.MARKDOWN_BRIEF);
            when(exportService.exportPackage(eq(packageId), eq(PlatformPackageExportType.MARKDOWN_BRIEF)))
                    .thenReturn(mockResp);

            ResponseEntity<ApiResponse<PlatformPackageExportResponse>> entity =
                    controller.exportPackage(packageId, "MARKDOWN_BRIEF");

            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody()).isNotNull();
            assertThat(entity.getBody().isSuccess()).isTrue();
            assertThat(entity.getBody().getData().getExportType()).isEqualTo(PlatformPackageExportType.MARKDOWN_BRIEF);
        }

        @Test
        @DisplayName("GET JSON_PACKAGE → 200 OK")
        void shouldExportJsonPackage() {
            PlatformPackageExportResponse mockResp = createMockResponse(PlatformPackageExportType.JSON_PACKAGE);
            when(exportService.exportPackage(eq(packageId), eq(PlatformPackageExportType.JSON_PACKAGE)))
                    .thenReturn(mockResp);

            ResponseEntity<ApiResponse<PlatformPackageExportResponse>> entity =
                    controller.exportPackage(packageId, "JSON_PACKAGE");

            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody().getData().getExportType()).isEqualTo(PlatformPackageExportType.JSON_PACKAGE);
        }

        @Test
        @DisplayName("GET COPY_TEXT → 200 OK")
        void shouldExportCopyText() {
            PlatformPackageExportResponse mockResp = createMockResponse(PlatformPackageExportType.COPY_TEXT);
            when(exportService.exportPackage(eq(packageId), eq(PlatformPackageExportType.COPY_TEXT)))
                    .thenReturn(mockResp);

            ResponseEntity<ApiResponse<PlatformPackageExportResponse>> entity =
                    controller.exportPackage(packageId, "COPY_TEXT");

            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody().getData().getExportType()).isEqualTo(PlatformPackageExportType.COPY_TEXT);
        }

        @Test
        @DisplayName("GET PRODUCTION_BRIEF → 200 OK")
        void shouldExportProductionBrief() {
            PlatformPackageExportResponse mockResp = createMockResponse(PlatformPackageExportType.PRODUCTION_BRIEF);
            when(exportService.exportPackage(eq(packageId), eq(PlatformPackageExportType.PRODUCTION_BRIEF)))
                    .thenReturn(mockResp);

            ResponseEntity<ApiResponse<PlatformPackageExportResponse>> entity =
                    controller.exportPackage(packageId, "PRODUCTION_BRIEF");

            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody().getData().getExportType()).isEqualTo(PlatformPackageExportType.PRODUCTION_BRIEF);
        }

        @Test
        @DisplayName("Case-insensitive exportType (lowercase) çalışır")
        void shouldHandleLowercaseExportType() {
            PlatformPackageExportResponse mockResp = createMockResponse(PlatformPackageExportType.MARKDOWN_BRIEF);
            when(exportService.exportPackage(eq(packageId), eq(PlatformPackageExportType.MARKDOWN_BRIEF)))
                    .thenReturn(mockResp);

            ResponseEntity<ApiResponse<PlatformPackageExportResponse>> entity =
                    controller.exportPackage(packageId, "markdown_brief");

            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ═══ Response Field Tests ═══

    @Nested
    @DisplayName("Response field doğrulamaları")
    class ResponseFieldTests {

        @Test
        @DisplayName("Response packageId, exportType, filename, contentType, content, metadata içerir")
        void shouldContainAllExpectedFields() {
            PlatformPackageExportResponse mockResp = createMockResponse(PlatformPackageExportType.MARKDOWN_BRIEF);
            when(exportService.exportPackage(eq(packageId), eq(PlatformPackageExportType.MARKDOWN_BRIEF)))
                    .thenReturn(mockResp);

            ResponseEntity<ApiResponse<PlatformPackageExportResponse>> entity =
                    controller.exportPackage(packageId, "MARKDOWN_BRIEF");

            PlatformPackageExportResponse data = entity.getBody().getData();
            assertThat(data.getPackageId()).isEqualTo(packageId);
            assertThat(data.getExportType()).isNotNull();
            assertThat(data.getFilename()).isNotBlank();
            assertThat(data.getContentType()).isNotBlank();
            assertThat(data.getContent()).isNotBlank();
            assertThat(data.getExportedAt()).isNotNull();
            assertThat(data.getMetadata()).isNotNull();
        }

        @Test
        @DisplayName("Response metadata scene usages dahil")
        void shouldContainMetadataWithSceneUsages() {
            PlatformPackageExportResponse mockResp = createMockResponse(PlatformPackageExportType.MARKDOWN_BRIEF);
            when(exportService.exportPackage(eq(packageId), eq(PlatformPackageExportType.MARKDOWN_BRIEF)))
                    .thenReturn(mockResp);

            ResponseEntity<ApiResponse<PlatformPackageExportResponse>> entity =
                    controller.exportPackage(packageId, "MARKDOWN_BRIEF");

            PlatformPackageExportMetadata meta = entity.getBody().getData().getMetadata();
            assertThat(meta.getContentProjectId()).isNotNull();
            assertThat(meta.getSourceStoryboardDraftId()).isNotNull();
            assertThat(meta.getPackageStatus()).isEqualTo("APPROVED");
            assertThat(meta.getSceneUsages()).hasSize(1);
            assertThat(meta.getSceneUsages().get(0).getUsageType()).isEqualTo("PRIMARY");
        }
    }

    // ═══ Error: Non-APPROVED → 409 ═══

    @Nested
    @DisplayName("Non-APPROVED → exception (409 via GlobalExceptionHandler)")
    class NonApprovedTests {

        @Test
        @DisplayName("AI_GENERATED → PlatformPackageExportNotAllowedException")
        void shouldThrowForAiGenerated() {
            when(exportService.exportPackage(eq(packageId), any()))
                    .thenThrow(new PlatformPackageExportNotAllowedException(packageId, "AI_GENERATED"));

            assertThatThrownBy(() -> controller.exportPackage(packageId, "MARKDOWN_BRIEF"))
                    .isInstanceOf(PlatformPackageExportNotAllowedException.class)
                    .hasMessageContaining("AI_GENERATED");
        }

        @Test
        @DisplayName("NEEDS_REVIEW → PlatformPackageExportNotAllowedException")
        void shouldThrowForNeedsReview() {
            when(exportService.exportPackage(eq(packageId), any()))
                    .thenThrow(new PlatformPackageExportNotAllowedException(packageId, "NEEDS_REVIEW"));

            assertThatThrownBy(() -> controller.exportPackage(packageId, "JSON_PACKAGE"))
                    .isInstanceOf(PlatformPackageExportNotAllowedException.class)
                    .hasMessageContaining("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("REJECTED → PlatformPackageExportNotAllowedException")
        void shouldThrowForRejected() {
            when(exportService.exportPackage(eq(packageId), any()))
                    .thenThrow(new PlatformPackageExportNotAllowedException(packageId, "REJECTED"));

            assertThatThrownBy(() -> controller.exportPackage(packageId, "COPY_TEXT"))
                    .isInstanceOf(PlatformPackageExportNotAllowedException.class)
                    .hasMessageContaining("REJECTED");
        }

        @Test
        @DisplayName("FAILED → PlatformPackageExportNotAllowedException")
        void shouldThrowForFailed() {
            when(exportService.exportPackage(eq(packageId), any()))
                    .thenThrow(new PlatformPackageExportNotAllowedException(packageId, "FAILED"));

            assertThatThrownBy(() -> controller.exportPackage(packageId, "PRODUCTION_BRIEF"))
                    .isInstanceOf(PlatformPackageExportNotAllowedException.class)
                    .hasMessageContaining("FAILED");
        }
    }

    // ═══ Error: Not Found → 404 ═══

    @Nested
    @DisplayName("Not Found → exception (404 via GlobalExceptionHandler)")
    class NotFoundTests {

        @Test
        @DisplayName("Missing package → NotFoundException")
        void shouldThrowNotFound() {
            UUID missing = UUID.randomUUID();
            when(exportService.exportPackage(eq(missing), any()))
                    .thenThrow(new NotFoundException("PlatformPackageDraft", missing));

            assertThatThrownBy(() -> controller.exportPackage(missing, "MARKDOWN_BRIEF"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("PlatformPackageDraft");
        }
    }

    // ═══ Error: Invalid Export Type → 400 ═══

    @Nested
    @DisplayName("Invalid export type → 400")
    class InvalidExportTypeTests {

        @Test
        @DisplayName("Geçersiz export type string → UnsupportedPlatformPackageExportTypeException")
        void shouldThrowForInvalidType() {
            assertThatThrownBy(() -> controller.exportPackage(packageId, "INVALID_FORMAT"))
                    .isInstanceOf(UnsupportedPlatformPackageExportTypeException.class)
                    .hasMessageContaining("INVALID_FORMAT");
        }

        @Test
        @DisplayName("Boş export type string → UnsupportedPlatformPackageExportTypeException")
        void shouldThrowForEmptyType() {
            assertThatThrownBy(() -> controller.exportPackage(packageId, ""))
                    .isInstanceOf(UnsupportedPlatformPackageExportTypeException.class);
        }
    }

    // ═══ Read-Only / Side-Effect Free ═══

    @Nested
    @DisplayName("Read-only / side-effect free")
    class ReadOnlyTests {

        @Test
        @DisplayName("Export endpoint service'i çağırır ama save yapmaz")
        void shouldDelegateToServiceOnly() {
            PlatformPackageExportResponse mockResp = createMockResponse(PlatformPackageExportType.MARKDOWN_BRIEF);
            when(exportService.exportPackage(eq(packageId), eq(PlatformPackageExportType.MARKDOWN_BRIEF)))
                    .thenReturn(mockResp);

            controller.exportPackage(packageId, "MARKDOWN_BRIEF");

            verify(exportService).exportPackage(packageId, PlatformPackageExportType.MARKDOWN_BRIEF);
            // Controller'da save/mutate method çağırılmaz
        }
    }

    // ═══ Safety — no unsafe fields ═══

    @Nested
    @DisplayName("Safety — unsafe field exposure yok")
    class SafetyTests {

        private static final List<String> UNSAFE_KEYWORDS = List.of(
                "rawPrompt", "raw_prompt", "providerRequest", "provider_request",
                "providerResponse", "provider_response", "providerPayload", "provider_payload",
                "apiKey", "api_key", "secretKey", "secret_key", "apiSecret", "api_secret"
        );

        @ParameterizedTest
        @EnumSource(PlatformPackageExportType.class)
        @DisplayName("Response content'te unsafe alan bulunmaz")
        void shouldNotExposeUnsafeFields(PlatformPackageExportType exportType) {
            PlatformPackageExportResponse mockResp = createMockResponse(exportType);
            when(exportService.exportPackage(eq(packageId), eq(exportType)))
                    .thenReturn(mockResp);

            ResponseEntity<ApiResponse<PlatformPackageExportResponse>> entity =
                    controller.exportPackage(packageId, exportType.name());

            String content = entity.getBody().getData().getContent().toLowerCase();
            for (String unsafe : UNSAFE_KEYWORDS) {
                assertThat(content).as("Should not contain: " + unsafe)
                        .doesNotContain(unsafe.toLowerCase());
            }
        }
    }

    // ═══ Annotation / Convention Tests ═══

    @Nested
    @DisplayName("Controller annotation ve convention doğrulamaları")
    class AnnotationTests {

        @Test
        @DisplayName("Controller @RestController annotasyonu var")
        void shouldHaveRestControllerAnnotation() {
            assertThat(PlatformPackageExportController.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isTrue();
        }

        @Test
        @DisplayName("Controller @RequestMapping /api/admin base path kullanır")
        void shouldHaveCorrectBasePath() {
            org.springframework.web.bind.annotation.RequestMapping mapping =
                    PlatformPackageExportController.class.getAnnotation(
                            org.springframework.web.bind.annotation.RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin");
        }

        @Test
        @DisplayName("Export endpoint @GetMapping kullanır")
        void shouldUseGetMapping() throws NoSuchMethodException {
            var method = PlatformPackageExportController.class.getMethod(
                    "exportPackage", UUID.class, String.class);
            assertThat(method.isAnnotationPresent(
                    org.springframework.web.bind.annotation.GetMapping.class)).isTrue();
        }

        @Test
        @DisplayName("Export endpoint path doğru")
        void shouldHaveCorrectEndpointPath() throws NoSuchMethodException {
            var method = PlatformPackageExportController.class.getMethod(
                    "exportPackage", UUID.class, String.class);
            var getMapping = method.getAnnotation(
                    org.springframework.web.bind.annotation.GetMapping.class);
            assertThat(getMapping.value())
                    .contains("/platform-packages/{packageId}/exports/{exportType}");
        }
    }
}
