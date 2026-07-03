package com.anadoluatlasi.backend.platformpackage.entity;

import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-004: PlatformPackageDraft ve PlatformPackageSceneUsage domain model unit testleri.
 *
 * <p>Bu testler entity oluşturma, enum değerleri, ilişkiler, flat metadata,
 * version tracking, governance kuralları ve güvenlik kısıtlamalarını doğrular.
 * JPA/persistence testleri değildir.</p>
 */
class PlatformPackageDraftDomainTest {

    private ContentProject project;
    private ContentDraft contentDraft;
    private ScenarioDraft scenarioDraft;
    private StoryboardDraft storyboardDraft;

    @BeforeEach
    void setUp() {
        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .retryCount(0)
                .build();

        contentDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Test Draft")
                .body("Test body")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        scenarioDraft = ScenarioDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(contentDraft)
                .version(1)
                .status(ScenarioDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        storyboardDraft = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(contentDraft)
                .sourceScenarioDraft(scenarioDraft)
                .sourceScenarioVersion(1)
                .version(1)
                .status(StoryboardDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    // ── PlatformPackageDraft Oluşturma ──────────────────────────

    @Nested
    @DisplayName("PlatformPackageDraft oluşturma")
    class PackageDraftCreation {

        @Test
        @DisplayName("Gerekli alanlarla PlatformPackageDraft oluşturulabilir")
        void shouldCreateWithRequiredFields() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);

            assertThat(draft.getContentProject()).isEqualTo(project);
            assertThat(draft.getSourceStoryboardDraft()).isEqualTo(storyboardDraft);
            assertThat(draft.getPackageType()).isEqualTo(PlatformPackageType.YOUTUBE_LONG_FORM);
            assertThat(draft.getStatus()).isEqualTo(PlatformPackageStatus.AI_GENERATED);
            assertThat(draft.getVersion()).isEqualTo(1);
            assertThat(draft.getTitle()).isEqualTo("Test Paketi");
            assertThat(draft.getCaptionOrDescription()).isEqualTo("Test açıklaması");
        }

        @Test
        @DisplayName("YouTube Long-form metadata alanlarıyla oluşturulabilir")
        void shouldCreateWithYoutubeLongFormMetadata() {
            PlatformPackageDraft draft = PlatformPackageDraft.builder()
                    .contentProject(project)
                    .sourceStoryboardDraft(storyboardDraft)
                    .packageType(PlatformPackageType.YOUTUBE_LONG_FORM)
                    .status(PlatformPackageStatus.AI_GENERATED)
                    .version(1)
                    .title("Truva Atı'nın Bilinmeyen Hikayesi")
                    .captionOrDescription("Antik dünyanın en ilginç hikayesi...")
                    .hashtagsJson("[\"TruvaAtı\",\"Tarih\",\"Anadolu\"]")
                    .callToAction("Abone olmayı unutmayın!")
                    .targetDurationSeconds(340)
                    .aspectRatio("16:9")
                    .videoTitle("Truva Atı'nın Bilinmeyen Hikayesi | Anadolu Atlası")
                    .longDescription("Bu videoda Truva Atı'nın gerçek hikayesini...")
                    .chapterOutlineJson("[\"00:00 Giriş\",\"00:10 Truva'nın Önemi\"]")
                    .pinnedCommentSuggestion("Hangi Anadolu hikayesini dinlemek istersiniz?")
                    .thumbnailTextSuggestion("TRUVA ATI'NIN SIRRI")
                    .tagsJson("[\"truva atı\",\"anadolu\",\"tarih\"]")
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .providerType("OPENAI")
                    .modelName("gpt-4o-mini")
                    .build();

            assertThat(draft.getVideoTitle()).contains("Bilinmeyen Hikayesi");
            assertThat(draft.getLongDescription()).contains("gerçek hikayesi");
            assertThat(draft.getChapterOutlineJson()).contains("Giriş");
            assertThat(draft.getPinnedCommentSuggestion()).contains("Anadolu");
            assertThat(draft.getThumbnailTextSuggestion()).contains("SIRRI");
            assertThat(draft.getTagsJson()).contains("tarih");
        }

        @Test
        @DisplayName("Short-form/Reels metadata alanlarıyla oluşturulabilir")
        void shouldCreateWithShortFormMetadata() {
            PlatformPackageDraft draft = PlatformPackageDraft.builder()
                    .contentProject(project)
                    .sourceStoryboardDraft(storyboardDraft)
                    .packageType(PlatformPackageType.YOUTUBE_SHORTS)
                    .status(PlatformPackageStatus.AI_GENERATED)
                    .version(1)
                    .title("Truva Atı'nın Sırrı #shorts")
                    .captionOrDescription("Kısa açıklama")
                    .targetDurationSeconds(45)
                    .aspectRatio("9:16")
                    .shortHook("Truva Atı'nın gerçek hikayesini biliyor musun?")
                    .shortCaption("Antik çağın en büyük sırrı...")
                    .overlayTextSuggestionsJson("[\"Truva Atı\",\"MÖ 1200\"]")
                    .shortHashtagSetJson("[\"TruvaAtı\",\"shorts\",\"tarih\"]")
                    .shortCta("Tam videoyu izle!")
                    .durationGuidance("30-45 saniye")
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getShortHook()).contains("gerçek hikayesi");
            assertThat(draft.getShortCaption()).contains("sırrı");
            assertThat(draft.getOverlayTextSuggestionsJson()).contains("MÖ 1200");
            assertThat(draft.getShortHashtagSetJson()).contains("shorts");
            assertThat(draft.getShortCta()).contains("Tam videoyu");
            assertThat(draft.getDurationGuidance()).contains("30-45");
            assertThat(draft.getAspectRatio()).isEqualTo("9:16");
        }

        @Test
        @DisplayName("@PrePersist UUID otomatik atanır")
        void shouldGenerateUuidOnPrePersist() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            assertThat(draft.getId()).isNull();
            draft.onPrePersist();
            assertThat(draft.getId()).isNotNull();
        }

