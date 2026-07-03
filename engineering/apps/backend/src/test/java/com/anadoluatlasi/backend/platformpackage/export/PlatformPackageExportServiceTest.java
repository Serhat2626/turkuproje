package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageSceneUsage;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageStatus;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import com.anadoluatlasi.backend.platformpackage.repository.PlatformPackageDraftRepository;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-005 Task 04: PlatformPackageExportService ve formatter unit testleri.
 *
 * <p>Kapsam:</p>
 * <ul>
 *   <li>APPROVED → 4 export tipi başarılı</li>
 *   <li>Non-APPROVED statüler → exception</li>
 *   <li>Export read-only — no save, no mutate</li>
 *   <li>Response field doğruluğu</li>
 *   <li>Format-spesifik output doğrulamaları</li>
 *   <li>Safety — raw prompt/key/secret yok</li>
 * </ul>
 */
class PlatformPackageExportServiceTest {

    private PlatformPackageDraftRepository repository;
    private PlatformPackageExportService exportService;

    private ContentProject project;
    private StoryboardDraft storyboardDraft;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = mock(PlatformPackageDraftRepository.class);

        List<PlatformPackageExportFormatter> formatters = List.of(
                new MarkdownBriefPlatformPackageExportFormatter(),
                new JsonPlatformPackageExportFormatter(),
                new CopyTextPlatformPackageExportFormatter(),
                new ProductionBriefPlatformPackageExportFormatter()
        );

        exportService = new PlatformPackageExportService(repository, formatters);

        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .retryCount(0)
                .build();

        storyboardDraft = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .version(1)
                .status(StoryboardDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    // ═══ Helper ═══

    private PlatformPackageDraft createApprovedYtLfDraft() {
        PlatformPackageDraft draft = PlatformPackageDraft.builder()
                .contentProject(project)
                .sourceStoryboardDraft(storyboardDraft)
                .packageType(PlatformPackageType.YOUTUBE_LONG_FORM)
                .status(PlatformPackageStatus.APPROVED)
                .version(1)
                .title("Truva Atının Hikayesi")
                .captionOrDescription("Antik dünyanın en ilginç hikayesi")
                .hashtagsJson("[\"#TruvaAtı\",\"#Tarih\",\"#Anadolu\"]")
                .callToAction("Abone olmayı unutmayın!")
                .targetDurationSeconds(340)
                .aspectRatio("16:9")
                .videoTitle("Truva Atı'nın Bilinmeyen Hikayesi | Anadolu Atlası")
                .longDescription("Bu videoda Truva Atı'nın gerçek hikayesini keşfedeceksiniz.")
                .chapterOutlineJson("[\"00:00 Giriş\",\"01:30 Truva'nın Önemi\",\"03:00 Sonuç\"]")
                .pinnedCommentSuggestion("Hangi Anadolu hikayesini dinlemek istersiniz?")
                .thumbnailTextSuggestion("TRUVA ATI'NIN SIRRI")
                .tagsJson("[\"truva atı\",\"anadolu\",\"tarih\"]")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("OPENAI")
                .modelName("gpt-4o-mini")
                .approvedAt(LocalDateTime.of(2026, 7, 3, 12, 0))
                .approvedBy("admin-serhat")
                .contentStructureNotes("Hook ile başla, duygusal zirveye çıkar.")
                .sceneUsageNotes("Sahne 3 atlansın, kısa format uygun değil.")
                .reviewNotes("Genel olarak iyi. Thumbnail metni büyük harfle yazılmalı.")
                .build();
        draft.setId(UUID.randomUUID());

        // Scene usages
        PlatformPackageSceneUsage scene1 = PlatformPackageSceneUsage.builder()
                .platformPackageDraft(draft)
                .sceneNumber(1)
                .usageType("PRIMARY")
                .included(true)
                .adaptationNote("Tam kullanım")
                .build();
        PlatformPackageSceneUsage scene2 = PlatformPackageSceneUsage.builder()
                .platformPackageDraft(draft)
                .sceneNumber(2)
                .usageType("ADAPTED")
                .included(true)
                .adaptationNote("Kısaltılarak adapte edildi")
                .build();
        PlatformPackageSceneUsage scene3 = PlatformPackageSceneUsage.builder()
                .platformPackageDraft(draft)
                .sceneNumber(3)
                .usageType("OMITTED")
                .included(false)
                .build();

        draft.getSceneUsages().addAll(List.of(scene1, scene2, scene3));
        return draft;
    }

    private PlatformPackageDraft createApprovedShortsDraft() {
        PlatformPackageDraft draft = PlatformPackageDraft.builder()
                .contentProject(project)
                .sourceStoryboardDraft(storyboardDraft)
                .packageType(PlatformPackageType.YOUTUBE_SHORTS)
                .status(PlatformPackageStatus.APPROVED)
                .version(1)
                .title("Truva Atı #shorts")
                .captionOrDescription("60 saniyede Truva Atı hikayesi")
                .shortHook("Truva Atı aslında ne anlama geliyordu?")
                .shortCaption("Anadolu'nun en büyük sırrını keşfet!")
                .shortHashtagSetJson("[\"#shorts\",\"#TruvaAtı\",\"#tarih\"]")
                .shortCta("Beğen ve takip et!")
                .overlayTextSuggestionsJson("[\"TRUVA ATI\",\"Gizli Geçiş\",\"Savaşın Sonu\"]")
                .durationGuidance("45-55 saniye")
                .targetDurationSeconds(50)
                .aspectRatio("9:16")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("OPENAI")
                .modelName("gpt-4o-mini")
                .approvedAt(LocalDateTime.of(2026, 7, 3, 12, 0))
                .approvedBy("admin-serhat")
                .build();
        draft.setId(UUID.randomUUID());
        return draft;
    }

    private PlatformPackageDraft createDraftWithStatus(PlatformPackageStatus status) {
        PlatformPackageDraft draft = PlatformPackageDraft.builder()
                .contentProject(project)
                .sourceStoryboardDraft(storyboardDraft)
                .packageType(PlatformPackageType.YOUTUBE_LONG_FORM)
                .status(status)
                .version(1)
                .title("Test Paketi")
                .captionOrDescription("Test")
                .build();
        draft.setId(UUID.randomUUID());
        return draft;
    }

    // ═══ APPROVED Export Tests ═══

    @Nested
    @DisplayName("APPROVED paket export")
    class ApprovedExportTests {

        @Test
        @DisplayName("APPROVED → MARKDOWN_BRIEF export başarılı")
        void shouldExportMarkdownBrief() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);

            assertThat(response).isNotNull();
            assertThat(response.getPackageId()).isEqualTo(draft.getId());
            assertThat(response.getExportType()).isEqualTo(PlatformPackageExportType.MARKDOWN_BRIEF);
            assertThat(response.getContentType()).isEqualTo("text/markdown");
            assertThat(response.getContent()).isNotBlank();
            assertThat(response.getFilename()).endsWith(".md");
            assertThat(response.getExportedAt()).isNotNull();
            assertThat(response.getMetadata()).isNotNull();
        }

