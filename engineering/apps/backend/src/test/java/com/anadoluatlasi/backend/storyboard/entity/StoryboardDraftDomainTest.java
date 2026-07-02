package com.anadoluatlasi.backend.storyboard.entity;

import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-003: StoryboardDraft ve StoryboardScene domain model unit testleri.
 *
 * <p>Bu testler entity oluşturma, scene ilişkisi, status enum,
 * source beat type reuse ve governance kurallarını doğrular.
 * JPA/persistence testleri değildir.</p>
 */
class StoryboardDraftDomainTest {

    private ContentProject project;
    private ContentDraft sourceDraft;
    private ScenarioDraft sourceScenario;

    @BeforeEach
    void setUp() {
        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .retryCount(0)
                .build();

        sourceDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Approved Draft Title")
                .body("Approved draft body content.")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        sourceScenario = ScenarioDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(sourceDraft)
                .version(1)
                .status(ScenarioDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    // ── StoryboardDraft Oluşturma ───────────────────────────

    @Nested
    @DisplayName("StoryboardDraft oluşturma")
    class StoryboardDraftCreation {

        @Test
        @DisplayName("Gerekli alanlarla StoryboardDraft oluşturulabilir")
        void shouldCreateWithRequiredFields() {
            StoryboardDraft draft = StoryboardDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .sourceScenarioDraft(sourceScenario)
                    .sourceScenarioVersion(1)
                    .version(1)
                    .status(StoryboardDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getContentProject()).isEqualTo(project);
            assertThat(draft.getSourceDraft()).isEqualTo(sourceDraft);
            assertThat(draft.getSourceScenarioDraft()).isEqualTo(sourceScenario);
            assertThat(draft.getSourceScenarioVersion()).isEqualTo(1);
            assertThat(draft.getVersion()).isEqualTo(1);
            assertThat(draft.getStatus()).isEqualTo(StoryboardDraftStatus.AI_GENERATED);
            assertThat(draft.getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
        }

        @Test
        @DisplayName("Tüm alanlarla StoryboardDraft oluşturulabilir")
        void shouldCreateWithAllFields() {
            StoryboardDraft draft = StoryboardDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .sourceScenarioDraft(sourceScenario)
                    .sourceScenarioVersion(1)
                    .version(1)
                    .status(StoryboardDraftStatus.AI_GENERATED)
                    .title("Truva Atı Storyboard")
                    .summary("Truva Atı hikayesi için sahne bazlı görsel planlama.")
                    .targetPlatform("YouTube")
                    .aspectRatio("16:9")
                    .estimatedDurationSeconds(600)
                    .visualStyleGuide("Tarihi dramatik tarz, sıcak renkler")
                    .narrationStyle("Hikâye anlatıcı, sakin ve etkileyici")
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .providerType("openai")
                    .modelName("gpt-4o-mini")
                    .build();

            assertThat(draft.getTitle()).isEqualTo("Truva Atı Storyboard");
            assertThat(draft.getTargetPlatform()).isEqualTo("YouTube");
            assertThat(draft.getAspectRatio()).isEqualTo("16:9");
            assertThat(draft.getEstimatedDurationSeconds()).isEqualTo(600);
            assertThat(draft.getVisualStyleGuide()).contains("dramatik");
            assertThat(draft.getNarrationStyle()).contains("anlatıcı");
            assertThat(draft.getProviderType()).isEqualTo("openai");
            assertThat(draft.getModelName()).isEqualTo("gpt-4o-mini");
        }

        @Test
        @DisplayName("@PrePersist UUID otomatik atanır")
        void shouldGenerateUuidOnPrePersist() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getId()).isNull();
            draft.prePersist();
            assertThat(draft.getId()).isNotNull();
        }

        @Test
        @DisplayName("@PrePersist createdAt otomatik atanır")
        void shouldSetCreatedAtOnPrePersist() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getCreatedAt()).isNull();
            draft.prePersist();
            assertThat(draft.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("@PreUpdate updatedAt güncellenir")
        void shouldUpdateTimestampOnPreUpdate() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getUpdatedAt()).isNull();
            draft.preUpdate();
            assertThat(draft.getUpdatedAt()).isNotNull();
        }
    }

    // ── StoryboardScene ─────────────────────────────────────

    @Nested
    @DisplayName("StoryboardScene")
    class StoryboardSceneTests {

        @Test
        @DisplayName("Scene gerekli alanlarla oluşturulabilir")
        void shouldCreateSceneWithRequiredFields() {
            StoryboardDraft draft = createMinimalDraft();
            draft.prePersist();

            StoryboardScene scene = createScene(draft, 1,
                    ScenarioBeatType.HOOK, "Açılış Sahnesi",
                    "Tarihi harabelerin havadan görünümü");

            assertThat(scene.getStoryboardDraft()).isEqualTo(draft);
            assertThat(scene.getSceneNumber()).isEqualTo(1);
            assertThat(scene.getSourceBeatType()).isEqualTo(ScenarioBeatType.HOOK);
            assertThat(scene.getSourceBeatOrder()).isEqualTo(1);
            assertThat(scene.getSceneTitle()).isEqualTo("Açılış Sahnesi");
            assertThat(scene.getVisualDescription()).contains("harabelerin");
            assertThat(scene.getEstimatedDurationSeconds()).isEqualTo(10);
        }

        @Test
        @DisplayName("Scene tüm opsiyonel alanlarla oluşturulabilir")
        void shouldCreateSceneWithAllFields() {
            StoryboardDraft draft = createMinimalDraft();
            draft.prePersist();

            StoryboardScene scene = StoryboardScene.builder()
                    .storyboardDraft(draft)
                    .sceneNumber(1)
                    .sourceBeatType(ScenarioBeatType.HOOK)
                    .sourceBeatOrder(1)
                    .sceneTitle("Açılış Sahnesi")
                    .visualDescription("Tarihi harabelerin havadan görünümü")
                    .narrationText("Binlerce yıl önce bu topraklarda...")
                    .onScreenText("Truva, MÖ 1200")
                    .cameraDirection("Drone wide shot → slow zoom in")
                    .transitionNote("fade in")
                    .mood("Gizemli, merak uyandıran")
                    .location("Truva harabeleri, Çanakkale")
                    .characters("Anlatıcı sesi")
                    .estimatedDurationSeconds(10)
                    .mediaGenerationNotes("Altın saat ışığında dramatik drone görüntüsü")
                    .build();

            assertThat(scene.getNarrationText()).contains("Binlerce yıl");
            assertThat(scene.getOnScreenText()).isEqualTo("Truva, MÖ 1200");
            assertThat(scene.getCameraDirection()).contains("Drone");
            assertThat(scene.getTransitionNote()).isEqualTo("fade in");
            assertThat(scene.getMood()).contains("Gizemli");
            assertThat(scene.getLocation()).contains("Çanakkale");
            assertThat(scene.getCharacters()).isEqualTo("Anlatıcı sesi");
            assertThat(scene.getMediaGenerationNotes()).contains("drone");
        }

        @Test
        @DisplayName("Storyboard birden fazla sıralı scene içerebilir")
        void shouldContainMultipleOrderedScenes() {
            StoryboardDraft draft = createMinimalDraft();
            draft.prePersist();

            List<StoryboardScene> scenes = List.of(
                    createScene(draft, 1, ScenarioBeatType.HOOK, "Açılış", "Hook görseli"),
                    createScene(draft, 2, ScenarioBeatType.INTRODUCTION, "Giriş", "Giriş görseli"),
                    createScene(draft, 3, ScenarioBeatType.MAIN_NARRATIVE, "Ana Anlatı I", "Ana sahne görseli"),
                    createScene(draft, 4, ScenarioBeatType.MAIN_NARRATIVE, "Ana Anlatı II", "Devam görseli"),
                    createScene(draft, 5, ScenarioBeatType.EMOTIONAL_CLIMAX, "Doruk", "Doruk görseli"),
                    createScene(draft, 6, ScenarioBeatType.CONCLUSION, "Kapanış", "Kapanış görseli"),
                    createScene(draft, 7, ScenarioBeatType.CALL_TO_ACTION, "CTA", "CTA görseli")
            );

            assertThat(scenes).hasSize(7);
            // MAIN_NARRATIVE beat'i birden fazla scene'e bölünebilir
            long mainNarrativeScenes = scenes.stream()
                    .filter(s -> s.getSourceBeatType() == ScenarioBeatType.MAIN_NARRATIVE)
                    .count();
            assertThat(mainNarrativeScenes).isEqualTo(2);
        }

        @Test
        @DisplayName("Scene sıralaması sceneNumber ile deterministik")
        void shouldHaveDeterministicOrderBySceneNumber() {
            StoryboardDraft draft = createMinimalDraft();
            draft.prePersist();

            StoryboardScene scene1 = createScene(draft, 1, ScenarioBeatType.HOOK, "Açılış", "Hook");
            StoryboardScene scene2 = createScene(draft, 2, ScenarioBeatType.INTRODUCTION, "Giriş", "Giriş");
            StoryboardScene scene3 = createScene(draft, 3, ScenarioBeatType.MAIN_NARRATIVE, "Ana", "Ana");

            assertThat(scene1.getSceneNumber()).isLessThan(scene2.getSceneNumber());
            assertThat(scene2.getSceneNumber()).isLessThan(scene3.getSceneNumber());
        }

        @Test
        @DisplayName("Scene @PrePersist UUID otomatik atanır")
        void shouldGenerateSceneUuid() {
            StoryboardDraft draft = createMinimalDraft();
            draft.prePersist();

            StoryboardScene scene = createScene(draft, 1, ScenarioBeatType.HOOK, "Test", "Test");
            assertThat(scene.getId()).isNull();
            scene.prePersist();
            assertThat(scene.getId()).isNotNull();
        }
    }

    // ── Status Enum ─────────────────────────────────────────

    @Nested
    @DisplayName("StoryboardDraftStatus")
    class StatusEnumTests {

        @Test
        @DisplayName("5 status değeri mevcut")
        void shouldHaveFiveValues() {
            assertThat(StoryboardDraftStatus.values()).hasSize(5);
        }

        @Test
        @DisplayName("Tüm beklenen status'ler mevcut")
        void shouldContainExpectedStatuses() {
            assertThat(StoryboardDraftStatus.values()).containsExactlyInAnyOrder(
                    StoryboardDraftStatus.AI_GENERATED,
                    StoryboardDraftStatus.NEEDS_REVIEW,
                    StoryboardDraftStatus.APPROVED,
                    StoryboardDraftStatus.REJECTED,
                    StoryboardDraftStatus.FAILED
            );
        }

        @Test
        @DisplayName("EDITED status mevcut değil — edit metadata ile izlenir")
        void shouldNotContainEditedStatus() {
            for (StoryboardDraftStatus status : StoryboardDraftStatus.values()) {
                assertThat(status.name()).isNotEqualTo("EDITED");
            }
        }

        @Test
        @DisplayName("Status değerleri ScenarioDraftStatus ile tutarlı")
        void shouldBeConsistentWithScenarioDraftStatus() {
            // FP-002 ve FP-003 aynı 5 status setini kullanır
            assertThat(StoryboardDraftStatus.values().length)
                    .isEqualTo(ScenarioDraftStatus.values().length);

            for (StoryboardDraftStatus status : StoryboardDraftStatus.values()) {
                assertThat(ScenarioDraftStatus.valueOf(status.name())).isNotNull();
            }
        }
    }

    // ── Source Beat Type Reuse ───────────────────────────────

    @Nested
    @DisplayName("Source beat type — ScenarioBeatType reuse")
    class SourceBeatTypeTests {

        @Test
        @DisplayName("Scene source beat type olarak ScenarioBeatType kullanılır")
        void shouldUseScenarioBeatTypeForSourceBeat() {
            StoryboardDraft draft = createMinimalDraft();
            draft.prePersist();

            StoryboardScene scene = createScene(draft, 1, ScenarioBeatType.HOOK, "Test", "Test");
            assertThat(scene.getSourceBeatType()).isEqualTo(ScenarioBeatType.HOOK);
        }

        @Test
        @DisplayName("Tüm 6 beat tipi source olarak kullanılabilir")
        void shouldSupportAllSixBeatTypes() {
            StoryboardDraft draft = createMinimalDraft();
            draft.prePersist();

            for (ScenarioBeatType beatType : ScenarioBeatType.values()) {
                StoryboardScene scene = createScene(draft, beatType.getOrder(),
                        beatType, "Test " + beatType.getLabel(), "Test visual");
                assertThat(scene.getSourceBeatType()).isEqualTo(beatType);
                assertThat(scene.getSourceBeatOrder()).isEqualTo(beatType.getOrder());
            }
        }

        @Test
        @DisplayName("Source beat type değerleri FP-002 beat değerleriyle hizalı")
        void shouldAlignWithFP002BeatValues() {
            assertThat(ScenarioBeatType.values()).containsExactlyInAnyOrder(
                    ScenarioBeatType.HOOK,
                    ScenarioBeatType.INTRODUCTION,
                    ScenarioBeatType.MAIN_NARRATIVE,
                    ScenarioBeatType.EMOTIONAL_CLIMAX,
                    ScenarioBeatType.CONCLUSION,
                    ScenarioBeatType.CALL_TO_ACTION
            );
        }
    }

    // ── Provider Metadata Safety ────────────────────────────

    @Nested
    @DisplayName("Provider metadata safety")
    class ProviderMetadataSafety {

        @Test
        @DisplayName("Provider metadata sadece safe alanlar: providerType ve modelName")
        void shouldOnlyHaveSafeProviderFields() {
            StoryboardDraft draft = StoryboardDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .sourceScenarioDraft(sourceScenario)
                    .sourceScenarioVersion(1)
                    .version(1)
                    .status(StoryboardDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .providerType("mock")
                    .modelName("mock-storyboard-v1")
                    .build();

            assertThat(draft.getProviderType()).isEqualTo("mock");
            assertThat(draft.getModelName()).isEqualTo("mock-storyboard-v1");
        }

        @Test
        @DisplayName("Entity'de rawPrompt, rawPayload, apiKey alanı yok")
        void shouldNotHaveUnsafeFields() {
            // StoryboardDraft entity field'larını kontrol et
            java.lang.reflect.Field[] fields = StoryboardDraft.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("rawprompt");
                assertThat(name).doesNotContain("rawpayload");
                assertThat(name).doesNotContain("apikey");
                assertThat(name).doesNotContain("secret");
            }
        }

        @Test
        @DisplayName("Entity'de media file alanı yok (image/video/audio URL)")
        void shouldNotHaveMediaFileFields() {
            java.lang.reflect.Field[] fields = StoryboardDraft.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("imageurl");
                assertThat(name).doesNotContain("videourl");
                assertThat(name).doesNotContain("audiourl");
                assertThat(name).doesNotContain("mediafile");
                assertThat(name).doesNotContain("asseturl");
            }
        }

        @Test
        @DisplayName("Scene entity'de media file alanı yok")
        void sceneShouldNotHaveMediaFileFields() {
            java.lang.reflect.Field[] fields = StoryboardScene.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("imageurl");
                assertThat(name).doesNotContain("videourl");
                assertThat(name).doesNotContain("audiourl");
                assertThat(name).doesNotContain("asseturl");
            }
        }
    }

    // ── Version Tracking ────────────────────────────────────

    @Nested
    @DisplayName("Version tracking")
    class VersionTracking {

        @Test
        @DisplayName("Version 1'den başlar")
        void shouldStartFromVersion1() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Regeneration yeni version oluşturur")
        void shouldSupportMultipleVersions() {
            StoryboardDraft v1 = StoryboardDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .sourceScenarioDraft(sourceScenario)
                    .sourceScenarioVersion(1)
                    .version(1)
                    .status(StoryboardDraftStatus.REJECTED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            StoryboardDraft v2 = StoryboardDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .sourceScenarioDraft(sourceScenario)
                    .sourceScenarioVersion(1)
                    .version(2)
                    .status(StoryboardDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(v2.getVersion()).isGreaterThan(v1.getVersion());
        }
    }

    // ── Traceability ────────────────────────────────────────

    @Nested
    @DisplayName("Traceability — izlenebilirlik")
    class Traceability {

        @Test
        @DisplayName("Content Project referansı korunur")
        void shouldMaintainContentProjectTraceability() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getContentProject()).isNotNull();
            assertThat(draft.getContentProject().getId()).isEqualTo(project.getId());
        }

        @Test
        @DisplayName("Source ContentDraft referansı korunur")
        void shouldMaintainSourceDraftTraceability() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getSourceDraft()).isNotNull();
            assertThat(draft.getSourceDraft().getId()).isEqualTo(sourceDraft.getId());
        }

        @Test
        @DisplayName("Source ScenarioDraft referansı korunur")
        void shouldMaintainSourceScenarioTraceability() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getSourceScenarioDraft()).isNotNull();
            assertThat(draft.getSourceScenarioDraft().getId()).isEqualTo(sourceScenario.getId());
        }

        @Test
        @DisplayName("Source scenario version korunur")
        void shouldMaintainSourceScenarioVersion() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getSourceScenarioVersion()).isEqualTo(1);
        }
    }

    // ── Governance Kuralları ─────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @Test
        @DisplayName("AI üretimi sadece AI_GENERATED status oluşturur")
        void aiGenerationShouldOnlyProduceAiGeneratedStatus() {
            StoryboardDraft draft = createMinimalDraft();
            assertThat(draft.getStatus()).isEqualTo(StoryboardDraftStatus.AI_GENERATED);
            assertThat(draft.getStatus()).isNotEqualTo(StoryboardDraftStatus.APPROVED);
        }

        @Test
        @DisplayName("AI üretimi FAILED status de oluşturabilir")
        void aiGenerationCanProduceFailedStatus() {
            StoryboardDraft draft = StoryboardDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .sourceScenarioDraft(sourceScenario)
                    .sourceScenarioVersion(1)
                    .version(1)
                    .status(StoryboardDraftStatus.FAILED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getStatus()).isEqualTo(StoryboardDraftStatus.FAILED);
        }
    }

    // ── Helper Methods ──────────────────────────────────────

    private StoryboardDraft createMinimalDraft() {
        return StoryboardDraft.builder()
                .contentProject(project)
                .sourceDraft(sourceDraft)
                .sourceScenarioDraft(sourceScenario)
                .sourceScenarioVersion(1)
                .version(1)
                .status(StoryboardDraftStatus.AI_GENERATED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    private StoryboardScene createScene(StoryboardDraft draft, int sceneNumber,
                                         ScenarioBeatType beatType, String title,
                                         String visualDescription) {
        return StoryboardScene.builder()
                .storyboardDraft(draft)
                .sceneNumber(sceneNumber)
                .sourceBeatType(beatType)
                .sourceBeatOrder(beatType.getOrder())
                .sceneTitle(title)
                .visualDescription(visualDescription)
                .estimatedDurationSeconds(10)
                .build();
    }
}