        @Test
        @DisplayName("createdAt alanı BaseEntity'den miras alınır")
        void shouldSupportCreatedAtField() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            assertThat(draft.getCreatedAt()).isNull();
            draft.setCreatedAt(LocalDateTime.now());
            assertThat(draft.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Tüm 4 platform tipi için draft oluşturulabilir")
        void shouldCreateDraftForAllPlatformTypes() {
            for (PlatformPackageType type : PlatformPackageType.values()) {
                PlatformPackageDraft draft = createMinimalDraft(type);
                assertThat(draft.getPackageType()).isEqualTo(type);
            }
        }
    }

    // ── PlatformPackageType Enum ────────────────────────────────

    @Nested
    @DisplayName("PlatformPackageType enum")
    class PackageTypeEnumTests {

        @Test
        @DisplayName("4 platform tipi mevcut")
        void shouldHaveFourValues() {
            assertThat(PlatformPackageType.values()).hasSize(4);
        }

        @Test
        @DisplayName("Tüm beklenen tipler mevcut")
        void shouldContainExpectedTypes() {
            assertThat(PlatformPackageType.values()).containsExactlyInAnyOrder(
                    PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageType.YOUTUBE_SHORTS,
                    PlatformPackageType.INSTAGRAM_REELS,
                    PlatformPackageType.FACEBOOK_REELS
            );
        }

        @Test
        @DisplayName("Publishing veya media tipi yok")
        void shouldNotContainPublishingOrMediaTypes() {
            for (PlatformPackageType type : PlatformPackageType.values()) {
                assertThat(type.name()).doesNotContain("PUBLISH");
                assertThat(type.name()).doesNotContain("MEDIA");
                assertThat(type.name()).doesNotContain("THUMBNAIL");
            }
        }
    }

    // ── PlatformPackageStatus Enum ──────────────────────────────

    @Nested
    @DisplayName("PlatformPackageStatus enum")
    class PackageStatusEnumTests {

        @Test
        @DisplayName("5 status değeri mevcut")
        void shouldHaveFiveValues() {
            assertThat(PlatformPackageStatus.values()).hasSize(5);
        }