        @Test
        @DisplayName("APPROVED → JSON_PACKAGE export başarılı")
        void shouldExportJsonPackage() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);

            assertThat(response.getExportType()).isEqualTo(PlatformPackageExportType.JSON_PACKAGE);
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getFilename()).endsWith(".json");
        }

        @Test
        @DisplayName("APPROVED → COPY_TEXT export başarılı")
        void shouldExportCopyText() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.COPY_TEXT);

            assertThat(response.getExportType()).isEqualTo(PlatformPackageExportType.COPY_TEXT);
            assertThat(response.getContentType()).isEqualTo("text/plain");
            assertThat(response.getFilename()).endsWith(".txt");
        }

        @Test
        @DisplayName("APPROVED → PRODUCTION_BRIEF export başarılı")
        void shouldExportProductionBrief() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.PRODUCTION_BRIEF);

            assertThat(response.getExportType()).isEqualTo(PlatformPackageExportType.PRODUCTION_BRIEF);
            assertThat(response.getContentType()).isEqualTo("text/markdown");
            assertThat(response.getFilename()).endsWith(".md");
        }
    }

    // ═══ Non-APPROVED Rejection Tests ═══

    @Nested
    @DisplayName("Non-APPROVED paketler reddedilir")
    class NonApprovedRejectionTests {

        @Test
        @DisplayName("AI_GENERATED → export reddedilir (409)")
        void shouldRejectAiGenerated() {
            PlatformPackageDraft draft = createDraftWithStatus(PlatformPackageStatus.AI_GENERATED);
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF))
                    .isInstanceOf(PlatformPackageExportNotAllowedException.class)
                    .hasMessageContaining("AI_GENERATED");
        }

        @Test
        @DisplayName("NEEDS_REVIEW → export reddedilir (409)")
        void shouldRejectNeedsReview() {
            PlatformPackageDraft draft = createDraftWithStatus(PlatformPackageStatus.NEEDS_REVIEW);
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE))
                    .isInstanceOf(PlatformPackageExportNotAllowedException.class)
                    .hasMessageContaining("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("REJECTED → export reddedilir (409)")
        void shouldRejectRejected() {
            PlatformPackageDraft draft = createDraftWithStatus(PlatformPackageStatus.REJECTED);
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.COPY_TEXT))
                    .isInstanceOf(PlatformPackageExportNotAllowedException.class)
                    .hasMessageContaining("REJECTED");
        }

        @Test
        @DisplayName("FAILED → export reddedilir (409)")
        void shouldRejectFailed() {
            PlatformPackageDraft draft = createDraftWithStatus(PlatformPackageStatus.FAILED);
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.PRODUCTION_BRIEF))
                    .isInstanceOf(PlatformPackageExportNotAllowedException.class)
                    .hasMessageContaining("FAILED");
        }
    }

    // ═══ Not Found Tests ═══

    @Nested
    @DisplayName("Package not found")
    class NotFoundTests {

        @Test
        @DisplayName("Var olmayan paket → NotFoundException (404)")
        void shouldThrowNotFoundForMissingPackage() {
            UUID nonExistent = UUID.randomUUID();
            when(repository.findById(nonExistent)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> exportService.exportPackage(
                    nonExistent, PlatformPackageExportType.MARKDOWN_BRIEF))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("PlatformPackageDraft");
        }
    }

    // ═══ Null Export Type ═══

    @Nested
    @DisplayName("Null/invalid export type")
    class InvalidExportTypeTests {

        @Test
        @DisplayName("null export type → UnsupportedPlatformPackageExportTypeException (400)")
        void shouldRejectNullExportType() {
            assertThatThrownBy(() -> exportService.exportPackage(
                    UUID.randomUUID(), null))
                    .isInstanceOf(UnsupportedPlatformPackageExportTypeException.class);
        }
    }

    // ═══ Read-Only Tests ═══

    @Nested
    @DisplayName("Export read-only / side-effect free")
    class ReadOnlyTests {

        @Test
        @DisplayName("Export, repository.save çağırmaz")
        void shouldNotCallSave() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            exportService.exportPackage(draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);

            verify(repository, never()).save(any());
            verify(repository, never()).saveAndFlush(any());
            verify(repository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Export, paket statusunu değiştirmez")
        void shouldNotMutateStatus() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageStatus statusBefore = draft.getStatus();

            exportService.exportPackage(draft.getId(), PlatformPackageExportType.JSON_PACKAGE);

            assertThat(draft.getStatus()).isEqualTo(statusBefore);
            assertThat(draft.getStatus()).isEqualTo(PlatformPackageStatus.APPROVED);
        }
    }

    // ═══ Response Field Tests ═══

    @Nested
    @DisplayName("Response field doğrulamaları")
    class ResponseFieldTests {

        @Test
        @DisplayName("Response metadata doğru map edilir")
        void shouldIncludeCorrectMetadata() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);

            PlatformPackageExportMetadata meta = response.getMetadata();
            assertThat(meta.getContentProjectId()).isEqualTo(project.getId());
            assertThat(meta.getSourceStoryboardDraftId()).isEqualTo(storyboardDraft.getId());
            assertThat(meta.getVersion()).isEqualTo(1);
            assertThat(meta.getPackageStatus()).isEqualTo("APPROVED");
            assertThat(meta.getPackageTitle()).isEqualTo("Truva Atının Hikayesi");
            assertThat(meta.getApprovedBy()).isEqualTo("admin-serhat");
            assertThat(meta.getProviderType()).isEqualTo("OPENAI");
            assertThat(meta.getModelName()).isEqualTo("gpt-4o-mini");
            assertThat(meta.getGeneratedBy()).isEqualTo("AI_CONTENT_AGENT");
        }

        @Test
        @DisplayName("Response scene usages doğru map edilir")
        void shouldIncludeSceneUsages() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);

            List<PlatformPackageExportSceneUsageDto> usages = response.getMetadata().getSceneUsages();
            assertThat(usages).hasSize(3);
            assertThat(usages.get(0).getSceneNumber()).isEqualTo(1);
            assertThat(usages.get(0).getUsageType()).isEqualTo("PRIMARY");
            assertThat(usages.get(0).getIncluded()).isTrue();
            assertThat(usages.get(2).getUsageType()).isEqualTo("OMITTED");
            assertThat(usages.get(2).getIncluded()).isFalse();
        }

        @Test
        @DisplayName("Response filename doğru formatta")
        void shouldHaveCorrectFilename() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);

            assertThat(response.getFilename())
                    .isEqualTo("truva-atinin-hikayesi_youtube-long-form_v1.md");
        }

        @Test
        @DisplayName("Response packageType doğru")
        void shouldHaveCorrectPackageType() {
            PlatformPackageDraft draft = createApprovedShortsDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.COPY_TEXT);

            assertThat(response.getPackageType()).isEqualTo("YOUTUBE_SHORTS");
        }
    }

    // ═══ Markdown Output Tests ═══

    @Nested
    @DisplayName("Markdown Brief output doğrulamaları")
    class MarkdownOutputTests {

        @Test
        @DisplayName("Markdown beklenen bölümleri içerir")
        void shouldContainExpectedSections() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);
            String content = response.getContent();

            assertThat(content).contains("# Truva Atının Hikayesi");
            assertThat(content).contains("## Genel Bilgiler");
            assertThat(content).contains("## Video Başlığı");
            assertThat(content).contains("## Sahne Kullanımı");
            assertThat(content).contains("## Kaynak Referansları");
            assertThat(content).contains("YOUTUBE_LONG_FORM");
        }

        @Test
        @DisplayName("Short-form markdown Hook bölümü içerir")
        void shouldContainShortFormSections() {
            PlatformPackageDraft draft = createApprovedShortsDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);
            String content = response.getContent();

            assertThat(content).contains("## Hook");
            assertThat(content).contains("Truva Atı aslında ne anlama geliyordu?");
        }
    }

    // ═══ JSON Output Tests ═══

    @Nested
    @DisplayName("JSON Package output doğrulamaları")
    class JsonOutputTests {

        @Test
        @DisplayName("JSON output parse edilebilir")
        void shouldBeParseableJson() throws Exception {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);

            JsonNode root = OBJECT_MAPPER.readTree(response.getContent());
            assertThat(root.has("exportType")).isTrue();
            assertThat(root.get("exportType").asText()).isEqualTo("JSON_PACKAGE");
            assertThat(root.has("source")).isTrue();
            assertThat(root.has("package")).isTrue();
            assertThat(root.has("sceneUsages")).isTrue();
            assertThat(root.has("metadata")).isTrue();
        }

        @Test
        @DisplayName("JSON output hashtags parsed array olarak döner")
        void shouldContainParsedHashtags() throws Exception {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);

            JsonNode root = OBJECT_MAPPER.readTree(response.getContent());
            JsonNode hashtags = root.path("platformFields").path("hashtags");
            assertThat(hashtags.isArray()).isTrue();
            assertThat(hashtags.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("JSON output scene usages içerir")
        void shouldContainSceneUsages() throws Exception {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);

            JsonNode root = OBJECT_MAPPER.readTree(response.getContent());
            JsonNode scenes = root.path("sceneUsages");
            assertThat(scenes.isArray()).isTrue();
            assertThat(scenes.size()).isEqualTo(3);
            assertThat(scenes.get(0).path("sceneNumber").asInt()).isEqualTo(1);
        }
    }

    // ═══ Copy Text Output Tests ═══

    @Nested
    @DisplayName("Copy Text output doğrulamaları")
    class CopyTextOutputTests {

        @Test
        @DisplayName("Copy text plain text ve clipboard-friendly")
        void shouldBePlainText() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.COPY_TEXT);
            String content = response.getContent();

            assertThat(content).contains("PLATFORM PACKAGE EXPORT");
            assertThat(content).contains("=== BAŞLIK ===");
            assertThat(content).contains("Truva Atının Hikayesi");
            // Markdown heading olmamalı
            assertThat(content).doesNotContain("# Truva");
        }

        @Test
        @DisplayName("Copy text short-form hook bölümü içerir")
        void shouldContainShortFormHook() {
            PlatformPackageDraft draft = createApprovedShortsDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.COPY_TEXT);

            assertThat(response.getContent()).contains("=== HOOK ===");
            assertThat(response.getContent()).contains("Truva Atı aslında ne anlama geliyordu?");
        }
    }

    // ═══ Production Brief Output Tests ═══

    @Nested
    @DisplayName("Production Brief output doğrulamaları")
    class ProductionBriefOutputTests {

        @Test
        @DisplayName("Production brief handoff ve checklist bölümleri içerir")
        void shouldContainHandoffAndChecklist() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.PRODUCTION_BRIEF);
            String content = response.getContent();

            assertThat(content).contains("# PRODUCTION BRIEF");
            assertThat(content).contains("## Proje Bilgileri");
            assertThat(content).contains("## Amaç");
            assertThat(content).contains("## Copy-Ready İçerik");
            assertThat(content).contains("## Yayın Kontrol Listesi");
            assertThat(content).contains("## Kaynak Referansları");
            assertThat(content).contains("- [ ]");
        }

        @Test
        @DisplayName("Production brief sahne planı içerir")
        void shouldContainScenePlan() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.PRODUCTION_BRIEF);

            assertThat(response.getContent()).contains("## Sahne Planı");
            assertThat(response.getContent()).contains("PRIMARY");
            assertThat(response.getContent()).contains("OMITTED");
        }
    }

    // ═══ Safety Tests ═══

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
        @DisplayName("Hiçbir export formatında unsafe alan bulunmaz")
        void shouldNotExposeUnsafeFieldsInAnyFormat(PlatformPackageExportType exportType) {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), exportType);
            String content = response.getContent().toLowerCase();

            for (String unsafe : UNSAFE_KEYWORDS) {
                assertThat(content).as("Content should not contain unsafe keyword: " + unsafe)
                        .doesNotContain(unsafe.toLowerCase());
            }
        }
    }

    // ═══ Metadata Mapper Tests ═══

    @Nested
    @DisplayName("PlatformPackageExportMetadataMapper")
    class MetadataMapperTests {

        @Test
        @DisplayName("Mapper safe alanları doğru map eder")
        void shouldMapSafeFields() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();

            PlatformPackageExportMetadata meta = PlatformPackageExportMetadataMapper.mapFrom(draft);

            assertThat(meta.getContentProjectId()).isEqualTo(project.getId());
            assertThat(meta.getSourceStoryboardDraftId()).isEqualTo(storyboardDraft.getId());
            assertThat(meta.getVersion()).isEqualTo(1);
            assertThat(meta.getPackageStatus()).isEqualTo("APPROVED");
            assertThat(meta.getPackageTitle()).isEqualTo("Truva Atının Hikayesi");
            assertThat(meta.getApprovedAt()).isNotNull();
            assertThat(meta.getApprovedBy()).isEqualTo("admin-serhat");
        }

        @Test
        @DisplayName("Mapper boş scene usages → boş liste")
        void shouldHandleEmptySceneUsages() {
            PlatformPackageDraft draft = createApprovedShortsDraft();

            PlatformPackageExportMetadata meta = PlatformPackageExportMetadataMapper.mapFrom(draft);

            assertThat(meta.getSceneUsages()).isEmpty();
        }
    }

    // ═══ All 4 Export Types, All Platform Types ═══

    @Nested
    @DisplayName("Tüm export × platform kombinasyonları")
    class CrossCombinationTests {

        @ParameterizedTest
        @EnumSource(PlatformPackageExportType.class)
        @DisplayName("YOUTUBE_LONG_FORM × tüm export tipleri başarılı")
        void shouldExportYtLfAllFormats(PlatformPackageExportType exportType) {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), exportType);

            assertThat(response.getContent()).isNotBlank();
            assertThat(response.getExportType()).isEqualTo(exportType);
        }

        @ParameterizedTest
        @EnumSource(PlatformPackageExportType.class)
        @DisplayName("YOUTUBE_SHORTS × tüm export tipleri başarılı")
        void shouldExportShortsAllFormats(PlatformPackageExportType exportType) {
            PlatformPackageDraft draft = createApprovedShortsDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), exportType);

            assertThat(response.getContent()).isNotBlank();
            assertThat(response.getExportType()).isEqualTo(exportType);
        }
    }

    // ═══ §16.1 #17: YT Long-Form Platform Fields ═══

    @Nested
    @DisplayName("§16.1 #17 — YT Long-Form export alanları")
    class YtLongFormFieldTests {

        @Test
        @DisplayName("Markdown export videoTitle, chapters, tags, pinned comment içerir")
        void markdownShouldContainYtLfFields() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);
            String content = response.getContent();

            assertThat(content).contains("Truva Atı'nın Bilinmeyen Hikayesi | Anadolu Atlası");
            assertThat(content).contains("00:00 Giriş");
            assertThat(content).contains("truva atı");
            assertThat(content).contains("Hangi Anadolu hikayesini dinlemek istersiniz?");
            assertThat(content).contains("TRUVA ATI'NIN SIRRI");
        }

        @Test
        @DisplayName("JSON export videoTitle, chapters, tags parsed array olarak içerir")
        void jsonShouldContainYtLfFields() throws Exception {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);
            JsonNode root = OBJECT_MAPPER.readTree(response.getContent());
            JsonNode pf = root.path("platformFields");

            assertThat(pf.has("videoTitle")).isTrue();
            assertThat(pf.path("chapterOutline").isArray()).isTrue();
            assertThat(pf.path("chapterOutline").size()).isEqualTo(3);
            assertThat(pf.path("tags").isArray()).isTrue();
            assertThat(pf.path("tags").size()).isEqualTo(3);
            assertThat(pf.has("pinnedCommentSuggestion")).isTrue();
            assertThat(pf.has("thumbnailTextSuggestion")).isTrue();
        }

        @Test
        @DisplayName("Copy text export videoTitle ve chapters içerir")
        void copyTextShouldContainYtLfFields() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.COPY_TEXT);

            assertThat(response.getContent()).contains("=== VİDEO BAŞLIĞI ===");
            assertThat(response.getContent()).contains("=== SABİTLENECEK YORUM ===");
            assertThat(response.getContent()).contains("=== THUMBNAIL METNİ ===");
        }

        @Test
        @DisplayName("Production brief copy-ready bölümde YT LF alanları var")
        void productionBriefShouldContainYtLfFields() {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.PRODUCTION_BRIEF);

            assertThat(response.getContent()).contains("### Başlık");
            assertThat(response.getContent()).contains("### Bölümler");
            assertThat(response.getContent()).contains("### Sabitlenecek Yorum");
            assertThat(response.getContent()).contains("- [ ] Video başlığı kopyalandı");
        }
    }

    // ═══ §16.1 #18: Shorts/Reels Platform Fields ═══

    @Nested
    @DisplayName("§16.1 #18 — Shorts/Reels export alanları")
    class ShortsReelsFieldTests {

        @Test
        @DisplayName("Markdown export hook, overlay, short hashtags, duration guidance içerir")
        void markdownShouldContainShortsFields() {
            PlatformPackageDraft draft = createApprovedShortsDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);
            String content = response.getContent();

            assertThat(content).contains("Truva Atı aslında ne anlama geliyordu?"); // hook
            assertThat(content).contains("TRUVA ATI"); // overlay
            assertThat(content).contains("#shorts"); // short hashtags
            assertThat(content).contains("45-55 saniye"); // duration guidance
        }

        @Test
        @DisplayName("JSON export shortHook, overlays parsed, shortHashtags parsed")
        void jsonShouldContainShortsFields() throws Exception {
            PlatformPackageDraft draft = createApprovedShortsDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);
            JsonNode root = OBJECT_MAPPER.readTree(response.getContent());
            JsonNode pf = root.path("platformFields");

            assertThat(pf.has("shortHook")).isTrue();
            assertThat(pf.path("overlayTextSuggestions").isArray()).isTrue();
            assertThat(pf.path("overlayTextSuggestions").size()).isEqualTo(3);
            assertThat(pf.path("shortHashtagSet").isArray()).isTrue();
            assertThat(pf.path("shortHashtagSet").size()).isEqualTo(3);
            assertThat(pf.has("durationGuidance")).isTrue();
        }

        @Test
        @DisplayName("Copy text export hook ve overlay bölümleri içerir")
        void copyTextShouldContainShortsFields() {
            PlatformPackageDraft draft = createApprovedShortsDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.COPY_TEXT);

            assertThat(response.getContent()).contains("=== HOOK ===");
            assertThat(response.getContent()).contains("=== OVERLAY METİNLERİ ===");
            assertThat(response.getContent()).contains("=== SÜRE YÖNLENDİRMESİ ===");
        }

        @Test
        @DisplayName("Production brief short-form checklist'te hook ve overlay var")
        void productionBriefShouldContainShortChecklist() {
            PlatformPackageDraft draft = createApprovedShortsDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.PRODUCTION_BRIEF);

            assertThat(response.getContent()).contains("- [ ] Hook metni hazırlandı");
            assertThat(response.getContent()).contains("- [ ] Overlay metinleri eklendi");
        }
    }

    // ═══ §16.1 #19: Null Optional Fields ═══

    @Nested
    @DisplayName("§16.1 #19 — Null optional alanlar → section omitted, error yok")
    class NullOptionalFieldTests {

        private PlatformPackageDraft createMinimalApprovedDraft() {
            PlatformPackageDraft draft = PlatformPackageDraft.builder()
                    .contentProject(project)
                    .sourceStoryboardDraft(storyboardDraft)
                    .packageType(PlatformPackageType.YOUTUBE_LONG_FORM)
                    .status(PlatformPackageStatus.APPROVED)
                    .version(1)
                    .title("Minimal Paket")
                    .captionOrDescription("Minimal açıklama")
                    // Tüm opsiyonel alanlar null
                    .build();
            draft.setId(UUID.randomUUID());
            return draft;
        }

        @ParameterizedTest
        @EnumSource(PlatformPackageExportType.class)
        @DisplayName("Minimal draft (null opsiyoneller) → hatasız export")
        void shouldExportWithNullOptionalFields(PlatformPackageExportType exportType) {
            PlatformPackageDraft draft = createMinimalApprovedDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), exportType);

            assertThat(response.getContent()).isNotBlank();
            assertThat(response.getExportType()).isEqualTo(exportType);
        }

        @Test
        @DisplayName("Null videoTitle → 'Video Başlığı' bölümü yok (Markdown)")
        void markdownShouldOmitNullVideoTitle() {
            PlatformPackageDraft draft = createMinimalApprovedDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.MARKDOWN_BRIEF);

            assertThat(response.getContent()).doesNotContain("## Video Başlığı");
        }

        @Test
        @DisplayName("Null chapters → chapterOutline boş array (JSON)")
        void jsonShouldOmitNullChapters() throws Exception {
            PlatformPackageDraft draft = createMinimalApprovedDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);
            JsonNode root = OBJECT_MAPPER.readTree(response.getContent());

            assertThat(root.path("platformFields").path("chapterOutline").size()).isEqualTo(0);
        }

        @Test
        @DisplayName("Null sceneUsageNotes → copy text'te bölüm yok")
        void copyTextShouldOmitNullNotes() {
            PlatformPackageDraft draft = createMinimalApprovedDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.COPY_TEXT);

            assertThat(response.getContent()).doesNotContain("=== SAHNE KULLANIM NOTLARI ===");
        }
    }

    // ═══ §16.1 #13: JSON Shape Stability ═══

    @Nested
    @DisplayName("§16.1 #13 — JSON shape stability (source, package, platformFields, sceneUsages, metadata)")
    class JsonShapeStabilityTests {

        @Test
        @DisplayName("JSON export stable top-level keys içerir")
        void shouldHaveStableTopLevelKeys() throws Exception {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);
            JsonNode root = OBJECT_MAPPER.readTree(response.getContent());

            assertThat(root.has("exportType")).isTrue();
            assertThat(root.has("exportedAt")).isTrue();
            assertThat(root.has("source")).isTrue();
            assertThat(root.has("approval")).isTrue();
            assertThat(root.has("metadata")).isTrue();
            assertThat(root.has("package")).isTrue();
            assertThat(root.has("platformFields")).isTrue();
            assertThat(root.has("sceneUsages")).isTrue();
        }

        @Test
        @DisplayName("JSON source bölümü doğru alanları içerir")
        void shouldHaveCorrectSourceFields() throws Exception {
            PlatformPackageDraft draft = createApprovedYtLfDraft();
            when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageExportResponse response = exportService.exportPackage(
                    draft.getId(), PlatformPackageExportType.JSON_PACKAGE);
            JsonNode source = OBJECT_MAPPER.readTree(response.getContent()).path("source");

            assertThat(source.has("contentProjectId")).isTrue();
            assertThat(source.has("sourceStoryboardDraftId")).isTrue();
            assertThat(source.has("platformPackageDraftId")).isTrue();
            assertThat(source.has("packageType")).isTrue();
            assertThat(source.has("version")).isTrue();
        }
    }
}
