package com.anadoluatlasi.backend.platformpackage.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-005 Task 03: Export type enum, DTO, exception ve filename builder testleri.
 *
 * <p>Bu testler DTO/type-level davranışları doğrular.
 * Service/controller integration testleri henüz yoktur (Task 04-06'da eklenecek).</p>
 */
class PlatformPackageExportTypeTest {

    // ── Enum Tests ──

    @Nested
    @DisplayName("PlatformPackageExportType Enum")
    class ExportTypeEnumTests {

        @Test
        @DisplayName("Enum tam olarak 4 değer içerir")
        void shouldContainExactlyFourValues() {
            assertThat(PlatformPackageExportType.values()).hasSize(4);
        }

        @Test
        @DisplayName("Beklenen 4 export tipi mevcut")
        void shouldContainExpectedValues() {
            Set<String> names = Arrays.stream(PlatformPackageExportType.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(names).containsExactlyInAnyOrder(
                    "MARKDOWN_BRIEF",
                    "JSON_PACKAGE",
                    "COPY_TEXT",
                    "PRODUCTION_BRIEF"
            );
        }

        @Test
        @DisplayName("MARKDOWN_BRIEF → text/markdown, md")
        void markdownBriefContentType() {
            assertThat(PlatformPackageExportType.MARKDOWN_BRIEF.getContentType())
                    .isEqualTo("text/markdown");
            assertThat(PlatformPackageExportType.MARKDOWN_BRIEF.getFileExtension())
                    .isEqualTo("md");
        }

        @Test
        @DisplayName("JSON_PACKAGE → application/json, json")
        void jsonPackageContentType() {
            assertThat(PlatformPackageExportType.JSON_PACKAGE.getContentType())
                    .isEqualTo("application/json");
            assertThat(PlatformPackageExportType.JSON_PACKAGE.getFileExtension())
                    .isEqualTo("json");
        }

        @Test
        @DisplayName("COPY_TEXT → text/plain, txt")
        void copyTextContentType() {
            assertThat(PlatformPackageExportType.COPY_TEXT.getContentType())
                    .isEqualTo("text/plain");
            assertThat(PlatformPackageExportType.COPY_TEXT.getFileExtension())
                    .isEqualTo("txt");
        }

        @Test
        @DisplayName("PRODUCTION_BRIEF → text/markdown, md")
        void productionBriefContentType() {
            assertThat(PlatformPackageExportType.PRODUCTION_BRIEF.getContentType())
                    .isEqualTo("text/markdown");
            assertThat(PlatformPackageExportType.PRODUCTION_BRIEF.getFileExtension())
                    .isEqualTo("md");
        }

        @ParameterizedTest
        @EnumSource(PlatformPackageExportType.class)
        @DisplayName("Tüm export tiplerinde contentType ve fileExtension null değil")
        void allTypesHaveNonNullContentTypeAndExtension(PlatformPackageExportType type) {
            assertThat(type.getContentType()).isNotNull().isNotBlank();
            assertThat(type.getFileExtension()).isNotNull().isNotBlank();
        }
    }

    // ── Filename Builder Tests ──

    @Nested
    @DisplayName("PlatformPackageExportFilenameBuilder")
    class FilenameBuilderTests {

        @Test
        @DisplayName("Standart Türkçe başlık → doğru slug_type_version.ext")
        void shouldBuildStandardFilename() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    "Truva Atının Hikayesi",
                    "YOUTUBE_LONG_FORM",
                    1,
                    PlatformPackageExportType.MARKDOWN_BRIEF
            );
            assertThat(result).isEqualTo("truva-atinin-hikayesi_youtube-long-form_v1.md");
        }

        @Test
        @DisplayName("Türkçe özel karakterler doğru dönüştürülür")
        void shouldHandleTurkishCharacters() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    "Çanakkale Türküsünün Öyküsü",
                    "YOUTUBE_SHORTS",
                    2,
                    PlatformPackageExportType.JSON_PACKAGE
            );
            assertThat(result).isEqualTo("canakkale-turkusunun-oykusu_youtube-shorts_v2.json");
        }

        @Test
        @DisplayName("JSON export dosya uzantısı .json")
        void shouldUseJsonExtension() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    "Test", "INSTAGRAM_REELS", 1,
                    PlatformPackageExportType.JSON_PACKAGE
            );
            assertThat(result).endsWith(".json");
        }

        @Test
        @DisplayName("Copy text dosya uzantısı .txt")
        void shouldUseTxtExtension() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    "Test", "FACEBOOK_REELS", 3,
                    PlatformPackageExportType.COPY_TEXT
            );
            assertThat(result).endsWith(".txt");
        }

        @Test
        @DisplayName("Production brief dosya uzantısı .md")
        void shouldUseMdExtensionForBrief() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    "Test", "YOUTUBE_LONG_FORM", 1,
                    PlatformPackageExportType.PRODUCTION_BRIEF
            );
            assertThat(result).endsWith(".md");
        }

        @Test
        @DisplayName("Null başlık → 'export' fallback slug")
        void shouldHandleNullTitle() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    null, "YOUTUBE_SHORTS", 1,
                    PlatformPackageExportType.MARKDOWN_BRIEF
            );
            assertThat(result).startsWith("export_");
        }

        @Test
        @DisplayName("Boş başlık → 'export' fallback slug")
        void shouldHandleBlankTitle() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    "   ", "YOUTUBE_SHORTS", 1,
                    PlatformPackageExportType.COPY_TEXT
            );
            assertThat(result).startsWith("export_");
        }

        @Test
        @DisplayName("Versiyon dosya adına dahil edilir")
        void shouldIncludeVersion() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    "Test", "YOUTUBE_LONG_FORM", 5,
                    PlatformPackageExportType.MARKDOWN_BRIEF
            );
            assertThat(result).contains("_v5.");
        }

        @Test
        @DisplayName("Özel karakterler temizlenir")
        void shouldCleanSpecialCharacters() {
            String result = PlatformPackageExportFilenameBuilder.build(
                    "Test! @#$% Başlık (v2)",
                    "INSTAGRAM_REELS", 1,
                    PlatformPackageExportType.COPY_TEXT
            );
            assertThat(result).doesNotContain("!", "@", "#", "$", "%", "(", ")");
            assertThat(result).startsWith("test-");
        }
    }

    // ── DTO Builder Tests ──

    @Nested
    @DisplayName("Export Response DTO")
    class ResponseDtoTests {

        @Test
        @DisplayName("PlatformPackageExportResponse builder çalışır")
        void shouldBuildResponse() {
            PlatformPackageExportResponse response = PlatformPackageExportResponse.builder()
                    .packageId(UUID.randomUUID())
                    .packageType("YOUTUBE_LONG_FORM")
                    .exportType(PlatformPackageExportType.MARKDOWN_BRIEF)
                    .filename("test_youtube-long-form_v1.md")
                    .contentType("text/markdown")
                    .content("# Test Export")
                    .build();

            assertThat(response.getPackageId()).isNotNull();
            assertThat(response.getPackageType()).isEqualTo("YOUTUBE_LONG_FORM");
            assertThat(response.getExportType()).isEqualTo(PlatformPackageExportType.MARKDOWN_BRIEF);
            assertThat(response.getFilename()).isEqualTo("test_youtube-long-form_v1.md");
            assertThat(response.getContentType()).isEqualTo("text/markdown");
            assertThat(response.getContent()).isEqualTo("# Test Export");
        }
    }

    // ── Metadata DTO Tests ──

    @Nested
    @DisplayName("Export Metadata DTO")
    class MetadataDtoTests {

        @Test
        @DisplayName("PlatformPackageExportMetadata builder çalışır")
        void shouldBuildMetadata() {
            PlatformPackageExportMetadata metadata = PlatformPackageExportMetadata.builder()
                    .contentProjectId(UUID.randomUUID())
                    .sourceStoryboardDraftId(UUID.randomUUID())
                    .version(1)
                    .packageStatus("APPROVED")
                    .packageTitle("Test Title")
                    .providerType("OPENAI")
                    .modelName("gpt-4o-mini")
                    .generatedBy("AI_CONTENT_AGENT")
                    .build();

            assertThat(metadata.getPackageStatus()).isEqualTo("APPROVED");
            assertThat(metadata.getProviderType()).isEqualTo("OPENAI");
            assertThat(metadata.getModelName()).isEqualTo("gpt-4o-mini");
        }

        @Test
        @DisplayName("Metadata sceneUsages listesi nullable")
        void shouldAllowNullSceneUsages() {
            PlatformPackageExportMetadata metadata = PlatformPackageExportMetadata.builder()
                    .contentProjectId(UUID.randomUUID())
                    .build();

            assertThat(metadata.getSceneUsages()).isNull();
        }
    }

    // ── Scene Usage DTO Tests ──

    @Nested
    @DisplayName("Export Scene Usage DTO")
    class SceneUsageDtoTests {

        @Test
        @DisplayName("PlatformPackageExportSceneUsageDto builder çalışır")
        void shouldBuildSceneUsage() {
            PlatformPackageExportSceneUsageDto dto = PlatformPackageExportSceneUsageDto.builder()
                    .sourceStoryboardSceneId(UUID.randomUUID())
                    .sceneNumber(1)
                    .usageType("PRIMARY")
                    .included(true)
                    .adaptationNote("Tam kullanım")
                    .build();

            assertThat(dto.getSceneNumber()).isEqualTo(1);
            assertThat(dto.getUsageType()).isEqualTo("PRIMARY");
            assertThat(dto.getIncluded()).isTrue();
        }
    }

    // ── Exception Tests ──

    @Nested
    @DisplayName("Export Exceptions")
    class ExceptionTests {

        @Test
        @DisplayName("ExportNotAllowed → BusinessException (409)")
        void exportNotAllowedIsBusinessException() {
            UUID packageId = UUID.randomUUID();
            PlatformPackageExportNotAllowedException ex =
                    new PlatformPackageExportNotAllowedException(packageId, "AI_GENERATED");

            assertThat(ex).isInstanceOf(com.anadoluatlasi.backend.common.exception.BusinessException.class);
            assertThat(ex.getMessage()).contains(packageId.toString());
            assertThat(ex.getMessage()).contains("AI_GENERATED");
            assertThat(ex.getMessage()).contains("APPROVED");
        }

        @Test
        @DisplayName("UnsupportedExportType → ValidationException (400)")
        void unsupportedTypeIsValidationException() {
            UnsupportedPlatformPackageExportTypeException ex =
                    new UnsupportedPlatformPackageExportTypeException("INVALID_TYPE");

            assertThat(ex).isInstanceOf(com.anadoluatlasi.backend.common.exception.ValidationException.class);
            assertThat(ex.getMessage()).contains("INVALID_TYPE");
            assertThat(ex.getMessage()).contains("MARKDOWN_BRIEF");
        }
    }
}