        @Test
        @DisplayName("Tüm beklenen status'ler mevcut")
        void shouldContainExpectedStatuses() {
            assertThat(PlatformPackageStatus.values()).containsExactlyInAnyOrder(
                    PlatformPackageStatus.AI_GENERATED,
                    PlatformPackageStatus.NEEDS_REVIEW,
                    PlatformPackageStatus.APPROVED,
                    PlatformPackageStatus.REJECTED,
                    PlatformPackageStatus.FAILED
            );
        }

        @Test
        @DisplayName("READY_FOR_PUBLISH status mevcut değil")
        void shouldNotContainReadyForPublish() {
            for (PlatformPackageStatus status : PlatformPackageStatus.values()) {
                assertThat(status.name()).isNotEqualTo("READY_FOR_PUBLISH");
            }
        }

        @Test
        @DisplayName("Status değerleri StoryboardDraftStatus ile tutarlı")
        void shouldBeConsistentWithStoryboardDraftStatus() {
            assertThat(PlatformPackageStatus.values().length)
                    .isEqualTo(StoryboardDraftStatus.values().length);

            for (PlatformPackageStatus status : PlatformPackageStatus.values()) {
                assertThat(StoryboardDraftStatus.valueOf(status.name())).isNotNull();
            }
        }
    }

    // ── PlatformPackageSceneUsage ───────────────────────────────

    @Nested
    @DisplayName("PlatformPackageSceneUsage")
    class SceneUsageTests {

        @Test
        @DisplayName("Scene usage gerekli alanlarla oluşturulabilir")
        void shouldCreateWithRequiredFields() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_SHORTS);
            draft.onPrePersist();

            PlatformPackageSceneUsage usage = PlatformPackageSceneUsage.builder()
                    .platformPackageDraft(draft)
                    .sceneNumber(1)
                    .usageType("PRIMARY")
                    .included(true)
                    .build();

            assertThat(usage.getPlatformPackageDraft()).isEqualTo(draft);
            assertThat(usage.getSceneNumber()).isEqualTo(1);
            assertThat(usage.getUsageType()).isEqualTo("PRIMARY");
            assertThat(usage.getIncluded()).isTrue();
        }

        @Test
        @DisplayName("Scene usage tüm alanlarla oluşturulabilir")
        void shouldCreateWithAllFields() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.INSTAGRAM_REELS);
            draft.onPrePersist();

            StoryboardScene scene = StoryboardScene.builder()
                    .id(UUID.randomUUID())
                    .storyboardDraft(storyboardDraft)
                    .sceneNumber(1)
                    .build();

            PlatformPackageSceneUsage usage = PlatformPackageSceneUsage.builder()
                    .platformPackageDraft(draft)
                    .sceneNumber(1)
                    .sourceStoryboardScene(scene)
                    .usageType("ADAPTED")
                    .included(true)
                    .adaptationNote("Hook 5 saniyeye kısaltıldı")
                    .build();

            assertThat(usage.getSourceStoryboardScene()).isEqualTo(scene);
            assertThat(usage.getUsageType()).isEqualTo("ADAPTED");
            assertThat(usage.getAdaptationNote()).contains("kısaltıldı");
        }

        @Test
        @DisplayName("OMITTED scene usage — sahne kullanılmıyor")
        void shouldSupportOmittedUsage() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_SHORTS);
            draft.onPrePersist();

            PlatformPackageSceneUsage usage = PlatformPackageSceneUsage.builder()
                    .platformPackageDraft(draft)
                    .sceneNumber(5)
                    .usageType("OMITTED")
                    .included(false)
                    .adaptationNote("Shorts formatında CONCLUSION sahnesi gereksiz")
                    .build();

            assertThat(usage.getIncluded()).isFalse();
            assertThat(usage.getUsageType()).isEqualTo("OMITTED");
        }

        @Test
        @DisplayName("@PrePersist UUID otomatik atanır")
        void shouldGenerateUuidOnPrePersist() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_SHORTS);
            draft.onPrePersist();

            PlatformPackageSceneUsage usage = PlatformPackageSceneUsage.builder()
                    .platformPackageDraft(draft)
                    .sceneNumber(1)
                    .usageType("PRIMARY")
                    .included(true)
                    .build();

            assertThat(usage.getId()).isNull();
            usage.onPrePersist();
            assertThat(usage.getId()).isNotNull();
        }
    }

    // ── Review Lifecycle ────────────────────────────────────────

    @Nested
    @DisplayName("Review lifecycle alanları")
    class ReviewLifecycle {

        @Test
        @DisplayName("Approve metadata set edilebilir")
        void shouldSetApproveMetadata() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            draft.setStatus(PlatformPackageStatus.APPROVED);
            draft.setApprovedAt(LocalDateTime.now());
            draft.setApprovedBy("admin-test");

            assertThat(draft.getStatus()).isEqualTo(PlatformPackageStatus.APPROVED);
            assertThat(draft.getApprovedAt()).isNotNull();
            assertThat(draft.getApprovedBy()).isEqualTo("admin-test");
        }

        @Test
        @DisplayName("Reject metadata ve reason set edilebilir")
        void shouldSetRejectMetadataWithReason() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_SHORTS);
            draft.setStatus(PlatformPackageStatus.REJECTED);
            draft.setRejectedAt(LocalDateTime.now());
            draft.setRejectedBy("admin-test");
            draft.setRejectionReason("Hashtag'ler uygun değil");

            assertThat(draft.getStatus()).isEqualTo(PlatformPackageStatus.REJECTED);
            assertThat(draft.getRejectedAt()).isNotNull();
            assertThat(draft.getRejectedBy()).isEqualTo("admin-test");
            assertThat(draft.getRejectionReason()).contains("Hashtag");
        }

        @Test
        @DisplayName("Edit metadata set edilebilir → NEEDS_REVIEW")
        void shouldSetEditMetadata() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.INSTAGRAM_REELS);
            draft.setStatus(PlatformPackageStatus.NEEDS_REVIEW);
            draft.setEditedAt(LocalDateTime.now());
            draft.setEditedBy("admin-test");

            assertThat(draft.getStatus()).isEqualTo(PlatformPackageStatus.NEEDS_REVIEW);
            assertThat(draft.getEditedAt()).isNotNull();
            assertThat(draft.getEditedBy()).isEqualTo("admin-test");
        }
    }

    // ── Version Tracking ────────────────────────────────────────

    @Nested
    @DisplayName("Version tracking")
    class VersionTracking {

        @Test
        @DisplayName("Version 1'den başlar")
        void shouldStartFromVersion1() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            assertThat(draft.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Regeneration yeni version oluşturur — eski versiyon korunur")
        void shouldSupportMultipleVersions() {
            PlatformPackageDraft v1 = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            v1.setStatus(PlatformPackageStatus.REJECTED);

            PlatformPackageDraft v2 = PlatformPackageDraft.builder()
                    .contentProject(project)
                    .sourceStoryboardDraft(storyboardDraft)
                    .packageType(PlatformPackageType.YOUTUBE_LONG_FORM)
                    .status(PlatformPackageStatus.AI_GENERATED)
                    .version(2)
                    .title("V2 Paketi")
                    .captionOrDescription("V2 açıklaması")
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(v2.getVersion()).isGreaterThan(v1.getVersion());
        }
    }

    // ── Provider Metadata Safety ────────────────────────────────

    @Nested
    @DisplayName("Provider metadata safety")
    class ProviderMetadataSafety {

        @Test
        @DisplayName("Provider metadata sadece safe alanlar: providerType ve modelName")
        void shouldOnlyHaveSafeProviderFields() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            draft.setProviderType("MOCK");
            draft.setModelName("mock-package-v1");

            assertThat(draft.getProviderType()).isEqualTo("MOCK");
            assertThat(draft.getModelName()).isEqualTo("mock-package-v1");
        }

        @Test
        @DisplayName("Entity'de rawPrompt, rawPayload, apiKey alanı yok")
        void shouldNotHaveUnsafeFields() {
            java.lang.reflect.Field[] fields = PlatformPackageDraft.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("rawprompt");
                assertThat(name).doesNotContain("rawpayload");
                assertThat(name).doesNotContain("apikey");
                assertThat(name).doesNotContain("secret");
            }
        }

        @Test
        @DisplayName("Entity'de media file alanı yok")
        void shouldNotHaveMediaFileFields() {
            java.lang.reflect.Field[] fields = PlatformPackageDraft.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("imageurl");
                assertThat(name).doesNotContain("videourl");
                assertThat(name).doesNotContain("audiourl");
                assertThat(name).doesNotContain("mediafile");
            }
        }

        @Test
        @DisplayName("Entity'de publishing alanı yok")
        void shouldNotHavePublishingFields() {
            java.lang.reflect.Field[] fields = PlatformPackageDraft.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("publishedat");
                assertThat(name).doesNotContain("publishedby");
                assertThat(name).doesNotContain("publishurl");
                assertThat(name).doesNotContain("oauthtoken");
            }
        }
    }

    // ── Traceability ────────────────────────────────────────────

    @Nested
    @DisplayName("Traceability — izlenebilirlik")
    class Traceability {

        @Test
        @DisplayName("Content Project referansı korunur")
        void shouldMaintainContentProjectTraceability() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            assertThat(draft.getContentProject()).isNotNull();
            assertThat(draft.getContentProject().getId()).isEqualTo(project.getId());
        }

        @Test
        @DisplayName("Source StoryboardDraft referansı korunur")
        void shouldMaintainSourceStoryboardTraceability() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            assertThat(draft.getSourceStoryboardDraft()).isNotNull();
            assertThat(draft.getSourceStoryboardDraft().getId()).isEqualTo(storyboardDraft.getId());
        }

        @Test
        @DisplayName("FP-001→FP-002→FP-003→FP-004 zinciri izlenebilir")
        void shouldMaintainFullTraceabilityChain() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);

            // FP-004 → FP-003
            assertThat(draft.getSourceStoryboardDraft()).isEqualTo(storyboardDraft);
            // FP-003 → FP-002
            assertThat(storyboardDraft.getSourceScenarioDraft()).isEqualTo(scenarioDraft);
            // FP-002 → FP-001
            assertThat(scenarioDraft.getSourceDraft()).isEqualTo(contentDraft);
            // FP-001 → ContentProject
            assertThat(contentDraft.getContentProject()).isEqualTo(project);
        }
    }

    // ── Governance Kuralları ─────────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @Test
        @DisplayName("AI üretimi sadece AI_GENERATED status oluşturur")
        void aiGenerationShouldOnlyProduceAiGeneratedStatus() {
            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            assertThat(draft.getStatus()).isEqualTo(PlatformPackageStatus.AI_GENERATED);
            assertThat(draft.getStatus()).isNotEqualTo(PlatformPackageStatus.APPROVED);
        }

        @Test
        @DisplayName("AI üretimi FAILED status de oluşturabilir")
        void aiGenerationCanProduceFailedStatus() {
            PlatformPackageDraft draft = PlatformPackageDraft.builder()
                    .contentProject(project)
                    .sourceStoryboardDraft(storyboardDraft)
                    .packageType(PlatformPackageType.YOUTUBE_LONG_FORM)
                    .status(PlatformPackageStatus.FAILED)
                    .version(1)
                    .title("Başarısız Paket")
                    .captionOrDescription("Üretim başarısız")
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getStatus()).isEqualTo(PlatformPackageStatus.FAILED);
        }

        @Test
        @DisplayName("Kaynak storyboard APPROVED olmalı (entity düzeyinde referans korunur)")
        void sourceStoryboardMustBeApproved() {
            // Entity düzeyinde status kontrolü yapılmaz — bu service sorumluluğudur.
            // Ancak test olarak referans doğrulanır.
            assertThat(storyboardDraft.getStatus()).isEqualTo(StoryboardDraftStatus.APPROVED);

            PlatformPackageDraft draft = createMinimalDraft(PlatformPackageType.YOUTUBE_LONG_FORM);
            assertThat(draft.getSourceStoryboardDraft().getStatus())
                    .isEqualTo(StoryboardDraftStatus.APPROVED);
        }
    }

    // ── Helper Methods ──────────────────────────────────────────

    private PlatformPackageDraft createMinimalDraft(PlatformPackageType type) {
        return PlatformPackageDraft.builder()
                .contentProject(project)
                .sourceStoryboardDraft(storyboardDraft)
                .packageType(type)
                .status(PlatformPackageStatus.AI_GENERATED)
                .version(1)
                .title("Test Paketi")
                .captionOrDescription("Test açıklaması")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }
}
